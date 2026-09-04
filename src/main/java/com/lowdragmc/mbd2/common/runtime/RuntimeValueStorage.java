package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The per-machine / per-trait bag of {@link RuntimeValue} slots, and the one managed field that carries
 * them to disk.
 * <p>
 * <b>Persistence only — overrides are never sent between sides.</b> Each side owns the overrides on
 * its own machine instance. Almost every value a slot fronts (auto IO, transfer rates, recipe damping,
 * an entity scan box) is read from {@code serverTick} and the recipe engine, so a client copy would be
 * gameplay state on a side that must never act on it. A client that wants a client-only value — a
 * render toggle, an overlay preference — writes it on its own instance, where it costs nothing and
 * reaches nobody. A client that has written nothing reads its definition's authored value, which is the
 * same answer it gave before this system existed.
 * <p>
 * The flip side, and the one thing to know before relying on a client-side override: it lives only as
 * long as that client block entity. The client never deserializes this storage — {@code loadAdditional}
 * routes to {@code deserializeInitialData} there, which only covers synced fields — so a chunk reload
 * drops it and the slot goes back to reading the definition. Fine for a render toggle; not a place to
 * keep anything the player expects to persist.
 * <p>
 * Concretely: no {@code @DescSynced} anywhere in this system. The synced-field count of
 * {@code MBDMachine} and {@code Trait} is therefore unchanged by it, so it cannot produce a
 * {@code "Synced fields count mismatch"}, and an override costs zero bandwidth.
 * <p>
 * Slots are registered from the owner's field initialisers and never afterwards, so {@link #slots} is
 * effectively immutable by the time the block entity starts ticking. Overrides themselves live in
 * {@code volatile} fields on each slot and are replaced rather than mutated, which is what makes
 * {@link #serializeNBT} safe to call from LDLib's async persistence thread while the game thread writes.
 */
public final class RuntimeValueStorage implements INBTSerializable<CompoundTag> {
    /** Codec for the {@code AABB} leaves ({@code AutoWorldIO.range}), matching LDLib's own AABB accessor. */
    public static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("min").forGetter(AABB::getMinPosition),
            Vec3.CODEC.fieldOf("max").forGetter(AABB::getMaxPosition)
    ).apply(instance, AABB::new));

    /** Decodes to an unmodifiable list, so a decoded override cannot be mutated behind the slot's back. */
    public static final Codec<List<String>> STRING_LIST_CODEC =
            Codec.STRING.listOf().xmap(List::copyOf, List::copyOf);

    /**
     * The runtime type token for {@link #ofStringList}. {@code Class} cannot express {@code List<String>},
     * so the slot checks {@code List} and {@link RuntimeValue#coerce} does the element conversion.
     */
    @SuppressWarnings("unchecked")
    public static final Class<List<String>> STRING_LIST_TYPE = (Class<List<String>>) (Class<?>) List.class;

    private final IRuntimeValueHolder holder;
    private final Map<String, RuntimeValue<?>> slots = new LinkedHashMap<>();
    /**
     * Payloads for slot ids this build does not know — a downgraded mod, a removed trait type, a renamed
     * key. Kept verbatim and written back out, so a temporary mod-list change cannot silently turn into
     * permanent data loss. Empty in the normal case.
     */
    private volatile Map<String, Tag> unknown = Map.of();

    public RuntimeValueStorage(IRuntimeValueHolder holder) {
        this.holder = holder;
    }

    public IRuntimeValueHolder getHolder() {
        return holder;
    }

    // ***** registration ***** //

    public <T> RuntimeValue<T> of(String key, Class<T> type, Codec<T> codec, Supplier<T> fallback) {
        return register(new RuntimeValue<>(this, key, type, codec, fallback));
    }

    public RuntimeValue<Boolean> ofBool(String key, Supplier<Boolean> fallback) {
        return of(key, Boolean.class, Codec.BOOL, fallback);
    }

    public RuntimeValue<Integer> ofInt(String key, Supplier<Integer> fallback) {
        return of(key, Integer.class, Codec.INT, fallback);
    }

    public RuntimeValue<Long> ofLong(String key, Supplier<Long> fallback) {
        return of(key, Long.class, Codec.LONG, fallback);
    }

    public RuntimeValue<Float> ofFloat(String key, Supplier<Float> fallback) {
        return of(key, Float.class, Codec.FLOAT, fallback);
    }

    public RuntimeValue<Double> ofDouble(String key, Supplier<Double> fallback) {
        return of(key, Double.class, Codec.DOUBLE, fallback);
    }

    public RuntimeValue<String> ofString(String key, Supplier<String> fallback) {
        return of(key, String.class, Codec.STRING, fallback);
    }

    /**
     * A slot holding an <b>immutable</b> list of strings — {@code slot_names} is the one that exists.
     * <p>
     * The codec copies on both ends, and {@link RuntimeValue#coerce} copies whatever a script or a node
     * hands over, so the value behind the slot can never be a list the caller still holds a reference to.
     * That matters more here than for the scalar slots: {@link #serializeNBT} runs on LDLib's async
     * persistence thread, and a caller mutating its own {@code ArrayList} afterwards would be a data race
     * with no obvious cause.
     */
    public RuntimeValue<List<String>> ofStringList(String key, Supplier<List<String>> fallback) {
        return of(key, STRING_LIST_TYPE, STRING_LIST_CODEC, () -> List.copyOf(fallback.get()));
    }

    public RuntimeValue<AABB> ofAABB(String key, Supplier<AABB> fallback) {
        return of(key, AABB.class, AABB_CODEC, fallback);
    }

    public <E extends Enum<E>> RuntimeValue<E> ofEnum(String key, Class<E> type, Supplier<E> fallback) {
        return of(key, type, enumCodec(type), fallback);
    }

    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            for (var constant : type.getEnumConstants()) {
                if (constant.name().equals(name)) return DataResult.success(constant);
            }
            return DataResult.error(() -> "Unknown %s: %s".formatted(type.getSimpleName(), name));
        }, Enum::name);
    }

    private <T> RuntimeValue<T> register(RuntimeValue<T> value) {
        if (slots.containsKey(value.getKey())) {
            throw new IllegalStateException("Duplicate runtime value key '%s' on %s"
                    .formatted(value.getKey(), holder.getClass().getSimpleName()));
        }
        slots.put(value.getKey(), value);
        return value;
    }

    // ***** by-name access, for scripts and blueprint nodes ***** //

    @Nullable
    public RuntimeValue<?> slot(String key) {
        return slots.get(key);
    }

    public Collection<RuntimeValue<?>> slots() {
        return Collections.unmodifiableCollection(slots.values());
    }

    public boolean isOverridden(String key) {
        var slot = slots.get(key);
        return slot != null && slot.isOverridden();
    }

    @Nullable
    public Object get(String key) {
        var slot = slots.get(key);
        return slot == null ? null : slot.get();
    }

    @Nullable
    public Object authored(String key) {
        var slot = slots.get(key);
        return slot == null ? null : slot.authored();
    }

    @SuppressWarnings("unchecked")
    public void set(String key, Object value) {
        var slot = require(key);
        ((RuntimeValue<Object>) slot).set(slot.coerce(value));
    }

    public void clear(String key) {
        require(key).clear();
    }

    public void clearAll() {
        slots.values().forEach(RuntimeValue::clear);
    }

    private RuntimeValue<?> require(String key) {
        var slot = slots.get(key);
        if (slot == null) {
            throw new IllegalArgumentException("Unknown runtime value '%s' on %s. Available: %s"
                    .formatted(key, holder.getClass().getSimpleName(), slots.keySet()));
        }
        return slot;
    }

    /** Called by {@link RuntimeValue#set}/{@link RuntimeValue#clear} — the {@code @LazyManaged} contract. */
    void onSlotChanged(RuntimeValue<?> slot) {
        holder.markRuntimeValuesDirty();
        slot.fireChanged();
    }

    // ***** persistence ***** //

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        // copy: the produced tag escapes to the caller, and unknown outlives it
        unknown.forEach((key, payload) -> tag.put(key, payload.copy()));
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        for (var slot : slots.values()) {
            var encoded = slot.encode(ops);
            if (encoded != null) {
                tag.put(slot.getKey(), encoded);
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        List<RuntimeValue<?>> changed = null;
        for (var slot : slots.values()) {
            // absent key means "not overridden", so a slot missing from the tag is cleared, not skipped
            if (slot.decodeAndApply(ops, tag.get(slot.getKey()))) {
                if (changed == null) changed = new ArrayList<>();
                changed.add(slot);
            }
        }
        Map<String, Tag> leftover = null;
        for (var key : tag.getAllKeys()) {
            if (slots.containsKey(key)) continue;
            if (leftover == null) leftover = new LinkedHashMap<>();
            var payload = tag.get(key);
            if (payload != null) leftover.put(key, payload.copy());
        }
        if (leftover != null) {
            MBD2.LOGGER.debug("Preserving {} unknown runtime value(s) on {}: {}",
                    leftover.size(), holder.getClass().getSimpleName(), leftover.keySet());
        }
        unknown = leftover == null ? Map.of() : Collections.unmodifiableMap(leftover);
        if (changed != null) {
            changed.forEach(RuntimeValue::fireChanged);
        }
    }
}

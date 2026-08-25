package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A single named runtime slot on a machine or a trait. Scripts and blueprint graphs write the slot via
 * {@link #set}; if nothing has been written, {@link #get} falls back to the value authored on the shared
 * {@link com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition definition}.
 * <p>
 * A slot holds <b>one leaf value</b> — a boolean, an int, an enum, an AABB. Compound config objects such as
 * {@link com.lowdragmc.mbd2.common.trait.AutoIO} are modelled as a bundle of leaf slots (see
 * {@link RuntimeAutoIO}), so overriding one leaf leaves its siblings still reading the definition.
 * <p>
 * The read path is a direct field access on the owning object — no map lookup and no reflection. The
 * definition stays immutable shared data; the machine instance owns the overrides and persists them to
 * its block entity NBT. Nothing is transmitted between sides — see {@link RuntimeValueStorage}.
 * <p>
 * Slots are declared as {@code public final} fields in the owner's field initialisers, via
 * {@link RuntimeValueStorage#of} and friends, which registers them for serialization and by-name access:
 * <pre>{@code
 * public final RuntimeValue<Integer> slotLimit =
 *         runtimeValues.ofInt("slot_limit", () -> getDefinition().getSlotLimit());
 * }</pre>
 * The fallback must be a lambda — it is evaluated lazily, because {@code getDefinition()} is not yet
 * available while the owner's field initialisers run.
 *
 * @param <T> the leaf value type. Must be immutable: {@link RuntimeValueStorage#serializeNBT} runs on
 *            LDLib's async persistence thread while writes come from the game thread.
 */
public final class RuntimeValue<T> {
    private final RuntimeValueStorage storage;
    private final String key;
    private final Class<T> type;
    private final Codec<T> codec;
    private final Supplier<T> fallback;
    @Nullable
    private Runnable onChanged;

    /** {@code null} means "not overridden — read the definition". Immutable values only, see class doc. */
    @Nullable
    private volatile T override;

    RuntimeValue(RuntimeValueStorage storage, String key, Class<T> type, Codec<T> codec,
                 Supplier<T> fallback) {
        this.storage = storage;
        this.key = key;
        this.type = type;
        this.codec = codec;
        this.fallback = fallback;
    }

    /**
     * Register a side effect to run whenever this slot's effective value changes — from a script, a graph
     * node or an NBT load alike. Use it for the invalidation a plain read cannot do for itself, e.g.
     * {@code invalidateCapabilities()}.
     * <p>
     * The hook may run before the block entity is in a level (chunk load, editor preview), so it must
     * tolerate a null level. It may also run on either side, since a client can hold its own overrides.
     */
    public RuntimeValue<T> onChanged(Runnable hook) {
        if (onChanged != null) {
            // one hook per slot: silently replacing the first would drop an invalidation and the symptom
            // would be a stale capability cache somewhere far from here
            throw new IllegalStateException("Runtime value '%s' already has an onChanged hook".formatted(key));
        }
        this.onChanged = hook;
        return this;
    }

    /** The effective value: the override if one is set, otherwise the authored definition value. */
    public T get() {
        var value = override;
        return value != null ? value : fallback.get();
    }

    /** The authored definition value, ignoring any override. */
    public T authored() {
        return fallback.get();
    }

    public boolean isOverridden() {
        return override != null;
    }

    /**
     * Override the definition value for this machine only.
     * <p>
     * Not sent anywhere: each side owns its own overrides. A server-side write is server state and is
     * saved with the block entity. A client-side write is how a client-only value is held, and lives only
     * as long as that client block entity — see {@link RuntimeValueStorage}.
     */
    public void set(T value) {
        Objects.requireNonNull(value, "runtime value override must not be null, use clear() instead");
        if (Objects.equals(override, value)) return;
        override = value;
        storage.onSlotChanged(this);
    }

    /**
     * Set from an untyped value, coercing it to this slot's type — an int or a double for a number
     * slot, the constant name for an enum one.
     * <p>
     * This is the entry point <b>scripts</b> should use. {@link #set} is generic, so it erases to
     * {@code set(Object)}, and KubeJS's Rhino refuses to bind a JS primitive to that — a script calling
     * {@code slot.set(false)} gets {@code "Can't find method ...CachedClassInfo.set(boolean)"}. A
     * non-generic parameter resolves cleanly.
     */
    public void setValue(Object value) {
        set(coerce(value));
    }

    /** Drop the override so {@link #get} goes back to reading the definition. */
    public void clear() {
        if (override == null) return;
        override = null;
        storage.onSlotChanged(this);
    }

    public String getKey() {
        return key;
    }

    // ***** storage-internal ***** //

    void fireChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /** @return the encoded override, or null when this slot is not overridden (or failed to encode). */
    @Nullable
    <O> O encode(DynamicOps<O> ops) {
        var value = override;
        if (value == null) return null;
        var result = codec.encodeStart(ops, value);
        var encoded = result.result();
        if (encoded.isEmpty()) {
            MBD2.LOGGER.error("Failed to encode runtime value '{}': {}", key,
                    result.error().map(DataResult.Error::message).orElse("unknown"));
            return null;
        }
        return encoded.get();
    }

    /**
     * Apply a payload read back from NBT. A null payload clears the override, because the saved tag
     * simply omits slots that are not overridden — "absent" has to mean "cleared" rather than
     * "unchanged", or a slot could never be un-overridden by a reload.
     *
     * @return whether the effective value changed, i.e. whether {@link #fireChanged} is owed.
     */
    boolean decodeAndApply(DynamicOps<Tag> ops, @Nullable Tag payload) {
        T decoded = null;
        if (payload != null) {
            var result = codec.parse(ops, payload);
            decoded = result.result().orElse(null);
            if (decoded == null) {
                MBD2.LOGGER.error("Failed to decode runtime value '{}': {}. Falling back to the definition value.",
                        key, result.error().map(DataResult.Error::message).orElse("unknown"));
            }
        }
        if (Objects.equals(override, decoded)) return false;
        override = decoded;
        return true;
    }

    /**
     * Best-effort conversion of an untyped script value to this slot's type. KubeJS/Rhino already narrows
     * most things; this covers the numeric widening and the enum-by-name case it does not.
     */
    @SuppressWarnings("unchecked")
    T coerce(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Runtime value '%s' cannot be set to null — clear() is how you remove an override"
                            .formatted(key));
        }
        if (type.isInstance(value)) return (T) value;
        if (value instanceof Number number) {
            // Rhino hands every JS number over as a Double, so an integral slot sees 7.0 rather than 7.
            // Accept that, but refuse to silently truncate 7.9 or saturate 1e20 into a tier.
            if (type == Integer.class) return (T) Integer.valueOf(exactInt(number));
            if (type == Long.class) return (T) Long.valueOf(exactLong(number));
            if (type == Double.class) return (T) Double.valueOf(number.doubleValue());
            if (type == Float.class) return (T) Float.valueOf(number.floatValue());
        }
        if (value instanceof CharSequence sequence) {
            var text = sequence.toString();
            if (type == String.class) return (T) text;
            if (type == Boolean.class) {
                // not Boolean.valueOf: it maps every non-"true" string to false, so a typo'd "ture" or a
                // JS-ish "1" would be stored as false with no error at all
                if (text.equalsIgnoreCase("true")) return (T) Boolean.TRUE;
                if (text.equalsIgnoreCase("false")) return (T) Boolean.FALSE;
            }
            if (type.isEnum()) {
                for (var constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(text)) return constant;
                }
            }
        }
        throw new IllegalArgumentException("Cannot use %s as runtime value '%s' of type %s"
                .formatted(value, key, type.getSimpleName()));
    }

    private int exactInt(Number number) {
        long exact = exactLong(number);
        if (exact < Integer.MIN_VALUE || exact > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("%s is out of range for runtime value '%s'"
                    .formatted(number, key));
        }
        return (int) exact;
    }

    private long exactLong(Number number) {
        double exact = number.doubleValue();
        if (exact != Math.rint(exact) || Double.isNaN(exact) || Double.isInfinite(exact)) {
            throw new IllegalArgumentException("Runtime value '%s' is a whole number, got %s"
                    .formatted(key, number));
        }
        return (long) exact;
    }

    @Override
    public String toString() {
        var value = override;
        return "RuntimeValue[%s=%s]".formatted(key, value == null ? "<definition>" : value);
    }
}

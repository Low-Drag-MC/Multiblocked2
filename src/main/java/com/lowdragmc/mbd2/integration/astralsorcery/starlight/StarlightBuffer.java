package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Received starlight per constellation id, each capped at {@link #getCapacity()}. Astral Sorcery itself keeps no
 * buffer; this one is what lets a recipe cost a fixed amount. Same threading as {@code LumenStorage}.
 */
public class StarlightBuffer implements INBTSerializable<CompoundTag>, IContentChangeAware, IContentVersion {
    /** Float rounding, not starlight. */
    public static final float EPSILON = 1e-4f;

    private final Map<ResourceLocation, Float> contents = new ConcurrentHashMap<>();
    // only the game thread writes it, so the non-atomic increment is safe
    private volatile long contentVersion;
    @Getter
    private volatile float capacity;
    private volatile Set<ResourceLocation> filter;
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    public StarlightBuffer(float capacity, Collection<ResourceLocation> filter) {
        this.capacity = Math.max(0, capacity);
        this.filter = Set.copyOf(filter);
    }

    public boolean accepts(ResourceLocation constellation) {
        var filter = this.filter;
        return filter.isEmpty() || filter.contains(constellation);
    }

    public float getAmount(ResourceLocation constellation) {
        return contents.getOrDefault(constellation, 0f);
    }

    public float getTotal() {
        return (float) contents.values().stream().mapToDouble(Float::doubleValue).sum();
    }

    /** Sorted by constellation id. */
    public Map<ResourceLocation, Float> getContents() {
        var sorted = new LinkedHashMap<ResourceLocation, Float>();
        contents.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    /** @return how much was taken in */
    public float receive(ResourceLocation constellation, float amount, boolean simulate) {
        if (amount <= 0 || !accepts(constellation)) return 0;
        var stored = getAmount(constellation);
        var moved = Math.min(amount, Math.max(0, capacity - stored));
        if (moved > 0 && !simulate) {
            contents.put(constellation, stored + moved);
            changed();
        }
        return moved;
    }

    /**
     * "Any constellation" takes from the fullest first, so what is left stays concentrated.
     *
     * @return how much was taken
     */
    public float consume(StarlightAmount stack, boolean simulate) {
        var amount = stack.amount();
        if (amount <= 0) return 0;
        if (stack.constellation().isPresent()) {
            return consume(stack.constellation().get(), amount, simulate);
        }
        float taken = 0;
        var byAmount = contents.entrySet().stream()
                .sorted(Map.Entry.<ResourceLocation, Float>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        for (var constellation : byAmount) {
            taken += consume(constellation, amount - taken, simulate);
            if (taken >= amount) break;
        }
        return taken;
    }

    private float consume(ResourceLocation constellation, float amount, boolean simulate) {
        var stored = getAmount(constellation);
        var moved = Math.min(amount, stored);
        if (moved > 0 && !simulate) {
            if (stored - moved > EPSILON) {
                contents.put(constellation, stored - moved);
            } else {
                contents.remove(constellation);
            }
            changed();
        }
        return moved;
    }

    /** Clamped to the capacity. */
    public void setAmount(ResourceLocation constellation, float amount) {
        var clamped = Math.max(0, Math.min(amount, capacity));
        if (getAmount(constellation) == clamped) return;
        if (clamped > 0) {
            contents.put(constellation, clamped);
        } else {
            contents.remove(constellation);
        }
        changed();
    }

    private void changed() {
        contentVersion++;
        onContentsChanged.run();
    }

    /** Spills what no longer fits; returns whether anything changed. */
    public boolean setCapacity(float capacity) {
        var clamped = Math.max(0, capacity);
        var changed = this.capacity != clamped;
        this.capacity = clamped;
        var spilled = false;
        for (var entry : contents.entrySet()) {
            if (entry.getValue() > clamped) {
                spilled = true;
                if (clamped > 0) {
                    entry.setValue(clamped);
                } else {
                    contents.remove(entry.getKey());
                }
            }
        }
        if (spilled) contentVersion++;
        return changed || spilled;
    }

    public void setFilter(Collection<ResourceLocation> filter) {
        this.filter = Set.copyOf(filter);
    }

    public StarlightBuffer copy() {
        var copy = new StarlightBuffer(capacity, filter);
        copy.contents.putAll(contents);
        return copy;
    }

    /** The capacity is not saved: it belongs to the trait's runtime value. */
    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        var list = new ListTag();
        getContents().forEach((constellation, amount) -> {
            var entry = new CompoundTag();
            entry.putString("constellation", constellation.toString());
            entry.putFloat("amount", amount);
            list.add(entry);
        });
        tag.put("contents", list);
        return tag;
    }

    public CompoundTag uiSnapshot() {
        var tag = serializeNBT(RegistryAccess.EMPTY);
        tag.putFloat("capacity", capacity);
        var filter = new ListTag();
        this.filter.stream().sorted().forEach(id -> filter.add(StringTag.valueOf(id.toString())));
        tag.put("filter", filter);
        return tag;
    }

    /** Not clamped, see {@code LumenStorage#deserializeNBT}. Unknown constellation ids are kept. */
    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        contents.clear();
        for (var element : tag.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var id = ResourceLocation.tryParse(entry.getString("constellation"));
            var amount = entry.getFloat("amount");
            if (id != null && amount > 0) {
                contents.put(id, amount);
            }
        }
        contentVersion++;
    }

    /** Counts changes to what {@link #serializeNBT} writes, so LDLib2's sync compares this instead of the tag. */
    @Override
    public long getContentVersion() {
        return contentVersion;
    }
}

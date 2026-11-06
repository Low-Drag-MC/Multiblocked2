package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An amount per lumen type, each capped at {@link #getCapacity()}, as Astral Sorcery's own holders count it.
 * The type filter only gates what goes in; narrowing it leaves stored lumen to be spent.
 *
 * <p>The async recipe search {@link #copy() copies} this off-thread while the game thread mutates it, hence the
 * concurrent map and the immutable filter swapped whole.</p>
 */
public class LumenStorage implements INBTSerializable<CompoundTag>, IContentChangeAware, IContentVersion {
    private final Map<Lumen, Integer> contents = new ConcurrentHashMap<>();
    // only the game thread writes it, so the non-atomic increment is safe
    private volatile long contentVersion;
    @Getter
    private volatile int capacity;
    @Getter
    private volatile Set<Lumen> filter;
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    public LumenStorage(int capacity, Collection<Lumen> filter) {
        this.capacity = Math.max(0, capacity);
        this.filter = Set.copyOf(filter);
    }

    public boolean accepts(@Nullable Lumen lumen) {
        if (lumen == null || lumen == LumenAS.NONE.get()) return false;
        var filter = this.filter;
        return filter.isEmpty() || filter.contains(lumen);
    }

    public int getAmount(Lumen lumen) {
        return contents.getOrDefault(lumen, 0);
    }

    public int getCapacity(Lumen lumen) {
        return accepts(lumen) ? capacity : 0;
    }

    public boolean isEmpty() {
        return contents.values().stream().allMatch(amount -> amount <= 0);
    }

    /** Sorted by lumen id. */
    public List<LumenStack> getContents() {
        return contents.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.comparing(entry -> AstralSorceryIntegration.lumenId(entry.getKey())))
                .map(entry -> entry.getKey().stack(entry.getValue()))
                .toList();
    }

    public Set<Lumen> getStoredTypes() {
        return contents.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** @return how much was (or would be) taken in */
    public int fill(Lumen lumen, int amount, boolean simulate) {
        if (amount <= 0 || !accepts(lumen)) return 0;
        var stored = getAmount(lumen);
        var moved = Math.min(amount, Math.max(0, capacity - stored));
        if (moved > 0 && !simulate) {
            contents.put(lumen, stored + moved);
            changed();
        }
        return moved;
    }

    /** @return how much was (or would be) given out */
    public int drain(Lumen lumen, int amount, boolean simulate) {
        if (amount <= 0) return 0;
        var stored = getAmount(lumen);
        var moved = Math.min(amount, stored);
        if (moved > 0 && !simulate) {
            if (stored - moved > 0) {
                contents.put(lumen, stored - moved);
            } else {
                contents.remove(lumen);
            }
            changed();
        }
        return moved;
    }

    /** Clamped to the filter and the capacity. */
    public void setAmount(Lumen lumen, int amount) {
        var clamped = Math.max(0, Math.min(amount, getCapacity(lumen)));
        if (getAmount(lumen) == clamped) return;
        if (clamped > 0) {
            contents.put(lumen, clamped);
        } else {
            contents.remove(lumen);
        }
        changed();
    }

    private void changed() {
        contentVersion++;
        onContentsChanged.run();
    }

    /** Spills what no longer fits; returns whether anything changed. */
    public boolean setCapacity(int capacity) {
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

    public void setFilter(Collection<Lumen> filter) {
        this.filter = Set.copyOf(filter);
    }

    public LumenStorage copy() {
        var copy = new LumenStorage(capacity, filter);
        copy.contents.putAll(contents);
        return copy;
    }

    /** The capacity is not saved: it belongs to the trait's runtime value. */
    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (var stack : getContents()) {
            var entry = new CompoundTag();
            entry.putString("lumen", AstralSorceryIntegration.lumenId(stack.getLumen()).toString());
            entry.putInt("amount", stack.getAmount());
            list.add(entry);
        }
        tag.put("contents", list);
        return tag;
    }

    public CompoundTag uiSnapshot() {
        var tag = serializeNBT(RegistryAccess.EMPTY);
        tag.putInt("capacity", capacity);
        var filter = new ListTag();
        this.filter.stream().map(AstralSorceryIntegration::lumenId).sorted()
                .forEach(id -> filter.add(StringTag.valueOf(id.toString())));
        tag.put("filter", filter);
        return tag;
    }

    /**
     * Not clamped: the trait's {@code capacity} override is read back after this, so clamping to the definition's
     * capacity here would lose what a larger override held. The trait re-applies it in {@code onMachineLoad}.
     */
    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        contents.clear();
        for (var element : tag.getList("contents", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var id = ResourceLocation.tryParse(entry.getString("lumen"));
            var lumen = AstralSorceryIntegration.lumen(id);
            var amount = entry.getInt("amount");
            if (lumen != null && amount > 0) {
                contents.put(lumen, amount);
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

package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.common.capability.SourceStorage;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * Ars Nouveau's own {@link SourceStorage}, plus the two things MBD2 needs from a storage:
 * a change hook to drive the machine's sync, and a copy for recipe simulation.
 *
 * <p>Mirrors {@link com.lowdragmc.mbd2.common.trait.forgeenergy.CopiableEnergyStorage} — the resource
 * behaves the same way, so the reasoning in that class about resizing and load order applies here
 * unchanged.</p>
 */
public class CopiableSourceStorage extends SourceStorage implements IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    public CopiableSourceStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public CopiableSourceStorage(int capacity, int maxReceive, int maxExtract, int source) {
        super(capacity, maxReceive, maxExtract, source);
    }

    /**
     * The superclass calls this from {@code receiveSource}/{@code extractSource} whenever a non-simulated
     * transfer moved something, which is exactly where the machine wants to hear about it.
     */
    @Override
    public void onContentsChanged() {
        onContentsChanged.run();
    }

    /**
     * As the superclass, but it notifies.
     * <p>
     * {@code SourceStorage#setSource} deliberately does not — Ars Nouveau's own tiles call
     * {@code updateBlock()} beside it instead. Everything reaching this through
     * {@link SourceCapWrapper} or the {@link MachineSourceProvider} adapter would otherwise change the
     * stored amount without the GUI or the renderer ever finding out.
     */
    @Override
    public void setSource(int source) {
        var before = getSource();
        super.setSource(source);
        if (before != getSource()) onContentsChanged.run();
    }

    /**
     * Resize the buffer in place, for the {@code capacity} runtime value.
     * <p>
     * Shrinking below what is stored spills the excess rather than leaving the storage over-full: a
     * source cap reporting {@code source > capacity} makes every percentage-based reader — the GUI bar,
     * the block renderer, an Arcane Relay's fullness check — read past 100%.
     *
     * @return whether anything changed, so the caller can skip a needless content-changed notification
     */
    public boolean setCapacity(int capacity) {
        var clamped = Math.max(0, capacity);
        if (this.capacity == clamped && this.source <= clamped) return false;
        this.capacity = clamped;
        this.source = Math.min(this.source, clamped);
        return true;
    }

    /** {@link #setCapacity}, for callers coming in through {@link com.hollingsworth.arsnouveau.api.source.ISourceCap}. */
    @Override
    public void setMaxSource(int max) {
        if (setCapacity(max)) onContentsChanged.run();
    }

    /**
     * As the superclass, but clamped to the current capacity.
     * <p>
     * Needed because {@code capacity} is a runtime value and the two are restored independently: LDLib
     * decides the order the persisted fields are read in, so a {@code capacity} override that shrinks the
     * buffer may well be applied before this tag is. Without the clamp that load order leaves the storage
     * holding more than it can, which the superclass' unconditional assignment would happily do.
     */
    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull Tag nbt) {
        super.deserializeNBT(provider, nbt);
        this.source = Math.min(this.source, this.capacity);
    }

    public CopiableSourceStorage copy() {
        return new CopiableSourceStorage(capacity, maxReceive, maxExtract, source);
    }
}

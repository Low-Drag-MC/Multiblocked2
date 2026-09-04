package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CopiableEnergyStorage extends EnergyStorage implements IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    public CopiableEnergyStorage(int capacity) {
        super(capacity);
    }

    public CopiableEnergyStorage(int capacity, int energy) {
        super(capacity, capacity, capacity, energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        var received =  super.receiveEnergy(maxReceive, simulate);
        if (received > 0) onContentsChanged.run();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        var extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0) onContentsChanged.run();
        return extracted;
    }

    /**
     * Resize the buffer in place, for the {@code capacity} runtime value.
     * <p>
     * Shrinking below what is stored spills the excess rather than leaving the storage over-full: an
     * {@code IEnergyStorage} reporting {@code stored > capacity} makes every percentage-based reader —
     * the GUI bar, {@code shouldSyncStorage}, other mods' meters — draw past 100%.
     *
     * @return whether anything changed, so the caller can skip a needless content-changed notification
     */
    public boolean setCapacity(int capacity) {
        var clamped = Math.max(0, capacity);
        if (this.capacity == clamped && this.energy <= clamped) return false;
        this.capacity = clamped;
        this.energy = Math.min(this.energy, clamped);
        return true;
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
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        super.deserializeNBT(provider, nbt);
        this.energy = Math.min(this.energy, this.capacity);
    }

    public CopiableEnergyStorage copy() {
        return new CopiableEnergyStorage(capacity, energy);
    }

}

package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.neoforged.neoforge.energy.IEnergyStorage;

public record EnergyStorageWrapper(IEnergyStorage storage,
                                   IO io,
                                   int maxReceive,
                                   int maxExtract) implements IEnergyStorage {

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (io == IO.IN || io == IO.BOTH) {
            return storage.receiveEnergy(Math.min(this.maxReceive, maxReceive), simulate);
        }
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (io == IO.OUT || io == IO.BOTH) {
            return storage.extractEnergy(Math.min(this.maxExtract, maxExtract), simulate);
        }
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return io == IO.OUT || io == IO.BOTH;
    }

    @Override
    public boolean canReceive() {
        return io == IO.IN || io == IO.BOTH;
    }
}

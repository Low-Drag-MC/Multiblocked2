package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageWrapper implements IEnergyStorage {
    private final IEnergyStorage storage;
    private final IO io;

    public EnergyStorageWrapper(IEnergyStorage storage, IO io) {
        this.storage = storage;
        this.io = io;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (io == IO.IN || io == IO.BOTH) {
            return storage.receiveEnergy(maxReceive, simulate);
        }
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (io == IO.OUT || io == IO.BOTH) {
            return storage.extractEnergy(maxExtract, simulate);
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

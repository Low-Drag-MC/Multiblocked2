package com.lowdragmc.mbd2.common.trait.forgeenergy;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.Arrays;

public record EnergyStorageList(IEnergyStorage[] storages) implements IEnergyStorage {
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = 0;
        for (var storage : storages) {
            received += storage.receiveEnergy(maxReceive - received, simulate);
            if (received >= maxReceive) break;
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = 0;
        for (var storage : storages) {
            extracted += storage.extractEnergy(maxExtract - extracted, simulate);
            if (extracted >= maxExtract) break;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return Arrays.stream(storages).reduce(0, (acc, storage) -> acc + storage.getEnergyStored(), Integer::sum);
    }

    @Override
    public int getMaxEnergyStored() {
        return Arrays.stream(storages).reduce(0, (acc, storage) -> acc + storage.getMaxEnergyStored(), Integer::sum);
    }

    @Override
    public boolean canExtract() {
        return Arrays.stream(storages).anyMatch(IEnergyStorage::canExtract);
    }

    @Override
    public boolean canReceive() {
        return Arrays.stream(storages).anyMatch(IEnergyStorage::canReceive);
    }
}

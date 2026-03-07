package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
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

    public CopiableEnergyStorage copy() {
        return new CopiableEnergyStorage(capacity, energy);
    }

}

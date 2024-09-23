package com.lowdragmc.mbd2.integration.gtm.trait;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraft.core.Direction;

public class EnergyContainerWrapper implements IEnergyContainer {
    private final IEnergyContainer container;
    private final IO io;

    public EnergyContainerWrapper(IEnergyContainer container, IO io) {
        this.container = container;
        this.io = io;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return io == IO.OUT;
    }

    @Override
    public long getOutputAmperage() {
        return container.getOutputAmperage();
    }

    @Override
    public long getOutputVoltage() {
        return container.getOutputVoltage();
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (io != IO.IN && io != IO.BOTH) return 0;
        return container.acceptEnergyFromNetwork(side, voltage, amperage);
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return io == IO.IN || io == IO.BOTH;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        return container.changeEnergy(differenceAmount);
    }

    @Override
    public long getEnergyStored() {
        return container.getEnergyStored();
    }

    @Override
    public long getEnergyCapacity() {
        return container.getEnergyCapacity();
    }

    @Override
    public long getInputAmperage() {
        return container.getInputAmperage();
    }

    @Override
    public long getInputVoltage() {
        return container.getInputVoltage();
    }
}

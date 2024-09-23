package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import mekanism.api.heat.IHeatHandler;

public class HeatContainerWrapper implements IHeatHandler {
    private final IHeatHandler container;
    private final IO io;

    public HeatContainerWrapper(IHeatHandler container, IO io) {
        this.container = container;
        this.io = io;
    }

    @Override
    public int getHeatCapacitorCount() {
        return container.getHeatCapacitorCount();
    }

    @Override
    public double getTemperature(int capacitor) {
        return container.getTemperature(capacitor);
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return container.getInverseConduction(capacitor);
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return container.getHeatCapacity(capacitor);
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        if (transfer > 0 && (io == IO.IN || io == IO.BOTH)) {
            container.handleHeat(capacitor, transfer);
        } else if (transfer < 0 && (io == IO.OUT || io == IO.BOTH)) {
            container.handleHeat(capacitor, transfer);
        }
    }
}

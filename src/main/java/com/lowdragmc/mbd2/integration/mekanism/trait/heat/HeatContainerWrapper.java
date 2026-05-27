package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import mekanism.api.heat.IHeatHandler;

public record HeatContainerWrapper(CopiableHeatContainer storage, IO io) implements IHeatHandler {

    @Override
    public int getHeatCapacitorCount() {
        return storage.getHeatCapacitorCount();
    }

    @Override
    public double getTemperature(int capacitor) {
        return storage.getTemperature(capacitor);
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return storage.getInverseConduction(capacitor);
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return storage.getHeatCapacity(capacitor);
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        if (transfer > 0 && !io.support(IO.IN)) return;
        if (transfer < 0 && !io.support(IO.OUT)) return;
        storage.handleHeat(capacitor, transfer);
    }
}

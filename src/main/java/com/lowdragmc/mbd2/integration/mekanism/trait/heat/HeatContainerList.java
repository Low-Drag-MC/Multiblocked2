package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import mekanism.api.heat.IHeatHandler;

public record HeatContainerList(IHeatHandler[] containers) implements IHeatHandler {

    @Override
    public int getHeatCapacitorCount() {
        return containers.length;
    }

    @Override
    public double getTemperature(int capacitor) {
        return containers[capacitor].getTemperature(capacitor);
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return containers[capacitor].getInverseConduction(capacitor);
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return containers[capacitor].getHeatCapacity(capacitor);
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        containers[capacitor].handleHeat(capacitor, transfer);
    }
}

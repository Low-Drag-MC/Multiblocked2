package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import mekanism.api.heat.IHeatHandler;

public record HeatContainerList(IHeatHandler[] handlers) implements IHeatHandler {

    @Override
    public int getHeatCapacitorCount() {
        int sum = 0;
        for (var handler : handlers) sum += handler.getHeatCapacitorCount();
        return sum;
    }

    private record Resolved(IHeatHandler handler, int localIndex) {}

    private Resolved resolve(int capacitor) {
        int index = 0;
        for (var handler : handlers) {
            var count = handler.getHeatCapacitorCount();
            if (capacitor - index < count) {
                return new Resolved(handler, capacitor - index);
            }
            index += count;
        }
        return null;
    }

    @Override
    public double getTemperature(int capacitor) {
        var r = resolve(capacitor);
        return r == null ? 0 : r.handler.getTemperature(r.localIndex);
    }

    @Override
    public double getInverseConduction(int capacitor) {
        var r = resolve(capacitor);
        return r == null ? 1 : r.handler.getInverseConduction(r.localIndex);
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        var r = resolve(capacitor);
        return r == null ? 0 : r.handler.getHeatCapacity(r.localIndex);
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        var r = resolve(capacitor);
        if (r != null) r.handler.handleHeat(r.localIndex, transfer);
    }
}

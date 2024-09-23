package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;

public class CopiableHeatContainer implements IHeatHandler, ITagSerializable<Tag> {

    public final double capacity;
    public final double inverseConduction;
    public double temperature;

    public CopiableHeatContainer(double capacity, double inverseConduction) {
        this.capacity = capacity;
        this.inverseConduction = Math.max(1, inverseConduction);
    }

    public CopiableHeatContainer copy() {
        CopiableHeatContainer copy = new CopiableHeatContainer(capacity, inverseConduction);
        copy.temperature = temperature;
        return copy;
    }

    @Override
    public int getHeatCapacitorCount() {
        return 1;
    }

    @Override
    public double getTemperature(int capacitor) {
        return capacitor == 0 ? 0 : temperature;
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return capacitor == 0 ? 1 : inverseConduction;
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return capacitor == 0 ? capacity : 0;
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        if (capacitor == 0) {
            temperature = Math.min(capacity, temperature + transfer);
        }
    }

    @Override
    public Tag serializeNBT() {
        return DoubleTag.valueOf(temperature);
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        if (nbt instanceof DoubleTag tag) {
            temperature = tag.getAsDouble();
        }
    }
}

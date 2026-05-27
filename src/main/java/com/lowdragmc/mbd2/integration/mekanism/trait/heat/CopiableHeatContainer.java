package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class CopiableHeatContainer implements IHeatHandler, INBTSerializable<CompoundTag>, IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    private final double capacity;
    private final double inverseConduction;
    private double heat;

    public CopiableHeatContainer(double capacity, double inverseConduction) {
        this.capacity = Math.max(1.0, capacity);
        this.inverseConduction = Math.max(1.0, inverseConduction);
        this.heat = this.capacity * HeatAPI.AMBIENT_TEMP;
    }

    public CopiableHeatContainer copy() {
        var copy = new CopiableHeatContainer(capacity, inverseConduction);
        copy.heat = heat;
        return copy;
    }

    public double getHeat() {
        return heat;
    }

    public void setHeat(double value) {
        if (this.heat != value) {
            this.heat = value;
            onContentsChanged.run();
        }
    }

    @Override
    public int getHeatCapacitorCount() {
        return 1;
    }

    @Override
    public double getTemperature(int capacitor) {
        return capacitor == 0 ? heat / capacity : 0;
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return capacitor == 0 ? inverseConduction : 1;
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return capacitor == 0 ? capacity : 0;
    }

    @Override
    public void handleHeat(int capacitor, double transfer) {
        if (capacitor == 0 && transfer != 0) {
            heat += transfer;
            onContentsChanged.run();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putDouble("heat", heat);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        heat = nbt.getDouble("heat");
    }
}

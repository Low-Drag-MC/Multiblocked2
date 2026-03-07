//package com.lowdragmc.mbd2.integration.mekanism.trait.heat;
//
//import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
//import com.lowdragmc.lowdraglib2.syncdata.ITagSerializable;
//import lombok.Getter;
//import lombok.Setter;
//import mekanism.api.heat.HeatAPI;
//import mekanism.api.heat.IHeatHandler;
//import net.minecraft.nbt.DoubleTag;
//import net.minecraft.nbt.Tag;
//
//public class CopiableHeatContainer implements IHeatHandler, ITagSerializable<Tag>, IContentChangeAware {
//    @Getter
//    @Setter
//    public Runnable onContentsChanged = () -> {};
//
//    public final double capacity;
//    public final double inverseConduction;
//    public double heat;
//
//    public CopiableHeatContainer(double capacity, double inverseConduction) {
//        this.capacity = capacity;
//        this.inverseConduction = Math.max(1, inverseConduction);
//        this.heat = capacity * HeatAPI.AMBIENT_TEMP;
//    }
//
//    public CopiableHeatContainer copy() {
//        CopiableHeatContainer copy = new CopiableHeatContainer(capacity, inverseConduction);
//        copy.heat = heat;
//        return copy;
//    }
//
//    @Override
//    public int getHeatCapacitorCount() {
//        return 1;
//    }
//
//    @Override
//    public double getTemperature(int capacitor) {
//        return capacitor == 0 ? heat / capacity : 0;
//    }
//
//    @Override
//    public double getInverseConduction(int capacitor) {
//        return capacitor == 0 ? inverseConduction : 1;
//    }
//
//    @Override
//    public double getHeatCapacity(int capacitor) {
//        return capacitor == 0 ? capacity : 0;
//    }
//
//    @Override
//    public void handleHeat(int capacitor, double transfer) {
//        if (capacitor == 0 && transfer != 0) {
//            heat += transfer;
//            onContentsChanged.run();
//        }
//    }
//
//    @Override
//    public Tag serializeNBT() {
//        return DoubleTag.valueOf(heat);
//    }
//
//    @Override
//    public void deserializeNBT(Tag nbt) {
//        if (nbt instanceof DoubleTag tag) {
//            heat = tag.getAsDouble();
//        }
//    }
//}

package com.lowdragmc.mbd2.integration.pneumaticcraft.trait;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.nbt.CompoundTag;

public class CopiableAirHandler implements IAirHandler, ITagSerializable<CompoundTag>, IContentChangeAware {
    @Getter
    @Setter
    public Runnable onContentsChanged = () -> {};
    @Getter
    @Accessors(fluent = true)
    protected final float maxPressure;
    @Getter
    protected int baseVolume;
    @Getter
    protected int air;

    public CopiableAirHandler(int volume, float maxPressure) {
        this(volume, 0, maxPressure);
    }

    public CopiableAirHandler(int volume, int air, float maxPressure) {
        this.baseVolume = volume;
        this.air = air;
        this.maxPressure = maxPressure;
    }

    public CopiableAirHandler copy() {
        return new CopiableAirHandler(baseVolume, air, maxPressure);
    }

    @Override
    public float getPressure() {
        return (float) air / getVolume();
    }

    @Override
    public void addAir(int amount) {
        air += amount;
        if (amount != 0) {
            onContentsChanged.run();
        }
    }

    @Override
    public void setBaseVolume(int newBaseVolume) {
        if (newBaseVolume != baseVolume) {
            baseVolume = newBaseVolume;
            onContentsChanged.run();
        }
    }

    @Override
    public int getVolume() {
        return baseVolume;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Air", getAir());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        air = nbt.getInt("Air");
    }
}

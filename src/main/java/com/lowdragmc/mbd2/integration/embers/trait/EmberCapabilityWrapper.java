package com.lowdragmc.mbd2.integration.embers.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmberCapabilityWrapper implements IEmberCapability {
    private final IEmberCapability storage;
    private final IO io;

    public EmberCapabilityWrapper(IEmberCapability storage, IO io) {
        this.storage = storage;
        this.io = io;
    }

    @Override
    public double getEmber() {
        return storage.getEmber();
    }

    @Override
    public double getEmberCapacity() {
        return storage.getEmberCapacity();
    }

    @Override
    public void setEmber(double v) {
        storage.setEmber(v);
    }

    @Override
    public void setEmberCapacity(double v) {
        storage.setEmberCapacity(v);
    }

    @Override
    public double addAmount(double v, boolean b) {
        if (io == IO.IN || io == IO.BOTH) {
            return storage.addAmount(v, b);
        }
        return 0;
    }

    @Override
    public double removeAmount(double v, boolean b) {
        if (io == IO.OUT || io == IO.BOTH) {
            return storage.removeAmount(v, b);
        }
        return 0;
    }

    @Override
    public void writeToNBT(CompoundTag compoundTag) {
        storage.writeToNBT(compoundTag);
    }

    @Override
    public void onContentsChanged() {
        storage.onContentsChanged();
    }

    @Override
    public void invalidate() {
        storage.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return storage.getCapability(cap, side);
    }

    @Override
    public CompoundTag serializeNBT() {
        return storage.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        storage.deserializeNBT(nbt);
    }

    @Override
    public boolean acceptsVolatile() {
        return storage.acceptsVolatile();
    }
}

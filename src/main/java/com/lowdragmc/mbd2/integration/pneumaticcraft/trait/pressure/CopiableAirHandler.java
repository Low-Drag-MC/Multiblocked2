package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.common.capabilities.MachineAirHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CopiableAirHandler extends MachineAirHandler implements INBTSerializable<CompoundTag>, IContentChangeAware {
    @Setter
    public Runnable onContentsChanged = () -> {};

    private final PressureTier tier;
    /**
     * Not final: it fronts the {@code max_pressure} runtime value, which a machine can override.
     * {@code tier} needs no such treatment — the trait hands in a {@link PressureTier} that reads its
     * own slots, and {@code MachineAirHandler} calls through it on every query.
     */
    @Setter
    private float maxPressure;
    private List<Direction> sides = new ArrayList<>();

    public CopiableAirHandler(PressureTier tier, int baseVolume, float maxPressure) {
        this(tier, baseVolume, 0, maxPressure);
    }

    public CopiableAirHandler(PressureTier tier, int baseVolume, float pressure, float maxPressure) {
        super(tier, baseVolume);
        this.tier = tier;
        this.maxPressure = maxPressure;
        super.setPressure(pressure);
    }

    public CopiableAirHandler copy() {
        var copy = new CopiableAirHandler(tier, getBaseVolume(), getPressure(), maxPressure);
        copy.setConnectableFaces(this.sides);
        return copy;
    }

    public float maxPressure() {
        return maxPressure;
    }

    @Override
    public void setConnectableFaces(Collection<Direction> sides) {
        this.sides = new ArrayList<>(sides);
        super.setConnectableFaces(sides);
    }

    /**
     * The faces this handler will connect through. Mirrors what was last handed to
     * {@link #setConnectableFaces}; PneumaticCraft's own base class keeps no readable copy.
     */
    public List<Direction> getConnectableFaces() {
        return java.util.Collections.unmodifiableList(sides);
    }

    @Override
    public void addAir(int amount) {
        super.addAir(amount);
        if (amount != 0) {
            onContentsChanged.run();
        }
    }

    @Override
    public void setBaseVolume(int newBaseVolume) {
        if (newBaseVolume != getBaseVolume()) {
            super.setBaseVolume(newBaseVolume);
            onContentsChanged.run();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return super.serializeNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }
}

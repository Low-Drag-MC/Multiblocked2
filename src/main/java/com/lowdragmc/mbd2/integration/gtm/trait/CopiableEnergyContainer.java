package com.lowdragmc.mbd2.integration.gtm.trait;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

public class CopiableEnergyContainer implements IEnergyContainer, ITagSerializable<Tag>, IContentChangeAware {
    @Getter
    @Setter
    public Runnable onContentsChanged = () -> {};
    private final MBDMachine machine;
    protected final boolean explosionMachine;
    @Getter
    protected final long energyCapacity;
    @Getter
    protected final long inputAmperage;
    @Getter
    protected final long inputVoltage;
    @Getter
    protected final long outputAmperage;
    @Getter
    protected final long outputVoltage;
    @Getter
    protected long energyStored = 0;
    // runtime
    protected long amps;
    protected long lastTS = Long.MIN_VALUE;

    public CopiableEnergyContainer(MBDMachine machine, boolean explosionMachine, long energyCapacity, long inputAmperage, long inputVoltage, long outputAmperage, long outputVoltage) {
        this.machine = machine;
        this.explosionMachine = explosionMachine;
        this.energyCapacity = energyCapacity;
        this.inputAmperage = inputAmperage;
        this.inputVoltage = inputVoltage;
        this.outputAmperage = outputAmperage;
        this.outputVoltage = outputVoltage;
    }

    public CopiableEnergyContainer copy() {
        return new CopiableEnergyContainer(machine, explosionMachine, energyCapacity, inputAmperage, inputVoltage, outputAmperage, outputVoltage);
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        var latestTS = machine.getOffsetTimer();
        if (lastTS < latestTS) {
            amps = 0;
            lastTS = latestTS;
        }
        if (amps >= getInputAmperage()) return 0;
        long canAccept = getEnergyCapacity() - getEnergyStored();
        if (voltage > 0L && (side == null || inputsEnergy(side))) {
            if (voltage > getInputVoltage()) {
                var level = machine.getLevel();
                var pos = machine.getPos();
                var explosionPower = GTUtil.getExplosionPower(voltage);
                level.removeBlock(pos, false);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        explosionPower, explosionMachine ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
                return Math.min(amperage, getInputAmperage() - amps);
            }
            if (canAccept >= voltage) {
                long amperesAccepted = Math.min(canAccept / voltage, Math.min(amperage, getInputAmperage() - amps));
                if (amperesAccepted > 0) {
                    setEnergyStored(getEnergyStored() + voltage * amperesAccepted);
                    amps += amperesAccepted;
                    return amperesAccepted;
                }
            }
        }
        return 0;
    }

    public void setEnergyStored(long energyStored) {
        if (this.energyStored == energyStored) return;
        this.energyStored = energyStored;
        onContentsChanged.run();
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return true;
    }

    @Override
    public long changeEnergy(long energyToAdd) {
        long oldEnergyStored = getEnergyStored();
        long newEnergyStored = (energyCapacity - oldEnergyStored < energyToAdd) ? energyCapacity : (oldEnergyStored + energyToAdd);
        if (newEnergyStored < 0)
            newEnergyStored = 0;
        setEnergyStored(newEnergyStored);
        return newEnergyStored - oldEnergyStored;
    }

    @Override
    public Tag serializeNBT() {
        return LongTag.valueOf(energyStored);
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        if (nbt instanceof LongTag tag) {
            energyStored = tag.getAsLong();
        }
    }
}

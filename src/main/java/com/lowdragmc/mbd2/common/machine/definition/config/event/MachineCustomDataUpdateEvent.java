package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.ICancellableEvent;

//@LDLRegister(name = "MachineCustomDataUpdateEvent", group = "MachineEvent")
public class MachineCustomDataUpdateEvent extends MachineEvent implements ICancellableEvent {
    public final CompoundTag oldValue;
    public final CompoundTag newValue;

    public MachineCustomDataUpdateEvent(MBDMachine machine, CompoundTag newValue, CompoundTag oldValue) {
        super(machine);
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

}

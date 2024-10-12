package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
@LDLRegister(name = "MachineCustomDataUpdateEvent", group = "MachineEvent")
public class MachineCustomDataUpdateEvent extends MachineEvent {
    @GraphParameterGet(displayName = "old data")
    public final CompoundTag oldValue;
    @GraphParameterGet(displayName = "new data")
    public final CompoundTag newValue;

    public MachineCustomDataUpdateEvent(MBDMachine machine, CompoundTag newValue, CompoundTag oldValue) {
        super(machine);
        this.newValue = newValue;
        this.oldValue = oldValue;
    }
}

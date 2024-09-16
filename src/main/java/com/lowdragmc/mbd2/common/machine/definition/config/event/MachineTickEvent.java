package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
@LDLRegister(name = "MachineTickEvent", group = "MachineEvent")
public class MachineTickEvent extends MachineEvent {

    public MachineTickEvent(MBDMachine machine) {
        super(machine);
    }
}

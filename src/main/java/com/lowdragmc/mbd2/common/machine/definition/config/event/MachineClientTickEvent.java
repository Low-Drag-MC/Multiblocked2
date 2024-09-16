package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

@LDLRegister(name = "MachineClientTickEvent", group = "MachineEvent")
public class MachineClientTickEvent extends MachineEvent {

    public MachineClientTickEvent(MBDMachine machine) {
        super(machine);
    }
}

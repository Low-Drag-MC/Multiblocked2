package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

@LDLRegister(name = "MachineStructureFormedEvent", group = "MachineEvent.Multiblock")
public class MachineStructureFormedEvent extends MachineEvent {

    public MachineStructureFormedEvent(MBDMachine machine) {
        super(machine);
    }
}

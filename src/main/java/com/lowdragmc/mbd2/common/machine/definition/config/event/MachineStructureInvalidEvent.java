package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

@LDLRegister(name = "MachineStructureInvalidEvent", group = "MachineEvent.Multiblock")
public class MachineStructureInvalidEvent extends MachineEvent {

    public MachineStructureInvalidEvent(MBDMachine machine) {
        super(machine);
    }
}

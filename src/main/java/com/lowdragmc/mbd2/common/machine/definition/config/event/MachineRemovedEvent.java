package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
@LDLRegister(name = "MachineRemovedEvent", group = "MachineEvent")
public class MachineRemovedEvent extends MachineEvent {

    public MachineRemovedEvent(MBDMachine machine) {
        super(machine);
    }
}

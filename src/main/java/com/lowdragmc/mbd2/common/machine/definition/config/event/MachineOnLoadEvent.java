package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
@LDLRegister(name = "MachineOnLoadEvent", group = "MachineEvent")
public class MachineOnLoadEvent extends MachineEvent {

    public MachineOnLoadEvent(MBDMachine machine) {
        super(machine);
    }

}

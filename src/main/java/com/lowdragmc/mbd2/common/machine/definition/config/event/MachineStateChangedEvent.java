package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;


@Getter
//@LDLRegister(name = "MachineStateChangedEvent", group = "MachineEvent")
public class MachineStateChangedEvent extends MachineEvent implements ICancellableEvent {
    public final String oldState;
    public final String newState;

    public MachineStateChangedEvent(MBDMachine machine, String oldState, String newState) {
        super(machine);
        this.oldState = oldState;
        this.newState = newState;
    }

}

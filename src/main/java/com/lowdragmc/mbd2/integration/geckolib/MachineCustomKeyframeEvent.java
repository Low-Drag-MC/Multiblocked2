package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineCustomKeyframeEvent extends MachineEvent {
    public String instruction;
    public String controllerName;
    public double animationTick;

    public MachineCustomKeyframeEvent(MBDMachine machine, String instruction, String controllerName, double animationTick) {
        super(machine);
        this.instruction = instruction;
        this.controllerName = controllerName;
        this.animationTick = animationTick;
    }
}

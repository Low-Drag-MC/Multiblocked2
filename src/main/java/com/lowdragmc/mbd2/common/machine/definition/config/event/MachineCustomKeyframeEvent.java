package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.geckolib.AnimatableMachine;
import lombok.Getter;
import lombok.Setter;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;

@Getter
@Setter
@LDLRegister(name = "MachineCustomKeyframeEvent", group = "MachineEvent", modID = "geckolib")
public class MachineCustomKeyframeEvent extends MachineEvent {
    public CustomInstructionKeyframeEvent<AnimatableMachine> event;
    @GraphParameterGet
    public String instruction;

    public MachineCustomKeyframeEvent(MBDMachine machine, CustomInstructionKeyframeEvent<AnimatableMachine> event) {
        super(machine);
        this.event = event;

        instruction = event.getKeyframeData().getInstructions();
    }

}

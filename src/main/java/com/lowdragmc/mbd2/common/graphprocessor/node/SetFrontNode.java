package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;

@LDLRegister(name = "set machine front", group = "graph_processor.node.mbd2.machine")
public class SetFrontNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort
    public Direction front;

    @Override
    protected void process() {
        if (machine != null && front != null) {
            machine.setFrontFacing(front);
        }
    }
}

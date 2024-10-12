package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

@LDLRegister(name = "set machine status", group = "graph_processor.node.mbd2.machine")
public class SetStatusNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort
    public String status;

    @Override
    protected void process() {
        if (machine != null && status != null) {
            machine.setMachineState(status);
        }
    }
}

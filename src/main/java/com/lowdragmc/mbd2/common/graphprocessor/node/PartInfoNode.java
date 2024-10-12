package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;

import java.util.List;

@LDLRegister(name = "part info", group = "graph_processor.node.mbd2.machine")
public class PartInfoNode extends BaseNode {
    @InputPort
    public MBDMachine machine;
    @OutputPort(name = "is formed")
    public boolean isFormed;
    @OutputPort
    public List<MBDMultiblockMachine> controllers;

    @Override
    protected void process() {
        if (machine instanceof MBDPartMachine part) {
            isFormed = part.isFormed();
            controllers = part.getControllers().stream().filter(MBDMultiblockMachine.class::isInstance).map(MBDMultiblockMachine.class::cast).toList();
        }
    }
}

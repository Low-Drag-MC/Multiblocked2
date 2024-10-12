package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;

import java.util.List;

@LDLRegister(name = "multiblock info", group = "graph_processor.node.mbd2.machine")
public class MultiblockInfoNode extends BaseNode {
    @InputPort
    public MBDMachine machine;
    @OutputPort(name = "is formed")
    public boolean isFormed;
    @InputPort
    public List<MBDMachine> parts;

    @Override
    protected void process() {
        if (machine instanceof MBDMultiblockMachine multiblock) {
            isFormed = multiblock.isFormed();
            parts = multiblock.getParts().stream().filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).toList();
        }
    }
}

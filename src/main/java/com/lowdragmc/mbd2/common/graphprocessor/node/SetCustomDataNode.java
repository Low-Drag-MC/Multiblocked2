package com.lowdragmc.mbd2.common.graphprocessor.node;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.nbt.CompoundTag;

@LDLRegister(name = "set custom data", group = "graph_processor.node.mbd2.machine")
public class SetCustomDataNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort
    public CompoundTag data;

    @Override
    protected void process() {
        if (machine != null) {
            machine.setCustomData(data);
        }
    }
}

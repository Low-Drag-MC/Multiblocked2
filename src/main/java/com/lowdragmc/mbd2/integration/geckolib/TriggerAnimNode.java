package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

@LDLRegister(name = "trigger geckolib anima", group = "graph_processor.node.mbd2.machine.geckolib", modID = "geckolib")
public class TriggerAnimNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort(name = "animation name")
    public String animName;

    @Override
    protected void process() {
        if (machine != null && animName != null) {
            machine.triggerGeckolibAnim(animName);
        }
    }
}

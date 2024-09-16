package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

@LDLRegister(name = "kill photon fx", group = "graph_processor.node.mbd2.machine.photon", modID = "photon")
public class KillPhotonFXNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort(name = "identifier", tips = "graph_processor.node.mbd2.machine.photon.identifier")
    public String identifier;
    @InputPort(name = "forced death", tips = {"graph_processor.node.mbd2.machine.photon.force_death.tips.0",
    "graph_processor.node.mbd2.machine.photon.force_death.tips.1", "graph_processor.node.mbd2.machine.photon.force_death.tips.2"})
    public boolean forcedDeath;

    @Override
    protected void process() {
        if (machine != null && identifier != null) {
            machine.killPhotonFx(identifier, forcedDeath);
        }
    }
}

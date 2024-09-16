package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

@LDLRegister(name = "emit photon fx", group = "graph_processor.node.mbd2.machine.photon", modID = "photon")
public class EmitPhotonFXNode extends LinearTriggerNode {
    @InputPort
    public MBDMachine machine;
    @InputPort(name = "identifier", tips = "graph_processor.node.mbd2.machine.photon.identifier")
    public String identifier;
    @InputPort(name = "fx location")
    public String fxLocation;
    @InputPort
    public Vector3f offset;
    @InputPort
    public Vector3f rotation;
    @InputPort
    public int delay;
    @InputPort(name = "forced death", tips = {"graph_processor.node.mbd2.machine.photon.force_death.tips.0",
    "graph_processor.node.mbd2.machine.photon.force_death.tips.1", "graph_processor.node.mbd2.machine.photon.force_death.tips.2"})
    public boolean forcedDeath;

    @Override
    protected void process() {
        if (machine != null && identifier != null && fxLocation != null && ResourceLocation.isValidResourceLocation(fxLocation)) {
            machine.emitPhotonFx(identifier, new ResourceLocation(fxLocation),
                    offset == null ? new Vector3f(): offset,
                    rotation == null ? new Vector3f(): rotation, delay, forcedDeath);
        }
    }
}

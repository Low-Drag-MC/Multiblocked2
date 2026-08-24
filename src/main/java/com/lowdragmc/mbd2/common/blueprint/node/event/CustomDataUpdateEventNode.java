package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineCustomDataUpdateEvent;
import net.minecraft.nbt.CompoundTag;

/** Fires when the machine's custom data tag is replaced, on both sides. */
@NodeAttribute(name = "mbd2_event_custom_data_update", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class CustomDataUpdateEventNode extends MachineEventNode<MachineCustomDataUpdateEvent> {

    @OutputPort
    public CompoundTag oldValue;
    @OutputPort
    public CompoundTag newValue;

    @Override
    public Class<MachineCustomDataUpdateEvent> eventClass() {
        return MachineCustomDataUpdateEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineCustomDataUpdateEvent event) {
        ctx.setOutput("oldValue", event.oldValue);
        ctx.setOutput("newValue", event.newValue);
    }
}

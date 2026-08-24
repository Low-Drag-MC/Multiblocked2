package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRemovedEvent;

/** Fires when the machine block is removed from the world. */
@NodeAttribute(name = "mbd2_event_removed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class RemovedEventNode extends MachineEventNode<MachineRemovedEvent> {
    @Override
    public Class<MachineRemovedEvent> eventClass() {
        return MachineRemovedEvent.class;
    }
}

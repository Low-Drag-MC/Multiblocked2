package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineStructureFormedEvent;

/** Fires on a multiblock controller when its structure completes. */
@NodeAttribute(name = "mbd2_event_structure_formed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class StructureFormedEventNode extends MachineEventNode<MachineStructureFormedEvent> {
    @Override
    public Class<MachineStructureFormedEvent> eventClass() {
        return MachineStructureFormedEvent.class;
    }
}

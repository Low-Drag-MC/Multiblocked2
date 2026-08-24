package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineStructureInvalidEvent;

/** Fires on a multiblock controller when its structure breaks. */
@NodeAttribute(name = "mbd2_event_structure_invalid", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class StructureInvalidEventNode extends MachineEventNode<MachineStructureInvalidEvent> {
    @Override
    public Class<MachineStructureInvalidEvent> eventClass() {
        return MachineStructureInvalidEvent.class;
    }
}

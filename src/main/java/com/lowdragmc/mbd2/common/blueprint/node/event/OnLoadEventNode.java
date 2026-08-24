package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOnLoadEvent;

/** Fires once on the server a tick after the machine's block entity becomes valid in its chunk. */
@NodeAttribute(name = "mbd2_event_on_load", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OnLoadEventNode extends MachineEventNode<MachineOnLoadEvent> {
    @Override
    public Class<MachineOnLoadEvent> eventClass() {
        return MachineOnLoadEvent.class;
    }
}

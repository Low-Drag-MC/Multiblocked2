package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineClientTickEvent;

/**
 * Fires every client tick.
 *
 * <p>Runs on the client only, so anything downstream that changes world state will desync. Use it for
 * sound, particles and animation; use {@code Machine Tick} for logic.</p>
 */
@NodeAttribute(name = "mbd2_event_client_tick", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class ClientTickEventNode extends MachineEventNode<MachineClientTickEvent> {
    @Override
    public Class<MachineClientTickEvent> eventClass() {
        return MachineClientTickEvent.class;
    }
}

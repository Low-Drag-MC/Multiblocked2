package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineTickEvent;

/**
 * Fires every server tick, before the machine's recipe logic runs.
 *
 * <p>Cancelable: cancelling skips the machine's own tick, including its recipe logic. Use
 * {@code Cancel Event} downstream.</p>
 */
@NodeAttribute(name = "mbd2_event_tick", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class TickEventNode extends MachineEventNode<MachineTickEvent> {
    @Override
    public Class<MachineTickEvent> eventClass() {
        return MachineTickEvent.class;
    }
}

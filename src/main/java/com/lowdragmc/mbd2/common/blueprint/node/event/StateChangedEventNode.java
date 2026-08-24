package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineStateChangedEvent;

/**
 * Fires before the machine's state changes.
 *
 * <p>Cancelable: cancelling keeps the machine in {@code oldState}. Note the state has <em>not</em>
 * changed yet when this runs, so reading the machine's state here gives {@code oldState}.</p>
 */
@NodeAttribute(name = "mbd2_event_state_changed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class StateChangedEventNode extends MachineEventNode<MachineStateChangedEvent> {

    @OutputPort
    public String oldState;
    @OutputPort
    public String newState;

    @Override
    public Class<MachineStateChangedEvent> eventClass() {
        return MachineStateChangedEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineStateChangedEvent event) {
        ctx.setOutput("oldState", event.oldState);
        ctx.setOutput("newState", event.newState);
    }
}

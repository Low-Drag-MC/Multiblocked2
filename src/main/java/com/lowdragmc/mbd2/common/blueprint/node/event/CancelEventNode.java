package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Cancel the event currently being dispatched.
 *
 * <p>What cancelling means is the event's own business — a cancelled {@code Machine Tick} skips the
 * machine's tick, a cancelled {@code Recipe Modify (Before)} rejects the recipe, a cancelled
 * {@code Open UI} keeps the UI shut. Events that are not cancelable ignore this node.</p>
 *
 * <p>{@code cancel} is an input rather than implied so a blueprint can also <em>un</em>-cancel an event
 * an earlier blueprint in the list cancelled.</p>
 */
@NodeAttribute(name = "mbd2_event_cancel", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class CancelEventNode extends MachineActionNode {

    @InputPort
    public boolean cancel = true;

    @Override
    protected void run(ExecContext ctx, MachineEnvironment env) {
        if (env.getCurrentEvent() instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(ctx.getInput("cancel", Boolean.class, true));
        }
    }
}

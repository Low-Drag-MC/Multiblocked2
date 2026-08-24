package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseWithoutItemEvent;
import net.minecraft.world.InteractionResult;

/**
 * Decide what an empty-handed right-click does. Only meaningful under {@code Use Without Item}.
 *
 * <p>{@code PASS} (the default the machine sets before dispatch) lets the machine carry on and open its
 * UI; {@code SUCCESS}/{@code CONSUME} take the click.</p>
 */
@NodeAttribute(name = "mbd2_event_set_interaction_result", group = "mbd2/event",
        graphTypes = MachineBlueprintGraph.class)
public class SetInteractionResultNode extends MachineActionNode {

    @InputPort
    public InteractionResult result = InteractionResult.SUCCESS;

    @Override
    protected void run(ExecContext ctx, MachineEnvironment env) {
        if (env.getCurrentEvent() instanceof MachineUseWithoutItemEvent event) {
            var value = ctx.getInput("result", InteractionResult.class, InteractionResult.SUCCESS);
            event.setInteractionResult(value);
        }
    }
}

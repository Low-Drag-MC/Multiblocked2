package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseItemOnEvent;
import net.minecraft.world.ItemInteractionResult;

/**
 * Decide what a right-click-with-item does. Only meaningful under {@code Use Item On}.
 *
 * <p>{@code PASS_TO_DEFAULT_BLOCK_INTERACTION} (the default the machine sets before dispatch) lets the
 * machine carry on and open its UI; {@code SUCCESS} consumes the click; {@code FAIL} rejects it.</p>
 */
@NodeAttribute(name = "mbd2_event_set_item_interaction_result", group = "mbd2/event",
        graphTypes = MachineBlueprintGraph.class)
public class SetItemInteractionResultNode extends MachineActionNode {

    @InputPort
    public ItemInteractionResult result = ItemInteractionResult.SUCCESS;

    @Override
    protected void run(ExecContext ctx, MachineEnvironment env) {
        if (env.getCurrentEvent() instanceof MachineUseItemOnEvent event) {
            var value = ctx.getInput("result", ItemInteractionResult.class, ItemInteractionResult.SUCCESS);
            event.setItemInteractionResult(value);
        }
    }
}

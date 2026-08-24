package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineFuelRecipeModifyEvent;

/**
 * Fires before a fuel recipe is accepted, so a blueprint can change or reject it.
 *
 * <p>Same shape as {@code Recipe Modify (Before)}: write back with {@code Set Event Recipe}, reject
 * with {@code Cancel Event}.</p>
 */
@NodeAttribute(name = "mbd2_event_fuel_recipe_modify", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class FuelRecipeModifyEventNode extends MachineEventNode<MachineFuelRecipeModifyEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineFuelRecipeModifyEvent> eventClass() {
        return MachineFuelRecipeModifyEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineFuelRecipeModifyEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

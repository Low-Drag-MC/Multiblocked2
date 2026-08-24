package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOnRecipeWaitingEvent;

/** Fires each tick the recipe logic is waiting - a recipe matched but cannot proceed (missing inputs, full outputs, not enough energy). */
@NodeAttribute(name = "mbd2_event_on_recipe_waiting", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OnRecipeWaitingEventNode extends MachineEventNode<MachineOnRecipeWaitingEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineOnRecipeWaitingEvent> eventClass() {
        return MachineOnRecipeWaitingEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineOnRecipeWaitingEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

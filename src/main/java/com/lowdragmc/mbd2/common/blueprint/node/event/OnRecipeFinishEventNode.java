package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOnRecipeFinishEvent;

/** Fires when a recipe completes and its outputs have been produced. */
@NodeAttribute(name = "mbd2_event_on_recipe_finish", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OnRecipeFinishEventNode extends MachineEventNode<MachineOnRecipeFinishEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineOnRecipeFinishEvent> eventClass() {
        return MachineOnRecipeFinishEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineOnRecipeFinishEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

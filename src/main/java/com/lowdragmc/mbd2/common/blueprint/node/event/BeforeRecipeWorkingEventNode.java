package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineBeforeRecipeWorkingEvent;

/** Fires each tick a recipe is running, before its progress advances. Cancelable: cancelling holds the recipe at its current progress. */
@NodeAttribute(name = "mbd2_event_before_recipe_working", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class BeforeRecipeWorkingEventNode extends MachineEventNode<MachineBeforeRecipeWorkingEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineBeforeRecipeWorkingEvent> eventClass() {
        return MachineBeforeRecipeWorkingEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineBeforeRecipeWorkingEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

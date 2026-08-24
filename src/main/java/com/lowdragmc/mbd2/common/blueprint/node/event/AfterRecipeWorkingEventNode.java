package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineAfterRecipeWorkingEvent;

/** Fires each tick a recipe is running, after its progress has advanced. */
@NodeAttribute(name = "mbd2_event_after_recipe_working", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class AfterRecipeWorkingEventNode extends MachineEventNode<MachineAfterRecipeWorkingEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineAfterRecipeWorkingEvent> eventClass() {
        return MachineAfterRecipeWorkingEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineAfterRecipeWorkingEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

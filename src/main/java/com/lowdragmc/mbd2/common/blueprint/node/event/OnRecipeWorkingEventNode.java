package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOnRecipeWorkingEvent;

/**
 * Fires each tick a recipe is running, with the progress it has reached.
 *
 * <p>Cancelable: cancelling stops progress advancing this tick.</p>
 */
@NodeAttribute(name = "mbd2_event_on_recipe_working", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OnRecipeWorkingEventNode extends MachineEventNode<MachineOnRecipeWorkingEvent> {

    @OutputPort
    public MBDRecipe recipe;
    @OutputPort
    public int progress;

    @Override
    public Class<MachineOnRecipeWorkingEvent> eventClass() {
        return MachineOnRecipeWorkingEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineOnRecipeWorkingEvent event) {
        ctx.setOutput("recipe", event.recipe);
        ctx.setOutput("progress", event.progress);
    }
}

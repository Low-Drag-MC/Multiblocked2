package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeModifyEvent;

/**
 * Fires before the definition's own recipe modifiers run, with the recipe as matched.
 *
 * <p>This is where a blueprint changes a recipe: build a new one (usually {@code Copy Recipe} plus a
 * {@code Content Modifier}) and hand it to {@code Set Event Recipe}. Cancelable: cancelling rejects
 * the recipe outright.</p>
 */
@NodeAttribute(name = "mbd2_event_recipe_modify_before", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class RecipeModifyBeforeEventNode extends MachineEventNode<MachineRecipeModifyEvent.Before> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineRecipeModifyEvent.Before> eventClass() {
        return MachineRecipeModifyEvent.Before.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineRecipeModifyEvent.Before event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

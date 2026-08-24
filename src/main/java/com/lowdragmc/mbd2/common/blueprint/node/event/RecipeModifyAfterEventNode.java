package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeModifyEvent;

/**
 * Fires after the definition's own recipe modifiers have run, with the resulting recipe.
 *
 * <p>Use it to have the last word on a recipe the definition's modifiers already touched. Not
 * cancelable — reject a recipe from {@code Recipe Modify (Before)} instead.</p>
 */
@NodeAttribute(name = "mbd2_event_recipe_modify_after", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class RecipeModifyAfterEventNode extends MachineEventNode<MachineRecipeModifyEvent.After> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineRecipeModifyEvent.After> eventClass() {
        return MachineRecipeModifyEvent.After.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineRecipeModifyEvent.After event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

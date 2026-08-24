package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineFuelRecipeModifyEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeModifyEvent;

/**
 * Write a recipe back onto the event being dispatched.
 *
 * <p>Only meaningful under {@code Recipe Modify (Before)}, {@code Recipe Modify (After)} and
 * {@code Fuel Recipe Modify} — those are the events whose recipe the machine reads back. Under any
 * other event this does nothing.</p>
 *
 * <p>Setting {@code null} rejects the recipe, the same as cancelling.</p>
 */
@NodeAttribute(name = "mbd2_event_set_recipe", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class SetEventRecipeNode extends MachineActionNode {

    @InputPort
    public MBDRecipe recipe;

    @Override
    protected void run(ExecContext ctx, MachineEnvironment env) {
        var value = ctx.getInput("recipe", MBDRecipe.class, null);
        var event = env.getCurrentEvent();
        if (event instanceof MachineRecipeModifyEvent modify) {
            modify.setRecipe(value);
        } else if (event instanceof MachineFuelRecipeModifyEvent fuel) {
            fuel.setRecipe(value);
        }
    }
}

package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoContextNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import org.jetbrains.annotations.Nullable;

/**
 * Holds an {@link MBDRecipe} for the blocks in {@link RecipeInfoBlocks} to read.
 *
 * <p>Leave {@code target} unwired to read the recipe the blueprint's own machine is running. Under a
 * {@code Recipe Modify} event you almost always want to wire the event's {@code recipe} instead — the
 * machine's running recipe is the previous one at that point.</p>
 */
@NodeAttribute(name = "mbd2_recipe_info", group = "mbd2/recipe", graphTypes = MachineBlueprintGraph.class)
public class RecipeInfoNode extends MachineInfoContextNode<MBDRecipe> {

    @Override
    protected Class<MBDRecipe> targetClass() {
        return MBDRecipe.class;
    }

    @Override
    @Nullable
    protected MBDRecipe defaultTarget(EvalContext ctx) {
        var machine = MachineNodes.ownMachine(ctx.getExecutor());
        return machine == null ? null : machine.getRecipeLogic().getLastRecipe();
    }
}

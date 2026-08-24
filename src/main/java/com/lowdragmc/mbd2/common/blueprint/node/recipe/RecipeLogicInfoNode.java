package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoContextNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a {@link RecipeLogic} for the blocks in {@link RecipeLogicInfoBlocks} to read — status,
 * progress, the recipe it is running, why it is waiting.
 *
 * <p>Leave {@code target} unwired to read the blueprint's own machine's recipe logic.</p>
 */
@NodeAttribute(name = "mbd2_recipe_logic_info", group = "mbd2/recipe/logic",
        graphTypes = MachineBlueprintGraph.class)
public class RecipeLogicInfoNode extends MachineInfoContextNode<RecipeLogic> {

    @Override
    protected Class<RecipeLogic> targetClass() {
        return RecipeLogic.class;
    }

    @Override
    @Nullable
    protected RecipeLogic defaultTarget(EvalContext ctx) {
        var machine = MachineNodes.ownMachine(ctx.getExecutor());
        return machine == null ? null : machine.getRecipeLogic();
    }
}

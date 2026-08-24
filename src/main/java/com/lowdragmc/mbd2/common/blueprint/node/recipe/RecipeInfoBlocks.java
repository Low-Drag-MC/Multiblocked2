package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** The properties of an {@link MBDRecipe}, one block each. */
public final class RecipeInfoBlocks {

    private static final String GROUP = "mbd2/recipe";

    private RecipeInfoBlocks() {}

    private abstract static class RecipeBlock extends MachineInfoBlock<MBDRecipe> {
        @Override
        protected final Class<MBDRecipe> targetClass() {
            return MBDRecipe.class;
        }
    }

    /** The recipe's id. For a modified recipe this is the original's id with a suffix. */
    @NodeAttribute(name = "mbd2_recipe_id", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class Id extends RecipeBlock {
        @OutputPort public ResourceLocation value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.id);
        }
    }

    /** How many ticks the recipe takes. */
    @NodeAttribute(name = "mbd2_recipe_duration", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class Duration extends RecipeBlock {
        @OutputPort public int value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.duration);
        }
    }

    /** Which recipe wins when several match. Higher goes first. */
    @NodeAttribute(name = "mbd2_recipe_priority", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class Priority extends RecipeBlock {
        @OutputPort public int value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.priority);
        }
    }

    /** The recipe type this recipe belongs to. */
    @NodeAttribute(name = "mbd2_recipe_type", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class Type extends RecipeBlock {
        @OutputPort public MBDRecipeType value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.recipeType);
        }
    }

    /** The recipe's free-form data tag, as authored in the recipe editor. */
    @NodeAttribute(name = "mbd2_recipe_data", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class Data extends RecipeBlock {
        @OutputPort public CompoundTag value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.data);
        }
    }

    /** Whether something has already modified this recipe — a definition modifier, or another blueprint. */
    @NodeAttribute(name = "mbd2_recipe_is_modified", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class IsModified extends RecipeBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.isModified());
        }
    }

    /** Whether the recipe is hidden from JEI/REI/EMI. */
    @NodeAttribute(name = "mbd2_recipe_is_xei_hidden", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class IsXEIHidden extends RecipeBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.isXEIHidden);
        }
    }

    /** Whether the recipe consumes or produces anything on every tick, as opposed to only at its ends. */
    @NodeAttribute(name = "mbd2_recipe_has_tick", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeInfoNode.class)
    public static class HasTick extends RecipeBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(MBDRecipe recipe, EvalContext ctx) {
            ctx.setOutput("value", recipe.hasTick());
        }
    }
}

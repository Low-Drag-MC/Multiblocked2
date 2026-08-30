package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Changing <em>what</em> a recipe consumes and produces, not just how much.
 *
 * <h2>Why these are separate from {@link RecipeBuildNodes}</h2>
 * Those scale a recipe: every content keeps its type and only its amount moves, which is what an
 * overclock or a parallel needs. These replace the contents outright — a machine that outputs raw ore
 * instead of an ingot, or accepts a substitute when the real input is unavailable. Nothing else in the
 * node set could express that, so it was a drop to KubeJS.
 *
 * <h2>Editing is safe because the engine re-matches</h2>
 * {@code Recipe Modify (Before)} fires after a recipe has matched, which makes "swap an input" sound
 * dangerous — the machine matched on iron and would then be asked for copper. It is not:
 * {@code RecipeLogic} re-runs {@code modified.matchRecipe(machine)} on the result of
 * {@code doModifyRecipe} and only starts if that succeeds, and consumption runs off the modified
 * recipe. A swap the machine cannot satisfy simply does not run.
 *
 * <h2>What they do not do</h2>
 * XEI still shows the recipe as authored. A per-machine, per-tick modification has no place in a
 * static recipe list, which is equally true of the duration the overclock built-in rewrites — worth
 * saying because a swapped output is much more visible to a player than a shorter one.
 *
 * <h2>Items and fluids get their own nodes</h2>
 * A {@link Content}'s payload is an {@code Object} whose real type only its {@link RecipeCapability}
 * knows — {@code SizedIngredient} for items, {@code SizedFluidIngredient} for fluids. One generic
 * node would have to take {@code Object} and would fail at runtime on a mismatch; a node per
 * capability keeps the port typed and the conversion at the boundary.
 */
public final class RecipeContentNodes {

    private static final String GROUP = "mbd2/recipe";

    private RecipeContentNodes() {}

    // ---- reading ------------------------------------------------------------------------------

    /**
     * The item contents of one side of a recipe, as the stacks they accept or produce.
     *
     * <p>A projection, not the contents themselves: a recipe input is an {@code Ingredient}, which may
     * be a tag matching many items, and this reports the first stack of each. That is the right answer
     * for the common "what does this make" question and the wrong one for a tag input — use the count
     * to branch, and treat the stacks as representative rather than exhaustive.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_items", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Items extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public List<ItemStack> stacks;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            var stacks = new ArrayList<ItemStack>();
            for (var content : contentsOf(ctx, ItemRecipeCapability.CAP)) {
                if (content.content instanceof SizedIngredient sized) {
                    var matching = sized.ingredient().getItems();
                    stacks.add(matching.length == 0
                            ? ItemStack.EMPTY
                            : matching[0].copyWithCount(sized.count()));
                }
            }
            ctx.setOutput("stacks", stacks);
            ctx.setOutput("count", stacks.size());
        }
    }

    /** The fluid contents of one side of a recipe, as the stacks they accept or produce. @see Items */
    @NodeAttribute(name = "mbd2_recipe_fluids", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Fluids extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public List<FluidStack> stacks;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            var stacks = new ArrayList<FluidStack>();
            for (var content : contentsOf(ctx, FluidRecipeCapability.CAP)) {
                if (content.content instanceof SizedFluidIngredient sized) {
                    var matching = sized.getFluids();
                    stacks.add(matching.length == 0 ? FluidStack.EMPTY : matching[0]);
                }
            }
            ctx.setOutput("stacks", stacks);
            ctx.setOutput("count", stacks.size());
        }
    }

    // ---- writing ------------------------------------------------------------------------------

    /**
     * A copy of the recipe with one more item on the chosen side.
     *
     * <p>{@code chance} is the recipe's own chance field, so a bonus added here shows up as a chance
     * output the way an authored one does — unlike inserting into a slot after the fact, which the
     * recipe never knows about.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_add_item", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AddItem extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public ItemStack item = ItemStack.EMPTY;
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var stack = ctx.getInput("item", ItemStack.class, ItemStack.EMPTY);
            if (stack == null || stack.isEmpty()) {
                ctx.setOutput("result", ctx.getInput("recipe", MBDRecipe.class, null));
                return;
            }
            var content = new Content(
                    new SizedIngredient(Ingredient.of(stack), stack.getCount()),
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    0f);
            ctx.setOutput("result", withAdded(ctx, ItemRecipeCapability.CAP, content));
        }
    }

    /** A copy of the recipe with one more fluid on the chosen side. @see AddItem */
    @NodeAttribute(name = "mbd2_recipe_add_fluid", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AddFluid extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public FluidStack fluid = FluidStack.EMPTY;
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var stack = ctx.getInput("fluid", FluidStack.class, FluidStack.EMPTY);
            if (stack == null || stack.isEmpty()) {
                ctx.setOutput("result", ctx.getInput("recipe", MBDRecipe.class, null));
                return;
            }
            var content = new Content(
                    SizedFluidIngredient.of(stack.getFluid(), stack.getAmount()),
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    0f);
            ctx.setOutput("result", withAdded(ctx, FluidRecipeCapability.CAP, content));
        }
    }

    /**
     * A copy of the recipe with every item content removed from the chosen side.
     *
     * <p>The other half of a replacement: clear, then add. Kept separate from {@code Add} because
     * "also produce this" and "produce this instead" are different intents, and a node that always
     * replaced would make the first one impossible.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_clear_items", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearItems extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("result", withCleared(ctx, ItemRecipeCapability.CAP));
        }
    }

    /** A copy of the recipe with every fluid content removed from the chosen side. @see ClearItems */
    @NodeAttribute(name = "mbd2_recipe_clear_fluids", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearFluids extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("result", withCleared(ctx, FluidRecipeCapability.CAP));
        }
    }

    // ---- shared -------------------------------------------------------------------------------

    /** The contents of the chosen side for one capability, or empty when there is no recipe. */
    private static List<Content> contentsOf(EvalContext ctx, RecipeCapability<?> capability) {
        var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
        if (recipe == null) return List.of();
        var side = sideOf(recipe, ctx.getInput("io", IO.class, IO.OUT));
        if (side == null) return List.of();
        var contents = side.get(capability);
        return contents == null ? List.of() : contents;
    }

    /**
     * The map for one side. {@link IO#BOTH} has no single answer here — a content belongs to the
     * input list or the output list, never to both — so it is treated as OUT rather than silently
     * editing two sides at once.
     */
    @Nullable
    private static Map<RecipeCapability<?>, List<Content>> sideOf(MBDRecipe recipe, IO io) {
        return io == IO.IN ? recipe.inputs : recipe.outputs;
    }

    /**
     * A shallow copy of the recipe with {@code content} appended to one capability's list.
     *
     * <p>Copied rather than edited in place because the recipe handed to {@code Recipe Modify} is the
     * one the recipe manager holds: mutating it would change the recipe for every machine in the
     * world, permanently, until the next reload.</p>
     */
    @Nullable
    private static MBDRecipe withAdded(EvalContext ctx, RecipeCapability<?> capability, Content content) {
        var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
        if (recipe == null) return null;
        var copied = recipe.copy();
        var side = sideOf(copied, ctx.getInput("io", IO.class, IO.OUT));
        if (side == null) return copied;
        var contents = new ArrayList<>(side.getOrDefault(capability, List.of()));
        contents.add(content);
        side.put(capability, contents);
        return copied;
    }

    /** A shallow copy of the recipe with one capability's list emptied on one side. @see #withAdded */
    @Nullable
    private static MBDRecipe withCleared(EvalContext ctx, RecipeCapability<?> capability) {
        var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
        if (recipe == null) return null;
        var copied = recipe.copy();
        var side = sideOf(copied, ctx.getInput("io", IO.class, IO.OUT));
        if (side != null) side.remove(capability);
        return copied;
    }
}

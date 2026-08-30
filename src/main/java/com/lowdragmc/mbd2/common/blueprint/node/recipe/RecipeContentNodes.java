package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.Option;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
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
 * node set could express that, so it meant dropping to KubeJS.
 *
 * <h2>Generic where it can be, typed only where it must</h2>
 * A recipe's contents are {@code Map<RecipeCapability<?>, List<Content>>}, so <em>adding</em>,
 * <em>clearing</em> and <em>counting</em> are the same operation whatever the capability — those nodes
 * take the capability as a dropdown fed by {@link MBDRegistries#RECIPE_CAPABILITIES} and work for
 * every registered one, including capabilities another mod adds after this file was written.
 *
 * <p>Only <em>constructing</em> a {@link Content} has to be typed, and unavoidably so: a Content's
 * payload is an {@code Object} whose real class only its capability knows ({@code SizedIngredient} for
 * items, {@code SizedFluidIngredient} for fluids, {@code Integer} for energy), and a Content does not
 * record which capability it belongs to. A single generic constructor would have to take
 * {@code Object} and fail at runtime on a mismatch, and it would have no sensible inline editor —
 * whereas {@code Content Of Item} can offer an item widget. A mod adding a capability adds one
 * constructor node and everything else here already works with it.
 *
 * <h2>Editing is safe because the engine re-matches</h2>
 * {@code Recipe Modify (Before)} fires after a recipe has matched, which makes "swap an input" sound
 * dangerous — the machine matched on iron and would then be asked for copper. It is not:
 * {@code RecipeLogic} re-runs {@code modified.matchRecipe(machine)} on the result of
 * {@code doModifyRecipe} and only starts if that succeeds, and consumption runs off the modified
 * recipe. A swap the machine cannot satisfy simply does not run.
 *
 * <h2>Pure data nodes, like the rest of the recipe family</h2>
 * None of these has exec pins, matching {@code Copy}, {@code Scale}, {@code Set Recipe Duration} and
 * the rest of {@link RecipeBuildNodes}: they are transformations of a recipe value, and
 * {@code Set Event Recipe} is the single exec node that commits the result. Every write returns a
 * {@link MBDRecipe#copy()} — the recipe handed to {@code Recipe Modify} is the recipe manager's own
 * object, so editing it in place would change that recipe for every machine in the world until the
 * next reload.
 *
 * <h2>What they do not do</h2>
 * XEI still shows the recipe as authored. A per-machine modification has no place in a static recipe
 * list, which is equally true of the duration the overclock built-in rewrites — worth saying because
 * a swapped output is much more visible to a player than a shorter one.
 */
public final class RecipeContentNodes {

    private static final String GROUP = "mbd2/recipe";
    /** The option every capability-generic node carries. */
    private static final String CAPABILITY = "capability";

    private RecipeContentNodes() {}

    /** Base for the nodes that name a capability by a dropdown rather than by being typed to one. */
    private abstract static class CapabilityNode extends AnnotatedNode {
        @Option public String capability = ItemRecipeCapability.CAP.name;

        @Override
        public List<String> optionChoices(String optionId) {
            return CAPABILITY.equals(optionId)
                    ? MBDRegistries.RECIPE_CAPABILITIES.keys().stream().sorted().toList()
                    : List.of();
        }

        /** The chosen capability, or null when the option names one that is not registered. */
        @Nullable
        protected RecipeCapability<?> capability(EvalContext ctx) {
            return MBDRegistries.RECIPE_CAPABILITIES.get(
                    ctx.getOption(CAPABILITY, String.class, ItemRecipeCapability.CAP.name));
        }
    }

    // ---- generic: add, clear, count ------------------------------------------------------------

    /**
     * A copy of the recipe with one more content on the chosen side.
     *
     * <p>Pair with a {@code Content Of ...} node for the value. Adding rather than replacing is the
     * default because "also produce this" is as common as "produce this instead" — compose it with
     * {@code Clear Recipe Contents} for the latter.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_add_content", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AddContent extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public Content content;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            var content = ctx.getInput("content", Content.class, null);
            var capability = capability(ctx);
            if (recipe == null || content == null || capability == null) {
                ctx.setOutput("result", recipe);
                return;
            }
            var copied = recipe.copy();
            var side = sideOf(copied, ctx.getInput("io", IO.class, IO.OUT));
            var contents = new ArrayList<>(side.getOrDefault(capability, List.of()));
            contents.add(content);
            side.put(capability, contents);
            ctx.setOutput("result", copied);
        }
    }

    /**
     * A copy of the recipe with every content of the chosen capability removed from one side.
     *
     * <p>The other half of a replacement: clear, then add. On its own it makes a side stop consuming
     * or producing that capability entirely.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_clear_contents", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearContents extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            var capability = capability(ctx);
            if (recipe == null || capability == null) {
                ctx.setOutput("result", recipe);
                return;
            }
            var copied = recipe.copy();
            sideOf(copied, ctx.getInput("io", IO.class, IO.OUT)).remove(capability);
            ctx.setOutput("result", copied);
        }
    }

    /** How many contents of the chosen capability one side of a recipe has. */
    @NodeAttribute(name = "mbd2_recipe_content_count", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentCount extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            var capability = capability(ctx);
            ctx.setOutput("count", capability == null ? 0 : contentsOf(ctx, capability).size());
        }
    }

    // ---- typed: constructing a content, and reading one back -----------------------------------

    /**
     * A recipe content holding an item.
     *
     * <p>{@code chance} is the recipe's own chance field, so a bonus built here behaves like an
     * authored chance output rather than like an item pushed into a slot afterwards.</p>
     */
    @NodeAttribute(name = "mbd2_content_of_item", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentOfItem extends AnnotatedNode {
        @InputPort public ItemStack item = ItemStack.EMPTY;
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @OutputPort public Content content;

        @Override
        public void evaluate(EvalContext ctx) {
            var stack = ctx.getInput("item", ItemStack.class, ItemStack.EMPTY);
            ctx.setOutput("content", stack == null || stack.isEmpty() ? null : new Content(
                    new SizedIngredient(Ingredient.of(stack), stack.getCount()),
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    0f));
        }
    }

    /** A recipe content holding a fluid. @see ContentOfItem */
    @NodeAttribute(name = "mbd2_content_of_fluid", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentOfFluid extends AnnotatedNode {
        @InputPort public FluidStack fluid = FluidStack.EMPTY;
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @OutputPort public Content content;

        @Override
        public void evaluate(EvalContext ctx) {
            var stack = ctx.getInput("fluid", FluidStack.class, FluidStack.EMPTY);
            ctx.setOutput("content", stack == null || stack.isEmpty() ? null : new Content(
                    SizedFluidIngredient.of(stack.getFluid(), stack.getAmount()),
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    0f));
        }
    }

    /**
     * The item contents of one side of a recipe, as the stacks they accept or produce.
     *
     * <p>Typed for the same reason the constructors are: turning a payload back into something a graph
     * can use means knowing what it is. A projection, not the contents themselves — a recipe input is
     * an {@code Ingredient}, which may be a tag matching many items, and this reports the first stack
     * of each. Right for "what does this make", wrong for a tag input.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_items", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Items extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public List<ItemStack> stacks;

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
        }
    }

    /** The fluid contents of one side of a recipe, as the stacks they accept or produce. @see Items */
    @NodeAttribute(name = "mbd2_recipe_fluids", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Fluids extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @OutputPort public List<FluidStack> stacks;

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
        }
    }

    // ---- shared -------------------------------------------------------------------------------

    /** The contents of the chosen side for one capability, or empty when there is no recipe. */
    private static List<Content> contentsOf(EvalContext ctx, RecipeCapability<?> capability) {
        var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
        if (recipe == null) return List.of();
        var contents = sideOf(recipe, ctx.getInput("io", IO.class, IO.OUT)).get(capability);
        return contents == null ? List.of() : contents;
    }

    /**
     * The map for one side. {@link IO#BOTH} has no single answer here — a content belongs to the
     * input list or the output list, never to both — so it is read as OUT rather than silently
     * editing two sides at once.
     */
    private static Map<RecipeCapability<?>, List<Content>> sideOf(MBDRecipe recipe, IO io) {
        return io == IO.IN ? recipe.inputs : recipe.outputs;
    }
}

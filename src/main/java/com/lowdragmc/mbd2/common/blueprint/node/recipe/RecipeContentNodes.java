package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.type.KGTypeHandles;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * <h2>One set of nodes for every capability</h2>
 * A recipe's contents are {@code Map<RecipeCapability<?>, List<Content>>}, so every operation here is
 * the same operation whatever the capability. Each node carries a {@code capability} dropdown fed by
 * {@link MBDRegistries#RECIPE_CAPABILITIES}, which means items, fluids, energy, Mekanism chemicals and
 * anything registered after this file was written are covered by one set of nodes rather than one set
 * each.
 *
 * <p>{@code Content Of} is the one that could have gone the other way. A {@link Content}'s payload is
 * an {@code Object} whose real class only its capability knows — {@code SizedIngredient} for items,
 * {@code SizedFluidIngredient} for fluids, {@code Integer} for energy — and a Content does not record
 * which capability it belongs to, so a typed constructor per capability looked unavoidable. It is not:
 * {@link RecipeCapability#of(Object)} is exactly the cast that was missing. Its port is
 * {@link TypeHandles#UNKNOWN}, so an {@code ItemStack}, a {@code FluidStack} or a number all wire in,
 * and the chosen capability decides how to read it — the same coercion a KubeJS recipe builder goes
 * through.</p>
 *
 * <p>The cost of that generality: a value the capability cannot interpret is not an error, it is that
 * capability's empty content — an item ingredient matching nothing, zero energy. Wiring a
 * {@code FluidStack} into a {@code Content Of} left on {@code item} produces a recipe that outputs
 * nothing rather than one that fails loudly. Check the capability dropdown first when a swap silently
 * does nothing.</p>
 *
 * <h2>Picking one content rather than all of them</h2>
 * {@code Clear Recipe Contents} drops a whole side; {@code Remove}, {@code Set} and {@code Content At}
 * take an index into that side's list, whose order is the order the recipe was authored in and which
 * {@link MBDRecipe#copy()} preserves. {@code Content Index Of Slot} turns the {@code slotName} a
 * recipe author wrote into that index, which is the stable way to name one particular content when
 * the recipe has several.
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
    /** The option every recipe-side node here carries. */
    private static final String CAPABILITY = "capability";

    private RecipeContentNodes() {}

    // ---- the capability dropdown ---------------------------------------------------------------

    /**
     * Base for the nodes that name a capability rather than being typed to one.
     *
     * <p>The option is declared imperatively rather than with {@code @Option} because
     * {@link #optionChoices} is documentation only — it feeds the node catalogue, not the editor.
     * Rendering an actual dropdown takes a configurator, which only the imperative hook accepts.</p>
     */
    private abstract static class CapabilityNode extends AnnotatedNode {

        @Override
        protected void onDefineExtraOptions(IOptionDefinitionContext context) {
            context.addOption(CAPABILITY, String.class)
                    .withDefaultValue(ItemRecipeCapability.CAP.name)
                    .withConfigurable(capabilityPicker())
                    .build();
        }

        @Override
        public List<String> optionChoices(String optionId) {
            return CAPABILITY.equals(optionId) ? capabilityNames() : List.of();
        }

        /** The chosen capability, or null when the option names one that is not registered. */
        @Nullable
        protected RecipeCapability<?> capability(EvalContext ctx) {
            return MBDRegistries.RECIPE_CAPABILITIES.get(
                    ctx.getOption(CAPABILITY, String.class, ItemRecipeCapability.CAP.name));
        }
    }

    private static List<String> capabilityNames() {
        return MBDRegistries.RECIPE_CAPABILITIES.keys().stream().sorted().toList();
    }

    /**
     * A search dropdown over the registered capability names.
     *
     * <p>Read through {@code Object} and {@code toString}: calling the generic
     * {@code IFieldValueConfigurable.getValue()} where a {@code String} is expected lets javac pick
     * the {@code char[]} overload of {@code String.valueOf} and fail at runtime instead of at compile
     * time. Same reason the KilaGraph pickers do it this way.</p>
     */
    private static ITypeConfigurable capabilityPicker() {
        var search = new SearchComponentConfigurator.ISearchConfigurator<String>() {
            @Override
            public String defaultValue() {
                return ItemRecipeCapability.CAP.name;
            }

            @Override
            public String resultText(@NotNull String value) {
                return value;
            }

            @Override
            public void search(String word, IResultHandler<String> handler) {
                var needle = word == null ? "" : word.toLowerCase(Locale.ROOT);
                for (var name : capabilityNames()) {
                    if (Thread.interrupted()) return;
                    if (needle.isEmpty() || name.toLowerCase(Locale.ROOT).contains(needle)) {
                        handler.accept(name);
                    }
                }
            }

            @Override
            public Component mapping(@NotNull String value) {
                return Component.literal(value);
            }
        };
        return (vc, typeHandle) -> IConfigurable.create(group ->
                group.addConfigurator(new SearchComponentConfigurator<>(
                        "",
                        () -> read(vc),
                        value -> vc.setValue(value == null ? ItemRecipeCapability.CAP.name : value),
                        search,
                        vc.forceUpdate())));
    }

    private static String read(IFieldValueConfigurable vc) {
        Object value = vc.getValue();
        return value == null ? ItemRecipeCapability.CAP.name : value.toString();
    }

    // ---- create ---------------------------------------------------------------------------------

    /**
     * A recipe content built from whatever the chosen capability can make of the value.
     *
     * <p>{@code value} is untyped on purpose — the capability decides. An {@code ItemStack} carries
     * its own count, a {@code FluidStack} its amount, a number is read as a number. {@code chance} is
     * the recipe's own chance field, so a bonus built here behaves like an authored chance output
     * rather than like an item pushed into a slot afterwards.</p>
     */
    @NodeAttribute(name = "mbd2_content_of", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentOf extends CapabilityNode {
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @InputPort public String slotName = "";
        @OutputPort public Content content;

        @Override
        protected void onDefineDynamicPorts(IPortDefinitionContext context) {
            context.addInputPort("value", TypeHandles.UNKNOWN);
        }

        @Override
        public void evaluate(EvalContext ctx) {
            var capability = capability(ctx);
            var value = ctx.getInputRaw("value");
            if (capability == null || value == null) {
                ctx.setOutput("content", null);
                return;
            }
            var payload = capability.of(value);
            ctx.setOutput("content", payload == null ? null : new Content(
                    payload,
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    0f,
                    ctx.getInput("slotName", String.class, ""),
                    ""));
        }
    }

    // ---- write ----------------------------------------------------------------------------------

    /**
     * A copy of the recipe with one more content on the chosen side.
     *
     * <p>Adding rather than replacing is the default because "also produce this" is as common as
     * "produce this instead" — compose it with {@code Clear Recipe Contents} for the latter.</p>
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
            var side = sideOf(copied, io(ctx));
            var contents = new ArrayList<>(side.getOrDefault(capability, List.of()));
            contents.add(content);
            side.put(capability, contents);
            ctx.setOutput("result", copied);
        }
    }

    /**
     * A copy of the recipe with the content at {@code index} replaced.
     *
     * <p>The targeted counterpart of clear-then-add: it keeps every other content, and their order,
     * which matters when a recipe's contents are bound to named slots. Out of range is a no-op.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_set_content", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetContent extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public int index = 0;
        @InputPort public Content content;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            var content = ctx.getInput("content", Content.class, null);
            var capability = capability(ctx);
            var index = ctx.getInt("index", 0);
            if (recipe == null || content == null || capability == null) {
                ctx.setOutput("result", recipe);
                return;
            }
            var copied = recipe.copy();
            var side = sideOf(copied, io(ctx));
            var contents = side.get(capability);
            if (contents == null || index < 0 || index >= contents.size()) {
                ctx.setOutput("result", recipe);
                return;
            }
            var edited = new ArrayList<>(contents);
            edited.set(index, content);
            side.put(capability, edited);
            ctx.setOutput("result", copied);
        }
    }

    /** A copy of the recipe with the content at {@code index} removed. Out of range is a no-op. */
    @NodeAttribute(name = "mbd2_recipe_remove_content", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class RemoveContent extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public int index = 0;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            var capability = capability(ctx);
            var index = ctx.getInt("index", 0);
            if (recipe == null || capability == null) {
                ctx.setOutput("result", recipe);
                return;
            }
            var copied = recipe.copy();
            var side = sideOf(copied, io(ctx));
            var contents = side.get(capability);
            if (contents == null || index < 0 || index >= contents.size()) {
                ctx.setOutput("result", recipe);
                return;
            }
            var edited = new ArrayList<>(contents);
            edited.remove(index);
            // An empty list is not the same as no entry: copyContents() drops empty lists, so one
            // left behind here would quietly disappear at the next copy. Drop it now instead.
            if (edited.isEmpty()) {
                side.remove(capability);
            } else {
                side.put(capability, edited);
            }
            ctx.setOutput("result", copied);
        }
    }

    /**
     * A copy of the recipe with every content of the chosen capability removed from one side.
     *
     * <p>The blunt instrument, and the other half of a wholesale replacement: clear, then add. On its
     * own it makes a side stop consuming or producing that capability entirely.</p>
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
            sideOf(copied, io(ctx)).remove(capability);
            ctx.setOutput("result", copied);
        }
    }

    // ---- read -----------------------------------------------------------------------------------

    /** One content of the chosen capability by position. {@code found} is false when out of range. */
    @NodeAttribute(name = "mbd2_recipe_content_at", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentAt extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public int index = 0;
        @OutputPort public Content content;
        @OutputPort public boolean found;

        @Override
        public void evaluate(EvalContext ctx) {
            var capability = capability(ctx);
            var index = ctx.getInt("index", 0);
            var contents = capability == null ? List.<Content>of() : contentsOf(ctx, capability);
            var hit = index >= 0 && index < contents.size();
            ctx.setOutput("content", hit ? contents.get(index) : null);
            ctx.setOutput("found", hit);
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

    /**
     * The index of the content whose {@code slotName} matches, or -1.
     *
     * <p>Position is a fragile way to name one content of several — insert one upstream and every
     * index after it shifts. A recipe author who wrote a slot name has already given that content a
     * stable identity, and this turns it back into the index the write nodes take.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_content_index", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentIndexOfSlot extends CapabilityNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public IO io = IO.OUT;
        @InputPort public String slotName = "";
        @OutputPort public int index;

        @Override
        public void evaluate(EvalContext ctx) {
            var capability = capability(ctx);
            var slotName = ctx.getInput("slotName", String.class, "");
            var contents = capability == null ? List.<Content>of() : contentsOf(ctx, capability);
            var found = -1;
            for (int i = 0; i < contents.size(); i++) {
                if (contents.get(i).slotName.equals(slotName)) {
                    found = i;
                    break;
                }
            }
            ctx.setOutput("index", found);
        }
    }

    // ---- the content itself ---------------------------------------------------------------------

    /**
     * The settings on a content, without knowing which capability it belongs to.
     *
     * <p>Its payload is deliberately not exposed: that is an {@code Object} only its capability can
     * read, and there is no capability here to ask. Use {@code Recipe Items} / {@code Recipe Fluids}
     * when the payload itself is what you want.</p>
     */
    @NodeAttribute(name = "mbd2_content_info", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentInfo extends AnnotatedNode {
        @InputPort public Content content;
        @OutputPort public float chance;
        @OutputPort public boolean perTick;
        @OutputPort public String slotName;
        @OutputPort public float tierChanceBoost;

        @Override
        public void evaluate(EvalContext ctx) {
            var content = ctx.getInput("content", Content.class, null);
            ctx.setOutput("chance", content == null ? 0f : content.chance);
            ctx.setOutput("perTick", content != null && content.perTick);
            ctx.setOutput("slotName", content == null ? "" : content.slotName);
            ctx.setOutput("tierChanceBoost", content == null ? 0f : content.tierChanceBoost);
        }
    }

    /**
     * What the content actually holds — the ingredient, the energy amount, the chemical stack.
     *
     * <p>The output port is typed from the chosen capability rather than left untyped, so it carries
     * a {@code SizedIngredient} on {@code item} and an {@code Integer} on {@code energy} and only
     * connects where that fits. Change the dropdown and the port retypes; a wire that no longer fits
     * is parked as a type conflict rather than silently dropped.</p>
     *
     * <p>Note the asymmetry with {@code Content Of}, which takes an untyped value: going in, the
     * capability coerces whatever it is given, and coming out there is exactly one right answer.</p>
     */
    @NodeAttribute(name = "mbd2_content_value", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentValue extends CapabilityNode {
        @InputPort public Content content;

        @Override
        protected void onDefineDynamicPorts(IPortDefinitionContext context) {
            context.addOutputPort("value", valueType());
        }

        /** The payload type of the chosen capability, or UNKNOWN when it cannot be resolved. */
        private TypeHandle valueType() {
            var capability = MBDRegistries.RECIPE_CAPABILITIES.get(
                    optionValue(CAPABILITY, String.class, ItemRecipeCapability.CAP.name));
            if (capability == null) return TypeHandles.UNKNOWN;
            var type = capability.contentType();
            return type == Object.class ? TypeHandles.UNKNOWN : KGTypeHandles.handleFor(type);
        }

        @Override
        public void evaluate(EvalContext ctx) {
            var content = ctx.getInput("content", Content.class, null);
            ctx.setOutput("value", content == null ? null : content.content);
        }
    }

    /**
     * The same content with different settings — the way to change a chance without rebuilding what
     * the content holds, and so without knowing its capability.
     *
     * <p>It sets all three of {@code chance}, {@code perTick} and {@code slotName}, so an unwired port
     * is a value and not "leave this one alone" — feed the ones you want kept from
     * {@code Content Info}. The tier chance boost and ui name are carried over, having no port here
     * to come from.</p>
     */
    @NodeAttribute(name = "mbd2_content_with", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ContentWith extends AnnotatedNode {
        @InputPort public Content content;
        @InputPort public float chance = 1f;
        @InputPort public boolean perTick = false;
        @InputPort public String slotName = "";
        @OutputPort public Content result;

        @Override
        public void evaluate(EvalContext ctx) {
            var content = ctx.getInput("content", Content.class, null);
            if (content == null) {
                ctx.setOutput("result", null);
                return;
            }
            ctx.setOutput("result", new Content(
                    content.content,
                    ctx.getInput("perTick", Boolean.class, false),
                    ctx.getInput("chance", Float.class, 1f),
                    content.tierChanceBoost,
                    ctx.getInput("slotName", String.class, ""),
                    content.uiName));
        }
    }

    // ---- typed projections -----------------------------------------------------------------------

    /**
     * The item contents of one side of a recipe, as the stacks they accept or produce.
     *
     * <p>Typed where the rest is not, because turning a payload back into something the graph can hold
     * means knowing what it is. A projection, not the contents themselves — a recipe input is an
     * {@code Ingredient}, which may be a tag matching many items, and this reports the first stack of
     * each. Right for "what does this make", wrong for a tag input.</p>
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

    private static IO io(EvalContext ctx) {
        return ctx.getInput("io", IO.class, IO.OUT);
    }

    /** The contents of the chosen side for one capability, or empty when there is no recipe. */
    private static List<Content> contentsOf(EvalContext ctx, RecipeCapability<?> capability) {
        var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
        if (recipe == null) return List.of();
        var contents = sideOf(recipe, io(ctx)).get(capability);
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

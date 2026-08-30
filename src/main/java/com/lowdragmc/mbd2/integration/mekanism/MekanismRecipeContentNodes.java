package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Reading and building the payload behind Mekanism's chemical recipe capability.
 *
 * <p>The recipe-content nodes are generic over capabilities, but only up to the payload: a
 * {@code Content Value} set to {@code mek_chemical} hands back a {@link ChemicalStackIngredient} and
 * nothing in MBD2 could open one. These are the pair every capability needs to supply — the one that
 * turns its payload into graph values, and the one that builds it back.</p>
 *
 * <h2>Absent Mekanism</h2>
 * This is the file where the guard genuinely matters. Every signature here mentions a Mekanism API
 * type, so loading the class without the jar is a {@code NoClassDefFoundError}. {@code modID =
 * "mekanism"} makes LDLib2's registry decide from the ASM scan data, <em>before</em> loading
 * anything, that these are not to be registered — the same mechanism {@link MekanismBlueprintNodes}
 * relies on, and the reason {@code gradlew runGameTestServer -PnoMekanism} exists: the claim is about
 * class resolution, and only a run without the jar tests it.
 */
public final class MekanismRecipeContentNodes {

    private static final String GROUP = "mbd2/recipe/mekanism";
    private static final String MOD = "mekanism";
    /** What Mekanism's own recipe builders default an unqualified chemical amount to. */
    private static final long DEFAULT_AMOUNT = 1000L;

    private MekanismRecipeContentNodes() {}

    /**
     * What a chemical payload accepts, and how much of it.
     *
     * <p>Same shape and same caveat as the item one: a chemical ingredient may be a tag matching
     * several chemicals, so {@code stacks} is the whole set and {@code first} a representative.</p>
     */
    @NodeAttribute(name = "mbd2_mek_chemical_ingredient_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class ChemicalIngredientInfo extends AnnotatedNode {
        @InputPort public ChemicalStackIngredient ingredient;
        @OutputPort public long amount;
        @OutputPort public List<ChemicalStack> stacks;
        @OutputPort public ChemicalStack first;

        @Override
        public void evaluate(EvalContext ctx) {
            var ingredient = ctx.getInput("ingredient", ChemicalStackIngredient.class, null);
            if (ingredient == null) {
                ctx.setOutput("amount", 0L);
                ctx.setOutput("stacks", List.of());
                ctx.setOutput("first", ChemicalStack.EMPTY);
                return;
            }
            var stacks = new ArrayList<ChemicalStack>();
            for (var holder : ingredient.ingredient().getChemicalHolders()) {
                stacks.add(new ChemicalStack(holder, ingredient.amount()));
            }
            ctx.setOutput("amount", ingredient.amount());
            ctx.setOutput("stacks", stacks);
            ctx.setOutput("first", stacks.isEmpty() ? ChemicalStack.EMPTY : stacks.getFirst());
        }
    }

    /**
     * A chemical payload for one chemical, by id.
     *
     * <p>An id rather than a {@code Chemical}, because nothing in the node set produces one out of
     * thin air — the chemical nodes all read a stack off a tank, which is no help when the point is
     * to name a chemical the machine does not have yet. An id that is not registered yields nothing
     * rather than throwing, which is the difference between a blueprint with a typo in it and a
     * server that stops.</p>
     */
    @NodeAttribute(name = "mbd2_mek_chemical_ingredient_of", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class ChemicalIngredientOf extends AnnotatedNode {
        @InputPort public ResourceLocation chemical;
        @InputPort public long amount = DEFAULT_AMOUNT;
        @OutputPort public ChemicalStackIngredient ingredient;

        @Override
        public void evaluate(EvalContext ctx) {
            var id = ctx.getInput("chemical", ResourceLocation.class, null);
            if (id == null || !MekanismAPI.CHEMICAL_REGISTRY.containsKey(id)) {
                ctx.setOutput("ingredient", null);
                return;
            }
            var chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
            if (chemical == null) {
                ctx.setOutput("ingredient", null);
                return;
            }
            ctx.setOutput("ingredient", new ChemicalStackIngredient(
                    IngredientCreatorAccess.chemical().of(chemical.getAsHolder()),
                    Math.max(1L, ctx.getLong("amount", DEFAULT_AMOUNT))));
        }
    }

    /** A chemical payload that accepts anything in a tag. @see ChemicalIngredientOf */
    @NodeAttribute(name = "mbd2_mek_chemical_ingredient_of_tag", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class ChemicalIngredientOfTag extends AnnotatedNode {
        @InputPort public ResourceLocation tag;
        @InputPort public long amount = DEFAULT_AMOUNT;
        @OutputPort public ChemicalStackIngredient ingredient;

        @Override
        public void evaluate(EvalContext ctx) {
            var tag = ctx.getInput("tag", ResourceLocation.class, null);
            if (tag == null) {
                ctx.setOutput("ingredient", null);
                return;
            }
            TagKey<Chemical> key = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, tag);
            ctx.setOutput("ingredient", new ChemicalStackIngredient(
                    IngredientCreatorAccess.chemical().tag(key),
                    Math.max(1L, ctx.getLong("amount", DEFAULT_AMOUNT))));
        }
    }
}

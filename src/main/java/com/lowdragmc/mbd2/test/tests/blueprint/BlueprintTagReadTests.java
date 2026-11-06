package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtCreateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtSetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtValueType;
import com.lowdragmc.kilagraph.graph.exec.GraphExecutor;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeContentNodes;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * KilaGraph's NBT setters write into the tag they are given, so a tag a node hands to a graph is the graph's to edit,
 * except a custom data read left uncopied, which stays the machine's own as it always was.
 */
@GameTestHolder(MBD2.MOD_ID)
public class BlueprintTagReadTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintTagReadFixtures.COUNTER_ID; }

    /** The default read is the machine's own tag, as it always was, so an edit of it is an edit of the machine. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void customDataIsReadAsTheMachinesOwnTagByDefault(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintTagReadFixtures.EDIT_CUSTOM_DATA_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var machine = scenario.machine();
        if (machine.getAnalogOutputSignal() != BlueprintTagReadFixtures.SIGNAL) {
            helper.fail("the graph did not see its own edit, signal " + machine.getAnalogOutputSignal());
            return;
        }
        if (machine.getCustomData().getInt(BlueprintTagReadFixtures.SCRATCH) != BlueprintTagReadFixtures.SIGNAL) {
            helper.fail("the default read was not the machine's own tag: " + machine.getCustomData());
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void editingACopiedCustomDataReadLeavesTheMachineAlone(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintTagReadFixtures.EDIT_CUSTOM_DATA_COPY_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var machine = scenario.machine();
        if (machine.getAnalogOutputSignal() != BlueprintTagReadFixtures.SIGNAL) {
            helper.fail("the graph did not see its own edit, signal " + machine.getAnalogOutputSignal());
            return;
        }
        if (machine.getCustomData().contains(BlueprintTagReadFixtures.SCRATCH)) {
            helper.fail("an edit nothing merged reached the machine's custom data: " + machine.getCustomData());
            return;
        }
        scenario.succeed();
    }

    /** With a copied read, one merge per tick is one increment, however many exec nodes read the chain behind it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aMergedCounterCountsOncePerTick(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintTagReadFixtures.COUNTER_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var data = scenario.machine().getCustomData();
        int counter = data.getInt(BlueprintTagReadFixtures.COUNTER);
        int reference = data.getInt(BlueprintTagReadFixtures.REFERENCE);
        if (reference == 0) {
            helper.fail("the graph never ran: " + data);
            return;
        }
        if (counter != reference) {
            helper.fail("the counter moved " + counter + " times in " + reference + " ticks: " + data);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void editingReadRecipeDataLeavesTheRecipeAlone(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintTagReadFixtures.EDIT_RECIPE_DATA_ID, new BlockPos(1, 1, 1))
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .runTicks(40)
                .assertItemCountAtLeast(1, Items.DIRT, 1);
        var machine = scenario.machine();
        if (machine.getAnalogOutputSignal() != BlueprintTagReadFixtures.SIGNAL) {
            helper.fail("the graph did not see its own edit, signal " + machine.getAnalogOutputSignal());
            return;
        }
        var data = recipeData(helper, BlueprintTagReadFixtures.RECIPE_ID);
        if (data == null) {
            helper.fail("the fixture recipe is not registered");
            return;
        }
        if (data.contains(BlueprintTagReadFixtures.SCRATCH) || data.getInt(BlueprintTagReadFixtures.ORIGINAL) != 1) {
            helper.fail("a blueprint's edit reached the registered recipe: " + data);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void entityIngredientTagsAreTheGraphsOwn(GameTestHelper helper) {
        var graph = new MachineBlueprintGraph();
        var fromTag = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientOfTag.class);
        KGGameTestHelpers.setInputConstant(fromTag, "tag", ResourceLocation.withDefaultNamespace("skeletons"));
        var resolve = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientInfo.class);
        KGGameTestHelpers.wire(graph, resolve.getInputsById().get("ingredient"), fromTag.getOutputsById().get("ingredient"));

        var source = KGGameTestHelpers.addRegisteredNode(graph, NbtSetNode.class);
        KGGameTestHelpers.setOption(source, "valueType", NbtValueType.INT);
        KGGameTestHelpers.setInputConstant(source, "key", BlueprintTagReadFixtures.ORIGINAL);
        KGGameTestHelpers.setInputConstant(source, "value", 1);
        KGGameTestHelpers.wire(graph, source.getInputsById().get("tag"),
                KGGameTestHelpers.addRegisteredNode(graph, NbtCreateNode.class).getOutputsById().get("out"));

        var maker = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientOf.class);
        KGGameTestHelpers.wire(graph, maker.getInputsById().get("type"), resolve.getOutputsById().get("first"));
        KGGameTestHelpers.wire(graph, maker.getInputsById().get("nbt"), source.getOutputsById().get("out"));
        var info = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientInfo.class);
        KGGameTestHelpers.wire(graph, info.getInputsById().get("ingredient"), maker.getOutputsById().get("ingredient"));

        var executor = new GraphExecutor(graph);
        var given = executor.evaluate(source.getOutputsById().get("out"), CompoundTag.class);
        var ingredient = executor.evaluate(maker.getOutputsById().get("ingredient"), EntityIngredient.class);
        var read = executor.evaluate(info.getOutputsById().get("nbt"), CompoundTag.class);
        if (ingredient == null || ingredient.getNbt() == null || read == null || given == null) {
            helper.fail("the graph built no ingredient with a tag: " + ingredient + ", read " + read);
            return;
        }
        if (!read.equals(ingredient.getNbt())) {
            helper.fail("Entity Ingredient Info read " + read + ", the ingredient holds " + ingredient.getNbt());
            return;
        }
        if (ingredient.getNbt() == given || read == ingredient.getNbt()) {
            helper.fail("the ingredient's tag is shared with the graph, so an NBT setter downstream would edit it");
            return;
        }
        helper.succeed();
    }

    private static CompoundTag recipeData(GameTestHelper helper, ResourceLocation id) {
        return helper.getLevel().getRecipeManager().byKey(id)
                .map(holder -> holder.value() instanceof MBDRecipe recipe ? recipe.data : null)
                .orElse(null);
    }
}

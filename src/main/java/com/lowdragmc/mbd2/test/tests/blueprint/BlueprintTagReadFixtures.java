package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.kilagraph.blueprint.nodes.math.AddNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtCreateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtGetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtSetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtValueType;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoNode;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Graphs that edit a tag a node read off the machine or a recipe. KilaGraph's NBT setters write into the tag they are
 * given, and since 21.1.0.16 a pure node runs again for every exec node that reads it.
 */
public class BlueprintTagReadFixtures implements TestFixtureProvider {
    /** Tick: signal = (custom data + {scratch: SIGNAL}).scratch, never merged, read as the machine's own tag. */
    public static final ResourceLocation EDIT_CUSTOM_DATA_ID = MBD2.id("blueprint_edit_read_custom_data");
    /** The same, read with {@code copy} set. */
    public static final ResourceLocation EDIT_CUSTOM_DATA_COPY_ID = MBD2.id("blueprint_edit_read_custom_data_copy");
    /** Tick: merge n + 1 into a copied read of custom data, then read the same pure chain again, then merge ref + 1. */
    public static final ResourceLocation COUNTER_ID = MBD2.id("blueprint_custom_data_counter");
    /** Recipe modify: signal = (recipe data + {scratch: SIGNAL}).scratch, the recipe left as it was. */
    public static final ResourceLocation EDIT_RECIPE_DATA_ID = MBD2.id("blueprint_edit_read_recipe_data");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("blueprint_tag_read_recipe_type");
    public static final ResourceLocation RECIPE_ID = MBD2.id("blueprint_tag_read_recipe");

    public static final String SCRATCH = "scratch";
    public static final String COUNTER = "n";
    public static final String REFERENCE = "ref";
    public static final String ORIGINAL = "original";
    public static final int SIGNAL = 7;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe(RECIPE_ID, b -> b.inputItems(Items.STONE).outputItems(Items.DIRT).duration(10).addData(ORIGINAL, 1))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(EDIT_CUSTOM_DATA_ID).withBlueprint(editCustomData(false)).register(event);
        TestMachineBuilder.simple(EDIT_CUSTOM_DATA_COPY_ID).withBlueprint(editCustomData(true)).register(event);
        TestMachineBuilder.simple(COUNTER_ID).withBlueprint(counter()).register(event);
        TestMachineBuilder.simple(EDIT_RECIPE_DATA_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlueprint(editRecipeData())
                .register(event);
    }

    private static MachineBlueprintGraph editCustomData(boolean copy) {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var signal = signalOfScratch(graph, customData(graph, copy));
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), tick.getOutputsById().get("next"));
        return graph;
    }

    private static MachineBlueprintGraph counter() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var data = customData(graph, true);

        var bumped = increment(graph, data, data, COUNTER);
        var mergeCounter = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.MergeCustomData.class);
        KGGameTestHelpers.wire(graph, mergeCounter.getInputsById().get("data"), bumped);

        var readAgain = get(graph, bumped, COUNTER);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("signal"), readAgain);

        var fresh = KGGameTestHelpers.addRegisteredNode(graph, NbtCreateNode.class).getOutputsById().get("out");
        var mergeReference = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.MergeCustomData.class);
        KGGameTestHelpers.wire(graph, mergeReference.getInputsById().get("data"), increment(graph, data, fresh, REFERENCE));

        KGGameTestHelpers.wire(graph, mergeCounter.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), mergeCounter.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, mergeReference.getInputsById().get("in"), signal.getOutputsById().get("next"));
        return graph;
    }

    private static MachineBlueprintGraph editRecipeData() {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var info = KGGameTestHelpers.addRegisteredNode(graph, RecipeInfoNode.class);
        KGGameTestHelpers.wire(graph, info.getInputsById().get("target"), modify.getOutputsById().get("recipe"));
        var data = KGGameTestHelpers.addBlock(graph, info, RecipeInfoBlocks.Data.class);
        var signal = signalOfScratch(graph, data.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    private static PortModel customData(MachineBlueprintGraph graph, boolean copy) {
        var info = KGGameTestHelpers.addRegisteredNode(graph, MachineInfoNode.class);
        var data = KGGameTestHelpers.addBlock(graph, info, MachineInfoBlocks.CustomData.class);
        KGGameTestHelpers.setInputConstant(data, "copy", copy);
        return data.getOutputsById().get("value");
    }

    /** Set Analog Signal(Nbt Get(Nbt Set(tag, scratch, SIGNAL), scratch)), so the edit is used but never saved. */
    private static NodeModel signalOfScratch(MachineBlueprintGraph graph, PortModel tag) {
        var edit = set(graph, tag, SCRATCH);
        KGGameTestHelpers.setInputConstant(edit, "value", SIGNAL);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("signal"), get(graph, edit.getOutputsById().get("out"), SCRATCH));
        return signal;
    }

    /** {@code into} with {@code key} set to {@code from.key + 1}. */
    private static PortModel increment(MachineBlueprintGraph graph, PortModel from, PortModel into, String key) {
        var plusOne = KGGameTestHelpers.addRegisteredNode(graph, AddNode.class);
        KGGameTestHelpers.wire(graph, plusOne.getInputsById().get("in1"), get(graph, from, key));
        KGGameTestHelpers.setInputConstant(plusOne, "in2", 1f);
        var put = set(graph, into, key);
        KGGameTestHelpers.wire(graph, put.getInputsById().get("value"), plusOne.getOutputsById().get("out"));
        return put.getOutputsById().get("out");
    }

    private static NodeModel set(MachineBlueprintGraph graph, PortModel tag, String key) {
        var put = KGGameTestHelpers.addRegisteredNode(graph, NbtSetNode.class);
        KGGameTestHelpers.setOption(put, "valueType", NbtValueType.INT);
        KGGameTestHelpers.setInputConstant(put, "key", key);
        KGGameTestHelpers.wire(graph, put.getInputsById().get("tag"), tag);
        return put;
    }

    private static PortModel get(MachineBlueprintGraph graph, PortModel tag, String key) {
        var get = KGGameTestHelpers.addRegisteredNode(graph, NbtGetNode.class);
        KGGameTestHelpers.setOption(get, "valueType", NbtValueType.INT);
        KGGameTestHelpers.setInputConstant(get, "key", key);
        KGGameTestHelpers.wire(graph, get.getInputsById().get("tag"), tag);
        return get.getOutputsById().get("out");
    }
}

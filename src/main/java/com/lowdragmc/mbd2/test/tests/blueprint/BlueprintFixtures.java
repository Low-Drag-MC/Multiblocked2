package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.CancelEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.trait.ItemHandlerNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fixtures for the blueprint end-to-end tests.
 *
 * <p>All four machines run the same 20-tick stone→dirt recipe, so "did the blueprint run?" is
 * observable as "was dirt produced?" — an assertion made of machine behaviour rather than of graph
 * internals, which is the only kind that proves the whole chain (binding → resource/inline tag →
 * executor → entry-node index → exec flow → event write-back) actually connects.</p>
 */
public class BlueprintFixtures implements TestFixtureProvider {

    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("blueprint_recipe_type");
    public static final ResourceLocation STONE_TO_DIRT_RECIPE_ID = MBD2.id("blueprint_stone_to_dirt");

    /** No blueprint — the control. Produces dirt. */
    public static final ResourceLocation PLAIN_MACHINE_ID = MBD2.id("blueprint_plain_machine");
    /** A blueprint that cancels every tick. Produces nothing. */
    public static final ResourceLocation CANCELLING_MACHINE_ID = MBD2.id("blueprint_cancelling_machine");
    /** The parameterised blueprint with {@code shouldCancel} left at its default of false. */
    public static final ResourceLocation PARAM_OFF_MACHINE_ID = MBD2.id("blueprint_param_off_machine");
    /** The same blueprint with {@code shouldCancel} overridden to true. */
    public static final ResourceLocation PARAM_ON_MACHINE_ID = MBD2.id("blueprint_param_on_machine");

    /** A blueprint that shuttles items from the input slot to the output slot every tick. */
    public static final ResourceLocation TRANSFER_MACHINE_ID = MBD2.id("blueprint_transfer_machine");

    /** The exposed parameter's name, shared by the fixture and the override. */
    public static final String SHOULD_CANCEL = "shouldCancel";

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe(STONE_TO_DIRT_RECIPE_ID, b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        machine(PLAIN_MACHINE_ID).register(event);

        machine(CANCELLING_MACHINE_ID)
                .withBlueprint(alwaysCancelTick())
                .register(event);

        // Same graph, two bindings: the whole point of an exposed parameter is that one blueprint
        // serves both machines and only the binding differs.
        var parameterised = cancelTickIf();
        machine(PARAM_OFF_MACHINE_ID)
                .withBlueprint(parameterised)
                .register(event);
        machine(PARAM_ON_MACHINE_ID)
                .withBlueprint(inlineWithShouldCancel(parameterised, true))
                .register(event);

        // No recipe type: this one's only behaviour is its blueprint, so a moved item cannot be
        // explained by the recipe logic having done it.
        TestMachineBuilder.simple(TRANSFER_MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withBlueprint(transferItems())
                .register(event);
    }

    private static TestMachineBuilder machine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID);
    }

    /** {@code Machine Tick → Cancel Event}. */
    private static MachineBlueprintGraph alwaysCancelTick() {
        var graph = new MachineBlueprintGraph();
        var entry = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var cancel = KGGameTestHelpers.addRegisteredNode(graph, CancelEventNode.class);
        KGGameTestHelpers.wire(graph, cancel.getInputsById().get("in"), entry.getOutputsById().get("next"));
        return graph;
    }

    /** {@code Machine Tick → Branch(shouldCancel) → [true] Cancel Event}. */
    private static MachineBlueprintGraph cancelTickIf() {
        var graph = new MachineBlueprintGraph();
        // INPUT kind is what makes it an exposed parameter — MachineBlueprintBinding generates one
        // configurator row per INPUT variable and seeds the executor's store from them.
        var shouldCancel = KGGameTestHelpers.dataVar(graph.graphModel, SHOULD_CANCEL, boolean.class,
                false, VariableKind.INPUT);
        var read = KGGameTestHelpers.varNode(graph.graphModel, shouldCancel);

        var entry = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var branch = KGGameTestHelpers.addRegisteredNode(graph, BranchNode.class);
        var cancel = KGGameTestHelpers.addRegisteredNode(graph, CancelEventNode.class);

        KGGameTestHelpers.wire(graph, branch.getInputsById().get("cond"), read.getOutputPort());
        KGGameTestHelpers.wire(graph, branch.getInputsById().get("in"), entry.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, cancel.getInputsById().get("in"), branch.getOutputsById().get("trueExec"));
        return graph;
    }

    /**
     * {@code Machine Tick → Extract Slot(input, 64) → Insert(output, extracted)}.
     *
     * <p>Exercises the part of the chain the cancel tests do not: a machine node reading a trait through
     * the capability bridge, two exec nodes in sequence, and a data wire carrying a value one exec node
     * staged into the next one's input.</p>
     */
    private static MachineBlueprintGraph transferItems() {
        var graph = new MachineBlueprintGraph();
        var entry = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var inHandler = KGGameTestHelpers.addRegisteredNode(graph, TraitCapabilityNodes.ItemHandlerOf.class);
        var outHandler = KGGameTestHelpers.addRegisteredNode(graph, TraitCapabilityNodes.ItemHandlerOf.class);
        var extract = KGGameTestHelpers.addRegisteredNode(graph, ItemHandlerNodes.ExtractSlot.class);
        var insert = KGGameTestHelpers.addRegisteredNode(graph, ItemHandlerNodes.Insert.class);

        // TestMachineBuilder names the first item-slot trait after its type and suffixes the rest.
        KGGameTestHelpers.setInputConstant(inHandler, "traitName", "item_slot");
        KGGameTestHelpers.setInputConstant(outHandler, "traitName", "item_slot_1");
        KGGameTestHelpers.setInputConstant(extract, "slot", 0);
        KGGameTestHelpers.setInputConstant(extract, "amount", 64);

        KGGameTestHelpers.wire(graph, extract.getInputsById().get("handler"), inHandler.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("handler"), outHandler.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("stack"), extract.getOutputsById().get("extracted"));

        KGGameTestHelpers.wire(graph, extract.getInputsById().get("in"), entry.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("in"), extract.getOutputsById().get("next"));
        return graph;
    }

    private static MachineBlueprintBinding inlineWithShouldCancel(MachineBlueprintGraph graph, boolean value) {
        var constant = new TypeConstant();
        constant.init(com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.BOOL);
        constant.setValue(value);
        return MachineBlueprintBinding
                .ofInline(graph.graphModel.serializeNBT(Platform.getFrozenRegistry()))
                .withVariable(SHOULD_CANCEL, constant);
    }

    public static ItemStack stone(int count) {
        return new ItemStack(Items.STONE, count);
    }

    public static ItemStack dirt(int count) {
        return new ItemStack(Items.DIRT, count);
    }

    /** Referenced so the fixture class is initialised before the gametest server starts. */
    public static MBDRecipeType recipeType() {
        return com.lowdragmc.mbd2.api.registry.MBDRegistries.RECIPE_TYPES.get(RECIPE_TYPE_ID);
    }
}

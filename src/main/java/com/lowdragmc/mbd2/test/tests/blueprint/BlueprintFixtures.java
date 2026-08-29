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
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.trait.ItemHandlerNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.integration.photon.PhotonFXNodes;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.joml.Vector2f;

import java.io.DataInputStream;

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

    /** A blueprint that inserts one item every {@link #PERIOD} ticks, driven by {@code Every N Ticks}. */
    public static final ResourceLocation PERIODIC_MACHINE_ID = MBD2.id("blueprint_periodic_machine");
    /** How often {@link #PERIODIC_MACHINE_ID} fires. Small, so a short test still sees several. */
    public static final int PERIOD = 4;

    /**
     * A machine bound to a blueprint recorded <em>with</em> Photon installed, so it can be loaded
     * without. @see BlueprintPhotonNodeTests
     */
    public static final ResourceLocation PHOTON_BLUEPRINT_MACHINE_ID = MBD2.id("blueprint_photon_machine");
    /** Nodes in {@link #photonBlueprintGraph}, so the load test can assert none went missing. */
    public static final int PHOTON_BLUEPRINT_NODE_COUNT = 4;
    private static final String PHOTON_BLUEPRINT_GOLDEN = "/data/mbd2/test/photon_blueprint_golden.nbt";

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

        TestMachineBuilder.simple(PERIODIC_MACHINE_ID)
                .withItemSlots(1, IO.OUT)
                .withBlueprint(insertEveryNTicks())
                .register(event);

        // Bound to the recorded bytes, not to a freshly built graph: without Photon the node cannot be
        // spawned at all, so a graph built here would differ between the two runs and the test would be
        // comparing two different things rather than one file read two ways.
        var photonBlueprint = photonBlueprintTag();
        if (photonBlueprint != null) {
            TestMachineBuilder.simple(PHOTON_BLUEPRINT_MACHINE_ID)
                    .withItemSlots(1, IO.OUT)
                    .withBlueprint(MachineBlueprintBinding.ofInline(photonBlueprint))
                    .register(event);
        }
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

    /**
     * {@code Machine Tick → Branch(Machine Info.Every N Ticks) → [true] Insert(1 dirt)}.
     *
     * <p>The periodic idiom. The test that goes with it asserts the count is neither zero nor every
     * tick, because those are the two shapes a broken gate takes: the remainder stops moving and the
     * branch then takes the same side forever, one way or the other.</p>
     */
    private static MachineBlueprintGraph insertEveryNTicks() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);

        var info = KGGameTestHelpers.addRegisteredNode(graph, MachineInfoNode.class);
        var every = KGGameTestHelpers.addBlock(graph, info, MachineInfoBlocks.EveryNTicks.class);
        KGGameTestHelpers.setInputConstant(every, "interval", PERIOD);

        var branch = KGGameTestHelpers.addRegisteredNode(graph, BranchNode.class);
        var handler = KGGameTestHelpers.addRegisteredNode(graph, TraitCapabilityNodes.ItemHandlerOf.class);
        KGGameTestHelpers.setInputConstant(handler, "traitName", "item_slot");
        var insert = KGGameTestHelpers.addRegisteredNode(graph, ItemHandlerNodes.Insert.class);
        KGGameTestHelpers.setInputConstant(insert, "stack", new ItemStack(Items.DIRT, 1));

        KGGameTestHelpers.wire(graph, branch.getInputsById().get("cond"), every.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, branch.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("handler"), handler.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("in"), branch.getOutputsById().get("trueExec"));
        return graph;
    }

    /**
     * {@code Machine Tick → Insert(1 dirt) → Play Machine FX}.
     *
     * <p>The insert comes <em>first</em> on purpose. Whether exec flow continues past a node that
     * could not be resolved is LDLib2's call and could reasonably go either way, so putting the
     * observable step before the Photon one makes the assertion mean the same thing whether or not
     * Photon is installed: the blueprint ran.</p>
     *
     * <p>Only buildable with Photon — {@code GraphNodeRegistry} filters {@code modID="photon"} nodes
     * out of the registry without it. That is exactly why the graph is recorded to a file rather than
     * built at fixture time; see {@link #photonBlueprintTag()}.</p>
     */
    static MachineBlueprintGraph photonBlueprintGraph() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var handler = KGGameTestHelpers.addRegisteredNode(graph, TraitCapabilityNodes.ItemHandlerOf.class);
        KGGameTestHelpers.setInputConstant(handler, "traitName", "item_slot");
        var insert = KGGameTestHelpers.addRegisteredNode(graph, ItemHandlerNodes.Insert.class);
        KGGameTestHelpers.setInputConstant(insert, "stack", new ItemStack(Items.DIRT, 1));
        var playFX = KGGameTestHelpers.addRegisteredNode(graph, PhotonFXNodes.PlayFX.class);
        KGGameTestHelpers.setInputConstant(playFX, "name", "burst");

        // Laid out, unlike the other fixtures here, because this graph is also opened on a canvas by
        // PhotonOptionalBlueprintScenario — four nodes all at the origin render as one unreadable pile
        // and the screenshot stops being evidence of anything.
        tick.setPosition(new Vector2f(0, 40));
        handler.setPosition(new Vector2f(0, 190));
        insert.setPosition(new Vector2f(230, 40));
        playFX.setPosition(new Vector2f(470, 40));

        KGGameTestHelpers.wire(graph, insert.getInputsById().get("handler"), handler.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, insert.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, playFX.getInputsById().get("in"), insert.getOutputsById().get("next"));
        return graph;
    }

    /**
     * The recorded form of {@link #photonBlueprintGraph()}, or null if the file is missing.
     *
     * <p>{@code data/mbd2/test/photon_blueprint_golden.nbt} was written by a build that had Photon,
     * which is the only way to get a graph naming a Photon node into a run that does not have one.</p>
     */
    public static CompoundTag photonBlueprintTag() {
        try (var stream = BlueprintFixtures.class.getResourceAsStream(PHOTON_BLUEPRINT_GOLDEN)) {
            if (stream == null) return null;
            return NbtIo.read(new DataInputStream(stream), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return null;
        }
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

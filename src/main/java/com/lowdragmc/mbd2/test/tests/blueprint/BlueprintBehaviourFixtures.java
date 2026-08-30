package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.StructureFormedEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeBuildNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeContentNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.EnergyNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import com.lowdragmc.mbd2.common.capability.recipe.EntityRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Machines whose only interesting behaviour is a blueprint, one per node group under test.
 *
 * <p>Each is built so that a broken node produces a <em>different</em> observable value rather than a
 * missing one. Asserting "the signal is not zero" would pass for a node that reads nothing and a node
 * that reads correctly alike; asserting "the signal is six, and six is a value nothing else in the
 * machine produces" does not.</p>
 */
public class BlueprintBehaviourFixtures implements TestFixtureProvider {

    /** Writes its tier, reads it back through Machine Info, and emits it as a comparator signal. */
    public static final ResourceLocation TIER_MACHINE_ID = MBD2.id("blueprint_tier_machine");
    /** Writes and then merges into its custom data. */
    public static final ResourceLocation CUSTOM_DATA_MACHINE_ID = MBD2.id("blueprint_custom_data_machine");
    /** Fills its own energy buffer through the trait capability bridge. */
    public static final ResourceLocation ENERGY_MACHINE_ID = MBD2.id("blueprint_energy_machine");
    /** Rewrites every recipe to take far longer than the test runs for. */
    public static final ResourceLocation SLOW_RECIPE_MACHINE_ID = MBD2.id("blueprint_slow_recipe_machine");
    /** Keeps its own recipe logic switched off. */
    public static final ResourceLocation DISABLED_MACHINE_ID = MBD2.id("blueprint_disabled_machine");
    /** A multiblock controller that emits a signal once its structure forms. */
    public static final ResourceLocation MULTIBLOCK_MACHINE_ID = MBD2.id("blueprint_multiblock_machine");
    /** Runs a client-only action on a server, which must be skipped rather than fatal. */
    public static final ResourceLocation CLIENT_ONLY_MACHINE_ID = MBD2.id("blueprint_client_only_machine");
    /** Hooks Machine Tick twice, with two independent effects. */
    public static final ResourceLocation DOUBLE_ENTRY_MACHINE_ID = MBD2.id("blueprint_double_entry_machine");

    /** A recipe type whose one recipe has two outputs, the second bound to a named slot. */
    public static final ResourceLocation TWO_OUTPUT_RECIPE_TYPE_ID = MBD2.id("blueprint_two_output_recipe_type");
    public static final ResourceLocation TWO_OUTPUT_RECIPE_ID = MBD2.id("blueprint_stone_to_dirt_and_diamond");
    /** The slot the second output is bound to, and the name the index lookup searches for. */
    public static final String BONUS_SLOT = "bonus";
    /** No blueprint: the control that makes the three below mean something. */
    public static final ResourceLocation TWO_OUTPUT_CONTROL_ID = MBD2.id("blueprint_two_output_control");
    /** Removes the output at index 1 and nothing else. */
    public static final ResourceLocation REMOVE_ONE_OUTPUT_ID = MBD2.id("blueprint_remove_one_output");
    /** Finds the bonus output by slot name and removes that one. */
    public static final ResourceLocation REMOVE_BY_SLOT_ID = MBD2.id("blueprint_remove_by_slot");
    /** Rewrites the chance on the output at index 1 to zero. */
    public static final ResourceLocation ZERO_CHANCE_OUTPUT_ID = MBD2.id("blueprint_zero_chance_output");
    /** Reads the payload out of the first output and adds a second output built from it. */
    public static final ResourceLocation ECHO_FIRST_OUTPUT_ID = MBD2.id("blueprint_echo_first_output");
    /** The same, but the payload goes out through NBT and comes back. */
    public static final ResourceLocation NBT_ROUND_TRIP_ID = MBD2.id("blueprint_content_nbt_round_trip");
    /** The same, but the payload is unpacked into a stack by Ingredient Info first. */
    public static final ResourceLocation UNPACK_INGREDIENT_ID = MBD2.id("blueprint_unpack_ingredient");

    /** Builds an item payload from a tag and reads its count back out. */
    public static final ResourceLocation ITEM_PAYLOAD_ID = MBD2.id("blueprint_item_payload_round_trip");
    /** Builds a fluid payload from a tag and reads its amount back out. */
    public static final ResourceLocation FLUID_PAYLOAD_ID = MBD2.id("blueprint_fluid_payload_round_trip");
    /** Builds an entity payload from a tag, rebuilds it by type, and reads its count back out. */
    public static final ResourceLocation ENTITY_PAYLOAD_ID = MBD2.id("blueprint_entity_payload_round_trip");

    /** The counts the payload fixtures ask for. Each is distinct and none is a default. */
    public static final int ITEM_PAYLOAD_COUNT = 3;
    public static final int FLUID_PAYLOAD_AMOUNT = 6;
    public static final int ENTITY_PAYLOAD_COUNT = 5;
    /** What the entity fixture's tag step asks for, so that the rebuild step's count is the one seen. */
    public static final int ENTITY_TAG_COUNT = 2;

    /** The tier the blueprint writes and reads back. Distinct from the default of zero. */
    public static final int TIER = 6;
    /** The signal the multiblock blueprint emits on forming. */
    public static final int FORMED_SIGNAL = 13;
    /** The marker the disable blueprint emits after switching the machine off. */
    public static final int DISABLED_MARKER = 5;
    /** The marker emitted after a client-only action was skipped on the server. */
    public static final int PAST_CLIENT_ONLY_MARKER = 9;
    /** The marker the second of two Machine Tick entries emits. */
    public static final int SECOND_ENTRY_MARKER = 11;
    public static final String COUNTER_KEY = "blueprintCounter";
    public static final String FLAG_KEY = "blueprintFlag";

    /**
     * Stone into dirt <em>and</em> a diamond, the diamond bound to the {@link #BONUS_SLOT} slot.
     *
     * <p>Two outputs of the same capability is what makes "clear the whole side" and "remove one of
     * them" distinguishable at all, and two <em>different</em> items make it visible which one went.
     * The diamond is second so that an index lookup returning a hardcoded zero removes the dirt
     * instead - a wrong answer and a right one produce different worlds, not the same one.</p>
     */
    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(TWO_OUTPUT_RECIPE_TYPE_ID)
                .recipe(TWO_OUTPUT_RECIPE_ID, b -> {
                    b.inputItems(Items.STONE).outputItems(Items.DIRT);
                    // Builder state, so it applies from here on: only the diamond is named.
                    b.slotName(BONUS_SLOT).outputItems(Items.DIAMOND).duration(20);
                })
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        twoOutputMachine(TWO_OUTPUT_CONTROL_ID).register(event);
        twoOutputMachine(REMOVE_ONE_OUTPUT_ID).withBlueprint(removeOutputAt(1)).register(event);
        twoOutputMachine(REMOVE_BY_SLOT_ID).withBlueprint(removeOutputNamed(BONUS_SLOT)).register(event);
        twoOutputMachine(ZERO_CHANCE_OUTPUT_ID).withBlueprint(zeroChanceOutputAt(1)).register(event);
        twoOutputMachine(ECHO_FIRST_OUTPUT_ID).withBlueprint(echoOutputAt(0)).register(event);
        twoOutputMachine(NBT_ROUND_TRIP_ID).withBlueprint(echoOutputThroughNbt(0)).register(event);
        twoOutputMachine(UNPACK_INGREDIENT_ID).withBlueprint(echoOutputThroughStack(0)).register(event);

        TestMachineBuilder.simple(ITEM_PAYLOAD_ID).withBlueprint(itemPayloadRoundTrip()).register(event);
        TestMachineBuilder.simple(FLUID_PAYLOAD_ID).withBlueprint(fluidPayloadRoundTrip()).register(event);
        TestMachineBuilder.simple(ENTITY_PAYLOAD_ID).withBlueprint(entityPayloadRoundTrip()).register(event);

        TestMachineBuilder.simple(TIER_MACHINE_ID)
                .withBlueprint(tierRoundTrip())
                .register(event);

        TestMachineBuilder.simple(CUSTOM_DATA_MACHINE_ID)
                .withBlueprint(customDataWrites())
                .register(event);

        TestMachineBuilder.simple(ENERGY_MACHINE_ID)
                .withEnergy(10_000)
                .withBlueprint(fillOwnEnergy())
                .register(event);

        TestMachineBuilder.simple(SLOW_RECIPE_MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(BlueprintFixtures.RECIPE_TYPE_ID)
                .withBlueprint(slowEveryRecipe())
                .register(event);

        TestMachineBuilder.simple(DISABLED_MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(BlueprintFixtures.RECIPE_TYPE_ID)
                .withBlueprint(keepDisabled())
                .register(event);

        TestMachineBuilder.simple(CLIENT_ONLY_MACHINE_ID)
                .withBlueprint(clientOnlyThenMarker())
                .register(event);

        TestMachineBuilder.simple(DOUBLE_ENTRY_MACHINE_ID)
                .withBlueprint(twoTickEntries())
                .register(event);

        TestMachineBuilder.multiblock(MULTIBLOCK_MACHINE_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("SSS")
                        .aisle("SCS")
                        .aisle("SSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .withBlueprint(signalOnFormed())
                .register(event);
    }

    /**
     * {@code Machine Tick → Set Machine Tier(6) → Set Analog Signal(Machine Info.Tier)}.
     *
     * <p>The read goes through the Machine Info context with its target unwired, so this is also the
     * test that the context's fallback to the blueprint's own machine works. A broken read stages
     * nothing and the signal ends up zero, which is distinguishable from six.</p>
     *
     * <p>Driven from Machine Tick rather than On Load: On Load reaches the machine through a server
     * {@code TickTask}, whose timing relative to the gametest harness is not deterministic, and a test
     * that sometimes passes is worse than one that covers a different entry node.</p>
     */
    private static MachineBlueprintGraph tierRoundTrip() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var setTier = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.SetTier.class);
        KGGameTestHelpers.setInputConstant(setTier, "tier", TIER);

        var info = KGGameTestHelpers.addRegisteredNode(graph, MachineInfoNode.class);
        var tier = KGGameTestHelpers.addBlock(graph, info, MachineInfoBlocks.MachineTier.class);
        var setSignal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.wire(graph, setSignal.getInputsById().get("signal"), tier.getOutputsById().get("value"));

        KGGameTestHelpers.wire(graph, setTier.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, setSignal.getInputsById().get("in"), setTier.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Machine Tick → Set Custom Data{counter:1} → Merge Custom Data{flag:true}}.
     *
     * <p>Both in one flow, so the assertion covers the thing that actually distinguishes them: after
     * the merge the counter written by the set must still be there.</p>
     */
    private static MachineBlueprintGraph customDataWrites() {
        var graph = new MachineBlueprintGraph();
        var load = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);

        var initial = new CompoundTag();
        initial.putInt(COUNTER_KEY, 1);
        var set = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.SetCustomData.class);
        KGGameTestHelpers.setInputConstant(set, "data", initial);

        var extra = new CompoundTag();
        extra.putBoolean(FLAG_KEY, true);
        var merge = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.MergeCustomData.class);
        KGGameTestHelpers.setInputConstant(merge, "data", extra);

        KGGameTestHelpers.wire(graph, set.getInputsById().get("in"), load.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, merge.getInputsById().get("in"), set.getOutputsById().get("next"));
        return graph;
    }

    /** {@code Machine Tick → Trait Energy Storage → Receive Energy(5000)}. */
    private static MachineBlueprintGraph fillOwnEnergy() {
        var graph = new MachineBlueprintGraph();
        var load = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var storage = KGGameTestHelpers.addRegisteredNode(graph, TraitCapabilityNodes.EnergyStorageOf.class);
        // A trait's default name is its registered type name; TestMachineBuilder does not rename the
        // first trait of a kind.
        KGGameTestHelpers.setInputConstant(storage, "traitName", "forge_energy_storage");
        var receive = KGGameTestHelpers.addRegisteredNode(graph, EnergyNodes.Receive.class);
        KGGameTestHelpers.setInputConstant(receive, "amount", 5_000);
        KGGameTestHelpers.wire(graph, receive.getInputsById().get("storage"), storage.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, receive.getInputsById().get("in"), load.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Recipe Modify (Before) → Copy Recipe → Set Recipe Duration(2000) → Set Event Recipe}.
     *
     * <p>Lengthening rather than shortening on purpose: "no output after forty ticks" is a stable
     * assertion, where "output within five ticks" would race the async recipe search.</p>
     */
    private static MachineBlueprintGraph slowEveryRecipe() {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var copy = KGGameTestHelpers.addRegisteredNode(graph, RecipeBuildNodes.Copy.class);
        var duration = KGGameTestHelpers.addRegisteredNode(graph, RecipeBuildNodes.SetDuration.class);
        KGGameTestHelpers.setInputConstant(duration, "duration", 2_000);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, copy.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, duration.getInputsById().get("recipe"), copy.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), duration.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Machine Tick → Set Working Enabled(false) → Set Analog Signal(5)}.
     *
     * <p>Every tick rather than once, because suspending is not a latch: {@code setWorkingEnabled}
     * sets the status, and the recipe logic is free to leave that status on a later tick. A machine
     * that should stay off has to keep saying so, which is what a redstone-gated machine does anyway.
     *
     * <p>The trailing signal is a marker, so the test can tell "the flow never ran" from "the flow ran
     * and the suspend did not hold". Without it a failure is ambiguous between a dispatch problem and a
     * recipe-logic one, which are fixed in completely different places.</p>
     */
    private static MachineBlueprintGraph keepDisabled() {
        var graph = new MachineBlueprintGraph();
        var load = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var disable = KGGameTestHelpers.addRegisteredNode(
                graph, RecipeLogicActionNodes.SetWorkingEnabled.class);
        KGGameTestHelpers.setInputConstant(disable, "enabled", false);
        var marker = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(marker, "signal", DISABLED_MARKER);

        KGGameTestHelpers.wire(graph, disable.getInputsById().get("in"), load.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, marker.getInputsById().get("in"), disable.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Machine Tick → Play State Sound → Set Analog Signal(9)}.
     *
     * <p>{@code Play State Sound} is {@code Side.CLIENT}: the method it calls is
     * {@code @OnlyIn(Dist.CLIENT)} and its body touches a client-only sound class, so reaching it on a
     * server would be fatal. The marker after it is what proves the two things that matter — the server
     * did not die, and skipping the action did not dead-end the flow.</p>
     */
    private static MachineBlueprintGraph clientOnlyThenMarker() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var sound = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.PlayStateSound.class);
        KGGameTestHelpers.setInputConstant(sound, "state", "base");
        var marker = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(marker, "signal", PAST_CLIENT_ONLY_MARKER);

        KGGameTestHelpers.wire(graph, sound.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, marker.getInputsById().get("in"), sound.getOutputsById().get("next"));
        return graph;
    }

    /**
     * Two separate {@code Machine Tick} entries, each with its own effect.
     *
     * <p>Deliberately order-independent — one writes custom data, the other a redstone signal — because
     * entries for one event run in node-creation order, which is not something a graph shows. A test
     * that depended on which went first would be asserting an implementation detail.</p>
     */
    private static MachineBlueprintGraph twoTickEntries() {
        var graph = new MachineBlueprintGraph();

        var first = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var data = new CompoundTag();
        data.putInt(COUNTER_KEY, 1);
        var write = KGGameTestHelpers.addRegisteredNode(graph, MachineActionNodes.SetCustomData.class);
        KGGameTestHelpers.setInputConstant(write, "data", data);
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), first.getOutputsById().get("next"));

        var second = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(signal, "signal", SECOND_ENTRY_MARKER);
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), second.getOutputsById().get("next"));
        return graph;
    }

    /** {@code Structure Formed → Set Analog Signal(13)}. */
    private static MachineBlueprintGraph signalOnFormed() {
        var graph = new MachineBlueprintGraph();
        var formed = KGGameTestHelpers.addRegisteredNode(graph, StructureFormedEventNode.class);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(signal, "signal", FORMED_SIGNAL);
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), formed.getOutputsById().get("next"));
        return graph;
    }

    // ---- recipe content editing -----------------------------------------------------------------

    /**
     * Input slot 0, an unnamed output slot 1, and output slot 2 bound to {@link #BONUS_SLOT}.
     *
     * <p>The named slot is not decoration: a content with a slot name only matches a handler that
     * declares that name, so without slot 2 the recipe would never match and every test below would
     * fail for a reason that has nothing to do with the nodes.</p>
     */
    private static TestMachineBuilder twoOutputMachine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withItemSlots(1, IO.OUT, definition -> definition.setSlotNames(List.of(BONUS_SLOT)))
                .withRecipeType(TWO_OUTPUT_RECIPE_TYPE_ID);
    }

    /** {@code Recipe Modify -> Remove Recipe Content(OUT, index) -> Set Event Recipe}. */
    private static MachineBlueprintGraph removeOutputAt(int index) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var remove = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.RemoveContent.class);
        KGGameTestHelpers.setInputConstant(remove, "index", index);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, remove.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), remove.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /** The same, except the index comes from {@code Content Index Of Slot} rather than a constant. */
    private static MachineBlueprintGraph removeOutputNamed(String slotName) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var lookup = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentIndexOfSlot.class);
        KGGameTestHelpers.setInputConstant(lookup, "slotName", slotName);
        var remove = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.RemoveContent.class);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, lookup.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, remove.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, remove.getInputsById().get("index"), lookup.getOutputsById().get("index"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), remove.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Content At(index) -> Content With(chance 0) -> Set Recipe Content(index) -> Set Event
     * Recipe}: reading a content, editing it and putting it back, none of which knows or cares which
     * capability it is dealing with. A chance of zero is never rolled, so the output stops appearing.
     */
    private static MachineBlueprintGraph zeroChanceOutputAt(int index) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var at = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentAt.class);
        KGGameTestHelpers.setInputConstant(at, "index", index);
        var with = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentWith.class);
        KGGameTestHelpers.setInputConstant(with, "chance", 0f);
        // The slot name has to survive the edit, or the content stops matching the slot it is bound
        // to and the recipe fails for a reason that is not the one under test.
        KGGameTestHelpers.setInputConstant(with, "slotName", BONUS_SLOT);
        var set = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.SetContent.class);
        KGGameTestHelpers.setInputConstant(set, "index", index);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, at.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, with.getInputsById().get("content"), at.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, set.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, set.getInputsById().get("content"), with.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), set.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Content At(0) -> Content Value -> Content Of -> Add Recipe Content -> Set Event Recipe}:
     * the payload comes out of one content and goes straight back into a new one, so the machine
     * produces its first output twice.
     *
     * <p>Doubling rather than replacing is what makes it observable: the payload has to survive
     * unchanged for the count to land on four, and a {@code Content Value} that returned nothing
     * leaves the recipe alone and the count at two. Nothing in the graph names an item — the whole
     * round trip is capability-generic.</p>
     */
    private static MachineBlueprintGraph echoOutputAt(int index) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var at = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentAt.class);
        KGGameTestHelpers.setInputConstant(at, "index", index);
        var value = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentValue.class);
        var of = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentOf.class);
        var add = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.AddContent.class);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, at.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, value.getInputsById().get("content"), at.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, of.getInputsById().get("value"), value.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("content"), of.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), add.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /**
     * The echo again, except the content goes out to NBT and is parsed back before being added.
     *
     * <p>This is the escape hatch a capability from another mod has to rely on, exercised on one MBD2
     * does have typed nodes for - the point being that the codec path is what is under test, not the
     * item-ness. A round trip cannot pass by accident: the two nodes have different types on both
     * sides, so neither can be the identity.</p>
     */
    private static MachineBlueprintGraph echoOutputThroughNbt(int index) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var at = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentAt.class);
        KGGameTestHelpers.setInputConstant(at, "index", index);
        var toNbt = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentToNbt.class);
        var fromNbt = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentFromNbt.class);
        var add = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.AddContent.class);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, at.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, toNbt.getInputsById().get("content"), at.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, fromNbt.getInputsById().get("nbt"), toNbt.getOutputsById().get("nbt"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("content"), fromNbt.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), add.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    /**
     * The echo again, except the ingredient is unpacked into a stack and the new content is built
     * from that stack rather than from the ingredient.
     *
     * <p>What that adds over {@link #echoOutputThroughNbt} is {@code Ingredient Info}: the payload has
     * to actually resolve to an item and a count, not merely be passed along, or the rebuilt content
     * is an ingredient matching nothing and no extra dirt appears.</p>
     */
    private static MachineBlueprintGraph echoOutputThroughStack(int index) {
        var graph = new MachineBlueprintGraph();
        var modify = KGGameTestHelpers.addRegisteredNode(graph, RecipeModifyBeforeEventNode.class);
        var at = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentAt.class);
        KGGameTestHelpers.setInputConstant(at, "index", index);
        var value = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentValue.class);
        var info = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.IngredientInfo.class);
        var of = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentOf.class);
        var add = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.AddContent.class);
        var write = KGGameTestHelpers.addRegisteredNode(graph, SetEventRecipeNode.class);

        KGGameTestHelpers.wire(graph, at.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, value.getInputsById().get("content"), at.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, info.getInputsById().get("ingredient"), value.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, of.getInputsById().get("value"), info.getOutputsById().get("first"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("recipe"), modify.getOutputsById().get("recipe"));
        KGGameTestHelpers.wire(graph, add.getInputsById().get("content"), of.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("recipe"), add.getOutputsById().get("result"));
        KGGameTestHelpers.wire(graph, write.getInputsById().get("in"), modify.getOutputsById().get("next"));
        return graph;
    }

    // ---- payload makers and readers, one capability at a time ------------------------------------

    /** {@code Ingredient Of Tag(#planks, 3)} round-tripped through the item capability. */
    private static MachineBlueprintGraph itemPayloadRoundTrip() {
        return PayloadRoundTrip.graph(ItemRecipeCapability.CAP.name,
                RecipeContentNodes.IngredientOfTag.class, "ingredient",
                maker -> {
                    KGGameTestHelpers.setInputConstant(maker, "tag", ResourceLocation.withDefaultNamespace("planks"));
                    KGGameTestHelpers.setInputConstant(maker, "count", ITEM_PAYLOAD_COUNT);
                },
                RecipeContentNodes.IngredientInfo.class, "ingredient", "count", null);
    }

    /** {@code Fluid Ingredient Of Tag(#water, 6)} round-tripped through the fluid capability. */
    private static MachineBlueprintGraph fluidPayloadRoundTrip() {
        return PayloadRoundTrip.graph(FluidRecipeCapability.CAP.name,
                RecipeContentNodes.FluidIngredientOfTag.class, "ingredient",
                maker -> {
                    KGGameTestHelpers.setInputConstant(maker, "tag", ResourceLocation.withDefaultNamespace("water"));
                    KGGameTestHelpers.setInputConstant(maker, "amount", FLUID_PAYLOAD_AMOUNT);
                },
                RecipeContentNodes.FluidIngredientInfo.class, "ingredient", "amount", null);
    }

    /**
     * The entity capability, with an extra step the other two do not need.
     *
     * <p>{@code Entity Ingredient Of} takes an entity type, and nothing in the node set produces one
     * as a constant, so the tag maker builds an ingredient first and {@code Entity Ingredient Info}
     * resolves it to a type. That makes the graph cover three nodes rather than two, and it is why
     * the tag step asks for a different count than the rebuild does: the signal at the end is the
     * rebuild's count, so a chain that skipped the rebuild would show two rather than five.</p>
     */
    private static MachineBlueprintGraph entityPayloadRoundTrip() {
        var built = PayloadRoundTrip.build(EntityRecipeCapability.CAP.name,
                RecipeContentNodes.EntityIngredientOf.class, "ingredient",
                maker -> KGGameTestHelpers.setInputConstant(maker, "count", ENTITY_PAYLOAD_COUNT),
                RecipeContentNodes.EntityIngredientInfo.class, "ingredient", "count", null);

        // Feed the maker's entity type from a tag, resolved through a second Info.
        var graph = built.graph();
        var maker = built.maker();
        var fromTag = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientOfTag.class);
        KGGameTestHelpers.setInputConstant(fromTag, "tag", ResourceLocation.withDefaultNamespace("skeletons"));
        KGGameTestHelpers.setInputConstant(fromTag, "count", ENTITY_TAG_COUNT);
        var resolve = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.EntityIngredientInfo.class);
        KGGameTestHelpers.wire(graph, resolve.getInputsById().get("ingredient"), fromTag.getOutputsById().get("ingredient"));
        KGGameTestHelpers.wire(graph, maker.getInputsById().get("type"), resolve.getOutputsById().get("first"));
        return graph;
    }
}

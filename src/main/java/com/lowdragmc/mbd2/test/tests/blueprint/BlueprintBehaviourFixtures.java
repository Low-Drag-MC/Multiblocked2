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
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.EnergyNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

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

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
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
}

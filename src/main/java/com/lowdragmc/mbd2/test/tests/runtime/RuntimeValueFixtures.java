package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRuntimeValueNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.tests.recipe.ItemRecipeCapabilityFixtures;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Machines for {@link RuntimeValueTests}. The definition deliberately keeps every value at its default
 * so the tests can assert "no override == the authored value" without pinning specific numbers.
 */
public class RuntimeValueFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_runtime_value_machine");
    /**
     * Auto-IO pulling from the right side every tick. Dedicated to these tests rather than shared with
     * {@code AutoIOTraitFixtures}, because {@link RuntimeValueTests} mutates its trait definition to
     * prove that unoverridden siblings still track the definition — and a definition is global.
     */
    public static final ResourceLocation AUTO_IO_MACHINE_ID = MBD2.id("test_runtime_value_auto_io");
    /** Same machine, plus a blueprint that switches its auto IO off on the first tick. */
    public static final ResourceLocation BLUEPRINT_AUTO_IO_ID = MBD2.id("test_runtime_value_blueprint_auto_io");

    /** Two item-slot traits, to prove overrides are namespaced per trait rather than per machine. */
    public static final ResourceLocation TWO_SLOT_TRAITS_ID = MBD2.id("test_runtime_value_two_traits");
    /** Redstone-connected on every side, so a signal_connection override has something to switch off. */
    public static final ResourceLocation SIGNAL_MACHINE_ID = MBD2.id("test_runtime_value_signal");
    /**
     * Bound to a real recipe type, so {@code runRecipeLogic()} is true by default — a machine with no
     * recipe type is short-circuited by {@code IMachine.runRecipeLogic} before the override is consulted.
     */
    public static final ResourceLocation RECIPE_LOGIC_MACHINE_ID = MBD2.id("test_runtime_value_recipe_logic");
    /** Energy storage with a generous transfer rate, for testing max_receive / max_extract overrides. */
    public static final ResourceLocation ENERGY_MACHINE_ID = MBD2.id("test_runtime_value_energy");

    /**
     * One input item-slot trait and one output one, both plain. For the recipe-handler triple —
     * {@code recipe_handler_io}, {@code distinct}, {@code slot_names} — which is about how the recipe
     * engine buckets handlers rather than about any one trait's contents.
     */
    public static final ResourceLocation RECIPE_HANDLER_MACHINE_ID = MBD2.id("test_runtime_value_recipe_handler");
    /**
     * Bound to a real recipe type and authored with {@code consumeInputsAfterWorking}, so the fix for
     * "the setting was never read" has something to be true of.
     */
    public static final ResourceLocation CONSUME_AFTER_MACHINE_ID = MBD2.id("test_runtime_value_consume_after");
    /** The same machine with the setting left at its default, to prove the two differ. */
    public static final ResourceLocation CONSUME_BEFORE_MACHINE_ID = MBD2.id("test_runtime_value_consume_before");
    /** An item slot whose definition filters to diamonds only, for the {@code filter.enable} override. */
    public static final ResourceLocation FILTERED_MACHINE_ID = MBD2.id("test_runtime_value_filtered");
    /** The same filter, authored <b>off</b> — so an override has to be able to switch one on, not just off. */
    public static final ResourceLocation UNFILTERED_MACHINE_ID = MBD2.id("test_runtime_value_unfiltered");
    /** A tank with room for exactly one bucket, for the {@code capacity} override. */
    public static final ResourceLocation TANK_MACHINE_ID = MBD2.id("test_runtime_value_tank");
    /** Item slots plus a blueprint that writes text and box values on the first tick. */
    public static final ResourceLocation BLUEPRINT_TEXT_AND_BOX_ID = MBD2.id("test_runtime_value_blueprint_text_box");
    /** Item slots plus a blueprint that switches world scanning on and points it at a neighbouring block. */
    public static final ResourceLocation BLUEPRINT_WORLD_IO_ID = MBD2.id("test_runtime_value_blueprint_world_io");

    /** The name the first item-slot trait gets — see {@code TestMachineBuilder.withItemSlots}. */
    public static final String ITEM_SLOT_TRAIT = "item_slot";
    /** The name the second one gets. */
    public static final String SECOND_ITEM_SLOT_TRAIT = "item_slot_1";

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withItemSlots(2, IO.BOTH)
                .register(event);
        TestMachineBuilder.simple(AUTO_IO_MACHINE_ID)
                .withItemSlots(1, IO.BOTH, RuntimeValueFixtures::pullFromTheRight)
                .register(event);
        TestMachineBuilder.simple(BLUEPRINT_AUTO_IO_ID)
                .withItemSlots(1, IO.BOTH, RuntimeValueFixtures::pullFromTheRight)
                .withBlueprint(disableAutoIO())
                .register(event);
        TestMachineBuilder.simple(TWO_SLOT_TRAITS_ID)
                .withItemSlots(1, IO.BOTH)
                .withItemSlots(1, IO.BOTH)
                .register(event);
        TestMachineBuilder.simple(SIGNAL_MACHINE_ID)
                .withItemSlots(1, IO.BOTH)
                .register(event);
        TestMachineBuilder.simple(RECIPE_LOGIC_MACHINE_ID)
                .withItemSlots(1, IO.BOTH)
                .withRecipeType(ItemRecipeCapabilityFixtures.RECIPE_TYPE_ID)
                .register(event);
        TestMachineBuilder.simple(ENERGY_MACHINE_ID)
                .withEnergy(5000)
                .register(event);
        TestMachineBuilder.simple(RECIPE_HANDLER_MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .register(event);
        TestMachineBuilder.simple(CONSUME_AFTER_MACHINE_ID)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(ItemRecipeCapabilityFixtures.RECIPE_TYPE_ID)
                .withConsumeInputsAfterWorking(true)
                .register(event);
        TestMachineBuilder.simple(CONSUME_BEFORE_MACHINE_ID)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(ItemRecipeCapabilityFixtures.RECIPE_TYPE_ID)
                .register(event);
        TestMachineBuilder.simple(FILTERED_MACHINE_ID)
                .withItemSlots(1, IO.BOTH, definition -> diamondsOnly(definition, true))
                .register(event);
        TestMachineBuilder.simple(UNFILTERED_MACHINE_ID)
                .withItemSlots(1, IO.BOTH, definition -> diamondsOnly(definition, false))
                .register(event);
        TestMachineBuilder.simple(TANK_MACHINE_ID)
                .withFluidTanks(1, 1000)
                .register(event);
        TestMachineBuilder.simple(BLUEPRINT_TEXT_AND_BOX_ID)
                // two traits: the second is where the reader nodes copy to, which is how they are
                // observed at all — a Get node's output has to be written somewhere to be asserted on
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.IN)
                .withBlueprint(exerciseValueNodes())
                .register(event);
        TestMachineBuilder.simple(BLUEPRINT_WORLD_IO_ID)
                .withItemSlots(1, IO.BOTH)
                .withBlueprint(enableWorldInput())
                .register(event);
    }

    /** A whitelist of exactly diamonds, switched on or off as the caller asks. */
    private static void diamondsOnly(ItemSlotCapabilityTraitDefinition definition, boolean enable) {
        var filter = definition.getItemFilterSettings();
        filter.setEnable(enable);
        filter.setWhitelist(true);
        filter.setFilterItems(List.of(new ItemStack(Items.DIAMOND)));
    }

    private static void pullFromTheRight(ItemSlotCapabilityTraitDefinition definition) {
        definition.getAutoIO().setEnable(true);
        definition.getAutoIO().setInterval(1);
        definition.getAutoIO().setRightIO(IO.IN);
    }

    /**
     * {@code Machine Tick → Set Auto IO Enabled(item_slot, false)}.
     *
     * <p>The tick event fires before the traits tick — see {@code MBDMachine.serverTick} — so the
     * override is already in place the first time auto IO would have run. A machine that pulls even one
     * item means the node did not reach the trait.</p>
     */
    private static MachineBlueprintGraph disableAutoIO() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var disable = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetAutoIOEnabled.class);
        KGGameTestHelpers.setInputConstant(disable, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(disable, "enabled", false);
        KGGameTestHelpers.wire(graph, disable.getInputsById().get("in"), tick.getOutputsById().get("next"));
        return graph;
    }

    /** The slot names the text-writing blueprint puts on its trait. */
    public static final String BLUEPRINT_SLOT_NAMES = "alpha,beta";
    /** The box the box-writing blueprint puts on its trait, as {@code minX minY minZ maxX maxY maxZ}. */
    public static final double[] BLUEPRINT_BOX = {-2, -1, -3, 4, 5, 6};

    /** The corner port names, in the order {@link #BLUEPRINT_BOX} gives them. */
    private static final String[] CORNERS = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"};

    /**
     * One graph exercising every new by-name node, chained off {@code Machine Tick}:
     * <ol>
     *   <li>{@code Set Runtime Value (Text)} writes {@code slot_names} on the first trait,</li>
     *   <li>{@code Set Runtime Value (Box)} writes {@code auto_world_input.range} on it,</li>
     *   <li>{@code Runtime Value Names → count} feeds {@code Set Runtime Value (Number)},</li>
     *   <li>{@code Get Runtime Text} copies {@code slot_names} to the second trait,</li>
     *   <li>{@code Get Runtime Box} copies the range to the second trait.</li>
     * </ol>
     *
     * <p>One graph rather than five fixtures because each step asserts on a different value, so a
     * failure still points at one node. The last two are the only way a reader node is observable at
     * all: its output has to be written somewhere before a test can look at it.</p>
     */
    private static MachineBlueprintGraph exerciseValueNodes() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);

        var text = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetRuntimeString.class);
        KGGameTestHelpers.setInputConstant(text, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(text, "key", "slot_names");
        KGGameTestHelpers.setInputConstant(text, "value", BLUEPRINT_SLOT_NAMES);
        KGGameTestHelpers.wire(graph, text.getInputsById().get("in"), tick.getOutputsById().get("next"));

        var box = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetRuntimeBox.class);
        KGGameTestHelpers.setInputConstant(box, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(box, "key", "auto_world_input.range");
        for (int i = 0; i < CORNERS.length; i++) {
            KGGameTestHelpers.setInputConstant(box, CORNERS[i], BLUEPRINT_BOX[i]);
        }
        KGGameTestHelpers.wire(graph, box.getInputsById().get("in"), text.getOutputsById().get("next"));

        // Runtime Value Names -> count -> slot_limit, so the count is observable as a number the test
        // can compare against the trait's own slot list
        var keys = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.RuntimeValueKeys.class);
        KGGameTestHelpers.setInputConstant(keys, "trait", ITEM_SLOT_TRAIT);
        var countToLimit = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetRuntimeInt.class);
        KGGameTestHelpers.setInputConstant(countToLimit, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(countToLimit, "key", "slot_limit");
        KGGameTestHelpers.wire(graph, countToLimit.getInputsById().get("value"), keys.getOutputsById().get("count"));
        KGGameTestHelpers.wire(graph, countToLimit.getInputsById().get("in"), box.getOutputsById().get("next"));

        var readText = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.GetRuntimeString.class);
        KGGameTestHelpers.setInputConstant(readText, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(readText, "key", "slot_names");
        var copyText = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetRuntimeString.class);
        KGGameTestHelpers.setInputConstant(copyText, "trait", SECOND_ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(copyText, "key", "slot_names");
        KGGameTestHelpers.wire(graph, copyText.getInputsById().get("value"), readText.getOutputsById().get("value"));
        KGGameTestHelpers.wire(graph, copyText.getInputsById().get("in"), countToLimit.getOutputsById().get("next"));

        var readBox = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.GetRuntimeBox.class);
        KGGameTestHelpers.setInputConstant(readBox, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(readBox, "key", "auto_world_input.range");
        var copyBox = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetRuntimeBox.class);
        KGGameTestHelpers.setInputConstant(copyBox, "trait", SECOND_ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(copyBox, "key", "auto_world_input.range");
        for (var corner : CORNERS) {
            KGGameTestHelpers.wire(graph, copyBox.getInputsById().get(corner), readBox.getOutputsById().get(corner));
        }
        KGGameTestHelpers.wire(graph, copyBox.getInputsById().get("in"), copyText.getOutputsById().get("next"));
        return graph;
    }

    /**
     * {@code Machine Tick → Set Auto World IO Enabled/Interval/Speed/Range}, all four on the input half.
     *
     * <p>Drives the whole set from a graph and then lets the trait actually run, so the test can assert
     * a dropped item is picked up rather than just that four values changed.</p>
     */
    private static MachineBlueprintGraph enableWorldInput() {
        var graph = new MachineBlueprintGraph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);

        var enable = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetAutoWorldIOEnabled.class);
        KGGameTestHelpers.setInputConstant(enable, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(enable, "io", IO.IN);
        KGGameTestHelpers.setInputConstant(enable, "enabled", true);
        KGGameTestHelpers.wire(graph, enable.getInputsById().get("in"), tick.getOutputsById().get("next"));

        var interval = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetAutoWorldIOInterval.class);
        KGGameTestHelpers.setInputConstant(interval, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(interval, "io", IO.IN);
        KGGameTestHelpers.setInputConstant(interval, "interval", 1);
        KGGameTestHelpers.wire(graph, interval.getInputsById().get("in"), enable.getOutputsById().get("next"));

        var speed = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetAutoWorldIOSpeed.class);
        KGGameTestHelpers.setInputConstant(speed, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(speed, "io", IO.IN);
        KGGameTestHelpers.setInputConstant(speed, "speed", 64);
        KGGameTestHelpers.wire(graph, speed.getInputsById().get("in"), interval.getOutputsById().get("next"));

        var range = KGGameTestHelpers.addRegisteredNode(graph, MachineRuntimeValueNodes.SetAutoWorldIORange.class);
        KGGameTestHelpers.setInputConstant(range, "trait", ITEM_SLOT_TRAIT);
        KGGameTestHelpers.setInputConstant(range, "io", IO.IN);
        // a 3x3x3 shell around the machine, so the test can drop an item on any neighbouring block
        var values = new double[]{-1, -1, -1, 2, 2, 2};
        for (int i = 0; i < CORNERS.length; i++) {
            KGGameTestHelpers.setInputConstant(range, CORNERS[i], values[i]);
        }
        KGGameTestHelpers.wire(graph, range.getInputsById().get("in"), speed.getOutputsById().get("next"));
        return graph;
    }
}

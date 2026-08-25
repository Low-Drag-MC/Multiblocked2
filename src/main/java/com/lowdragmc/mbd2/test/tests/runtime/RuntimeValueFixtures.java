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
}

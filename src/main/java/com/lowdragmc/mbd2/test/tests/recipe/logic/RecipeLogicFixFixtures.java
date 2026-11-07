package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.AfterRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.FuelBurningFinishEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineAfterRecipeWorkingEvent;
import com.lowdragmc.mbd2.common.recipe.MachineLevelCondition;
import com.lowdragmc.mbd2.common.recipe.RedstoneSignalCondition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;

/** Fixtures for {@link RecipeLogicFixTests}: one per recipe logic bug, each the smallest setup that shows it. */
public class RecipeLogicFixFixtures implements TestFixtureProvider {
    /** {@link RecipeLogicGoldenFixtures#FUEL_MACHINE} with a blueprint marking the fuel burning out. */
    public static final ResourceLocation FUEL_BLUEPRINT_MACHINE = MBD2.id("logic_fix_fuel_blueprint_machine");
    public static final int FUEL_OUT_MARKER = 9;

    /** Stone → dirt, but only on a level 5 machine: on a level 0 one it matches its contents and fails its condition. */
    public static final ResourceLocation GATED_TYPE = MBD2.id("logic_fix_gated_recipes");
    public static final ResourceLocation GATED_MACHINE = MBD2.id("logic_fix_gated_machine");
    /** Stone → dirt, but only with a redstone signal: a condition nothing reports the change of. */
    public static final ResourceLocation POWERED_TYPE = MBD2.id("logic_fix_powered_recipes");
    public static final ResourceLocation POWERED_MACHINE = MBD2.id("logic_fix_powered_machine");

    /** Proxies a recipe type that does not exist, then vanilla smelting. */
    public static final ResourceLocation PROXY_TYPE = MBD2.id("logic_fix_proxy_recipes");
    public static final ResourceLocation MISSING_TYPE = MBD2.id("logic_fix_no_such_recipe_type");

    /** One recipe with no contents at all. */
    public static final ResourceLocation EMPTY_TYPE = MBD2.id("logic_fix_empty_recipes");
    /** No traits, runs {@link #EMPTY_TYPE}. */
    public static final ResourceLocation TRAITLESS_MACHINE = MBD2.id("logic_fix_traitless_machine");

    /** One 2-slot item trait for inputs and outputs alike; recipes take their inputs when they finish. */
    public static final ResourceLocation SHARED_SLOTS_AFTER_MACHINE = MBD2.id("logic_fix_shared_slots_after_machine");

    /** Two cobblestone to gravel, taken when the recipe finishes. */
    public static final ResourceLocation PAIR_TYPE = MBD2.id("logic_fix_pair_recipes");
    /** 2 input slots (0, 1), 2 output slots (2, 3), runs {@link #PAIR_TYPE} taking its inputs when it finishes. */
    public static final ResourceLocation PAIR_AFTER_MACHINE = MBD2.id("logic_fix_pair_after_machine");

    /** Stone to dirt in no time at all, as a modifier rounding a short duration down makes a recipe. */
    public static final ResourceLocation INSTANT_TYPE = MBD2.id("logic_fix_instant_recipes");
    /** An input slot (0) and an output slot (1), runs {@link #INSTANT_TYPE}. */
    public static final ResourceLocation INSTANT_MACHINE = MBD2.id("logic_fix_instant_machine");

    /** {@link RecipeLogicGoldenFixtures#MACHINE}, its recipe interrupted from After Recipe Working by {@link #listenForInterrupter()}. */
    public static final ResourceLocation INTERRUPT_AFTER_MACHINE = MBD2.id("logic_fix_interrupt_after_machine");
    private static boolean interrupterListening;
    /** {@link RecipeLogicGoldenFixtures#MACHINE} with {@code After Recipe Working → Reset Recipe Logic}. */
    public static final ResourceLocation RESET_AFTER_MACHINE = MBD2.id("logic_fix_reset_after_machine");

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(GATED_TYPE)
                .recipe("gated_press", b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .addCondition(new MachineLevelCondition(5))
                        .duration(10))
                .register(event);
        event.register(new MBDRecipeType(PROXY_TYPE, MISSING_TYPE, ResourceLocation.withDefaultNamespace("smelting")));
        TestRecipeTypeBuilder.of(EMPTY_TYPE)
                .recipe("empty_recipe", b -> b.duration(10))
                .register(event);
        TestRecipeTypeBuilder.of(POWERED_TYPE)
                .recipe("powered_press", b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .addCondition(new RedstoneSignalCondition(1, 15))
                        .duration(10))
                .register(event);
        TestRecipeTypeBuilder.of(PAIR_TYPE)
                .recipe("pair_crush", b -> b
                        .inputItems(Items.COBBLESTONE, 2)
                        .outputItems(Items.GRAVEL)
                        .duration(10))
                .register(event);
        TestRecipeTypeBuilder.of(INSTANT_TYPE)
                .recipe("instant_press", b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .duration(0))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(FUEL_BLUEPRINT_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(RecipeLogicGoldenFixtures.FUELED_TYPE)
                .withBlueprint(markFuelBurningOut())
                .register(event);
        TestMachineBuilder.simple(GATED_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(GATED_TYPE)
                .register(event);
        TestMachineBuilder.simple(POWERED_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(POWERED_TYPE)
                .register(event);
        TestMachineBuilder.simple(TRAITLESS_MACHINE)
                .withRecipeType(EMPTY_TYPE)
                .register(event);
        TestMachineBuilder.simple(SHARED_SLOTS_AFTER_MACHINE)
                .withItemSlots(2, IO.BOTH)
                .withRecipeType(RecipeLogicGoldenFixtures.RECIPE_TYPE)
                .withConsumeInputsAfterWorking(true)
                .register(event);
        TestMachineBuilder.simple(PAIR_AFTER_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(PAIR_TYPE)
                .withConsumeInputsAfterWorking(true)
                .register(event);
        TestMachineBuilder.simple(INSTANT_MACHINE)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(INSTANT_TYPE)
                .register(event);
        stoneToDirt(INTERRUPT_AFTER_MACHINE).register(event);
        stoneToDirt(RESET_AFTER_MACHINE).withBlueprint(resetAfterRecipeWorking()).register(event);
    }

    /** Interrupt the recipe of an {@link #INTERRUPT_AFTER_MACHINE} from its After Recipe Working event, as a script would. */
    public static synchronized void listenForInterrupter() {
        if (interrupterListening) return;
        interrupterListening = true;
        NeoForge.EVENT_BUS.addListener(MachineAfterRecipeWorkingEvent.class, event -> {
            if (event.getMachine().getDefinition().id().equals(INTERRUPT_AFTER_MACHINE)) {
                event.recipeLogic.interruptRecipe();
            }
        });
    }

    private static TestMachineBuilder stoneToDirt(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(RecipeLogicGoldenFixtures.RECIPE_TYPE);
    }

    /** {@code After Recipe Working → Reset Recipe Logic}. */
    private static MachineBlueprintGraph resetAfterRecipeWorking() {
        var graph = new MachineBlueprintGraph();
        var after = KGGameTestHelpers.addRegisteredNode(graph, AfterRecipeWorkingEventNode.class);
        var reset = KGGameTestHelpers.addRegisteredNode(graph, RecipeLogicActionNodes.Reset.class);
        KGGameTestHelpers.wire(graph, reset.getInputsById().get("in"), after.getOutputsById().get("next"));
        return graph;
    }

    /** {@code Fuel Burning Finish → Set Analog Signal(9)}. */
    private static MachineBlueprintGraph markFuelBurningOut() {
        var graph = new MachineBlueprintGraph();
        var finish = KGGameTestHelpers.addRegisteredNode(graph, FuelBurningFinishEventNode.class);
        var marker = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(marker, "signal", FUEL_OUT_MARKER);
        KGGameTestHelpers.wire(graph, marker.getInputsById().get("in"), finish.getOutputsById().get("next"));
        return graph;
    }
}

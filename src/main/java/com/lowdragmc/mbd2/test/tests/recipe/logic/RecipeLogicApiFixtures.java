package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/** Fixtures for {@link RecipeLogicApiTests}. They run {@link RecipeLogicGoldenFixtures#RECIPE_TYPE}. */
public class RecipeLogicApiFixtures implements TestFixtureProvider {
    /** {@link RecipeLogicGoldenFixtures#MACHINE} built as a {@link NewHookMachines.SimpleMachine}. */
    public static final ResourceLocation NEW_HOOK_MACHINE = MBD2.id("logic_api_new_hook_machine");
    /** {@link RecipeLogicGoldenFixtures#MACHINE} with {@link #suspendTheWorkingLogic()} bound. */
    public static final ResourceLocation EVENT_LOGIC_MACHINE = MBD2.id("logic_api_event_logic_machine");
    public static final int SUSPENDED_MARKER = 5;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        machine(NEW_HOOK_MACHINE).withMachineFactory(NewHookMachines.SimpleMachine::new).register(event);
        machine(EVENT_LOGIC_MACHINE).withBlueprint(suspendTheWorkingLogic()).register(event);
    }

    private static TestMachineBuilder machine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(RecipeLogicGoldenFixtures.RECIPE_TYPE);
    }

    /**
     * {@code On Recipe Working → Set Working Enabled(false) → Set Analog Signal(5)}, with the event's
     * {@code recipeLogic} wired into the action: the logic that is working suspends itself.
     */
    public static MachineBlueprintGraph suspendTheWorkingLogic() {
        var graph = new MachineBlueprintGraph();
        var working = KGGameTestHelpers.addRegisteredNode(graph, OnRecipeWorkingEventNode.class);
        var disable = KGGameTestHelpers.addRegisteredNode(graph, RecipeLogicActionNodes.SetWorkingEnabled.class);
        KGGameTestHelpers.setInputConstant(disable, "enabled", false);
        var marker = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(marker, "signal", SUSPENDED_MARKER);
        KGGameTestHelpers.wire(graph, disable.getInputsById().get("in"), working.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, disable.getInputsById().get("recipeLogic"), working.getOutputsById().get("recipeLogic"));
        KGGameTestHelpers.wire(graph, marker.getInputsById().get("in"), disable.getOutputsById().get("next"));
        return graph;
    }
}

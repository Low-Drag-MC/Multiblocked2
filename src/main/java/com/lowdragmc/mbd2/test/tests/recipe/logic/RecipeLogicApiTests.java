package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.MachineEventRecorder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenFixtures.*;
import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenTests.*;

/**
 * The recipe logic API added for machines with several logics, on a machine that still has one: addresses, the
 * logic as its own capability holder, the per-logic hooks, the logic every recipe event carries, and the
 * blueprint port that hands it on.
 */
@GameTestHolder(MBD2.MOD_ID)
public class RecipeLogicApiTests {
    static { @SuppressWarnings("unused") var ignored = RecipeLogicApiFixtures.NEW_HOOK_MACHINE; }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void primary_logic_is_addressable(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(MACHINE, POS).machine();
        var logic = machine.getRecipeLogic();
        check(h, logic.getName().equals(RecipeLogic.PRIMARY_NAME) && logic.getThreadIndex() == 0
                && logic.getAddress().equals("main"), "the primary logic is main, thread 0: " + logic);
        check(h, logic.isPrimary(), "the primary logic does not know it is primary");
        check(h, machine.getRecipeLogics().equals(List.of(logic)), "a single-logic machine lists more than its logic");
        check(h, machine.getRecipeLogic("main") == logic && machine.getRecipeLogic("main#0") == logic,
                "the primary logic is not found by its address");
        check(h, machine.getRecipeLogic("main#1") == null && machine.getRecipeLogic("other") == null,
                "an address of no logic found one");
        check(h, machine.getContextRecipeLogic() == logic, "outside a hook the context logic is not the primary");
        check(h, logic.getRecipeType() == machine.getRecipeType(), "the logic runs another recipe type than the machine");
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_logic_is_a_capability_holder_over_its_handlers(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(MACHINE, POS).machine();
        var logic = machine.getRecipeLogic();
        var recipe = (MBDRecipe) h.getLevel().getRecipeManager().byKey(MBD2.id(PRESS)).orElseThrow().value();
        check(h, logic.getRecipeCapabilitiesProxy().equals(machine.getRecipeCapabilitiesProxy()),
                "with nothing scoped, the logic does not see exactly the machine's handlers");
        check(h, logic.getChanceTier() == machine.getChanceTier(), "the logic has its own chance tier");
        check(h, !recipe.matchRecipe(logic).isSuccess() && !recipe.matchRecipe(machine).isSuccess(),
                "an empty machine matched the recipe");
        MBDTestHelper.insertItem(h, machine, 0, new ItemStack(Items.STONE));
        MBDTestHelper.insertEnergy(h, machine, 1_000);
        check(h, recipe.matchRecipe(logic).isSuccess() && recipe.matchRecipe(machine).isSuccess()
                        && recipe.matchTickRecipe(logic).isSuccess(),
                "matching through the logic disagrees with matching through the machine");
        h.succeed();
    }

    /** Every recipe event names the logic it is about; the state change, which is the machine's, does not. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void every_recipe_event_names_its_logic(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS).withLogics()) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isWaiting(), 20)
                    .insertEnergy(1_000)
                    .runUntil(m -> logic(m).isIdle(), 40);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)@main",
                    "RecipeModifyAfter(golden_press)@main",
                    "BeforeRecipeWorking(golden_press)@main",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)@main",
                    "OnRecipeWorking(golden_press)@main ×10",
                    "StateChanged(working->waiting)",
                    "RecipeStatusChanged(WORKING->WAITING)@main",
                    "OnRecipeWaiting(golden_press)@main",
                    "StateChanged(waiting->working)",
                    "RecipeStatusChanged(WAITING->WORKING)@main",
                    "OnRecipeWorking(golden_press)@main ×12",
                    "AfterRecipeWorking(golden_press)@main",
                    "OnRecipeFinish(golden_press)@main",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)@main");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fuel_and_consume_after_events_name_their_logic(GameTestHelper h) {
        var fuel = MBDScenario.of(h).placeMachine(FUEL_MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS).withLogics()) {
            fuel.insertItem(0, new ItemStack(Items.SAND))
                    .insertItem(1, new ItemStack(Items.COAL))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .runUntil(m -> logic(m).getFuelTime() == 0, 40);
            var entries = trace.raw();
            check(h, entries.contains("FuelRecipeModify(golden_coal)@main") && entries.contains("FuelBurningFinish(golden_coal)@main"),
                    "the fuel events do not name the logic: " + entries);
        }
        var after = MBDScenario.of(h).placeMachine(AFTER_MACHINE, STONE_POS);
        try (var trace = MachineEventRecorder.start(h, STONE_POS).withLogics()) {
            after.insertItem(0, new ItemStack(Items.COBBLESTONE))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isIdle(), 20);
            check(h, trace.raw().contains("OnConsumeInputsAfterWorking(golden_after)@main"),
                    "the consume-after event does not name the logic: " + trace.raw());
        }
        h.succeed();
    }

    /** Code that posts recipe events the old way still produces events that name a logic: the context one. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void legacy_event_constructors_name_the_context_logic(GameTestHelper h) {
        MBDMachine machine = MBDScenario.of(h).placeMachine(MACHINE, POS).machine();
        var logic = machine.getRecipeLogic();
        var recipe = MBDTestHelper.dummyRecipe();
        var events = List.of(
                new MachineBeforeRecipeWorkingEvent(machine, recipe),
                new MachineOnRecipeWorkingEvent(machine, recipe, 3),
                new MachineOnRecipeWaitingEvent(machine, recipe),
                new MachineAfterRecipeWorkingEvent(machine, recipe),
                new MachineOnConsumeInputsAfterWorkingEvent(machine, recipe),
                new MachineOnRecipeFinishEvent(machine, recipe),
                new MachineRecipeModifyEvent.Before(machine, recipe),
                new MachineRecipeModifyEvent.After(machine, recipe),
                new MachineFuelRecipeModifyEvent(machine, recipe),
                new MachineFuelBurningFinishEvent(machine, recipe),
                new MachineRecipeStatusChangedEvent(machine, RecipeLogic.Status.IDLE, RecipeLogic.Status.WORKING));
        for (var event : events) {
            check(h, ((RecipeLogicEvent) event).getRecipeLogic() == logic,
                    event.getClass().getSimpleName() + " built the old way names no logic, or the wrong one");
        }
        h.succeed();
    }

    /** Through the per-logic hooks, the legacy ones see the logic they run for as their context. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void legacy_hooks_see_their_logic_as_context(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(LEGACY_MACHINE, POS);
        var counters = ((LegacyHookMachines.Counted) s.machine()).counters();
        runHookScenario(h, s);
        int calls = counters.snapshot().values().stream().mapToInt(Integer::intValue).sum();
        check(h, counters.contexts().equals(Map.of("main", calls)),
                "a legacy hook ran outside its logic's context: " + counters.contexts());
        s.succeed();
    }

    /**
     * A machine overriding only the per-logic hooks sees each called exactly as often as the legacy ones are, each
     * with its own logic, and calling super still reaches MBD2's behaviour: the trace is the plain machine's.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void per_logic_hooks_run_once_each_with_their_logic(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(RecipeLogicApiFixtures.NEW_HOOK_MACHINE, POS);
        var machine = (NewHookMachines.SimpleMachine) s.machine();
        runHookScenario(h, s);
        assertCounts(h, "per-logic hooks", machine.counters().snapshot(), SIMPLE_HOOK_COUNTS);
        int calls = SIMPLE_HOOK_COUNTS.values().stream().mapToInt(Integer::intValue).sum();
        check(h, machine.logics.equals(Map.of("main", calls)), "a per-logic hook got another logic: " + machine.logics);
        s.succeed();
    }

    /** An event node's recipeLogic port hands the logic that fired it to an action, which then acts on it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void event_node_hands_its_logic_to_an_action(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicApiFixtures.EVENT_LOGIC_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> m.getAnalogOutputSignal() == RecipeLogicApiFixtures.SUSPENDED_MARKER, START_BUDGET)
                .check("the working logic suspended itself", m -> logic(m).isSuspend())
                .runTicks(10)
                .check("and stays suspended", m -> logic(m).isSuspend() && logic(m).getProgress() <= 1)
                .succeed();
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) {
            h.fail(message);
        }
    }
}

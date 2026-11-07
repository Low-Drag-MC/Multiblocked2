package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.Trait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.MachineEventRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.lowdragmc.mbd2.test.tests.recipe.logic.MultiRecipeLogicFixtures.*;
import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenTests.*;

/**
 * Machines with several recipe logics: logics side by side on scoped slots, threads sharing slots, hooks and events
 * per logic, saving, multiblock predicates and how the machine shows them all as one status.
 */
@GameTestHolder(MBD2.MOD_ID)
public class MultiRecipeLogicTests {
    static { @SuppressWarnings("unused") var ignored = MultiRecipeLogicFixtures.ISOLATED; }

    private static final int BOTH_BUDGET = 120;

    // region identity

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void logics_are_created_named_and_addressed(GameTestHelper h) {
        var isolated = MBDScenario.of(h).placeMachine(ISOLATED, POS).machine();
        var a = isolated.getRecipeLogic();
        var b = isolated.getRecipeLogic("b");
        check(h, isolated.getRecipeLogics().equals(List.of(a, b)), "logics are not [a, b]: " + isolated.getRecipeLogics());
        check(h, a.getName().equals("a") && a.isPrimary() && isolated.getRecipeLogic("a") == a, "the primary is not a: " + a);
        check(h, b != null && b.getName().equals("b") && b.getThreadIndex() == 0 && !b.isPrimary(), "b is wrong: " + b);
        check(h, a.getRecipeType().getRegistryName().equals(TYPE_A) && b.getRecipeType().getRegistryName().equals(TYPE_B),
                "a logic runs the other's recipe type");
        check(h, isolated.getAggregatedRecipeStatus() == RecipeLogic.Status.IDLE, "a fresh machine is not idle");

        var threads = MBDScenario.of(h).placeMachine(THREADS, STONE_POS).machine();
        var addresses = threads.getRecipeLogics().stream().map(RecipeLogic::getAddress).toList();
        check(h, addresses.equals(List.of("main", "main#1", "main#2")), "threads are addressed " + addresses);
        check(h, threads.getRecipeLogic("main#2") == threads.getRecipeLogics().get(2)
                && threads.getRecipeLogic("main#0") == threads.getRecipeLogic(), "a thread is not found by its address");
        h.succeed();
    }

    /**
     * Unwired, outside a recipe event, an action node acts on every logic: switching the machine off stops its threads
     * and its other logics too, not just the primary.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_unwired_action_outside_recipe_events_acts_on_every_logic(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED_SWITCHED, POS)
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runTicks(80)
                .check("every logic is suspended", m -> m.getRecipeLogics().size() == 3
                        && m.getRecipeLogics().stream().allMatch(RecipeLogic::isSuspend))
                .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty() && MBDTestHelper.readItem(h, m, 3).isEmpty())
                .succeed();
    }

    /** Recipe Logic At left at its default address is the primary, whatever the primary is named. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_logic_at_defaults_to_the_primary(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED_PRIMARY_OFF, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> count(h, m, 3, Items.GRAVEL) == 1, START_BUDGET + 40)
                .check("only the primary was switched off", m -> logic(m).isSuspend() && count(h, m, 0, Items.STONE) == 1)
                .succeed();
    }

    /** Switched off, "Has Recipe Logic" leaves only the primary logic every machine has, and that one idle. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_machine_without_recipe_logic_runs_none(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(SWITCHED_OFF, POS)
                .check("neither a's threads nor b exist", m -> m.getRecipeLogics().equals(List.of(m.getRecipeLogic())))
                .insertItem(0, new ItemStack(Items.STONE))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runTicks(80)
                .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty() && MBDTestHelper.readItem(h, m, 3).isEmpty())
                .succeed();
    }

    // endregion

    // region scoped logics

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void scoped_logics_run_side_by_side(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking() && b(m).isWorking(), START_BUDGET)
                .runUntil(m -> logic(m).isIdle() && b(m).isIdle(), BOTH_BUDGET)
                .check("a pressed the stone into its own output", m -> count(h, m, 1, Items.DIRT) == 1)
                .check("b crushed the cobblestone into its own output", m -> count(h, m, 3, Items.GRAVEL) == 1)
                .check("both drew on the shared energy: 20 + 30 ticks at 10 FE", m -> energy(h, m) == 100_000 - 500)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_logic_never_touches_another_logics_slots(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE))
                .insertItem(2, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runTicks(80)
                .check("both logics stay idle", m -> logic(m).isIdle() && b(m).isIdle())
                .check("b left the cobblestone in a's slot alone", m -> count(h, m, 0, Items.COBBLESTONE) == 1)
                .check("a left the stone in b's slot alone", m -> count(h, m, 2, Items.STONE) == 1)
                .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty() && MBDTestHelper.readItem(h, m, 3).isEmpty())
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void each_logic_sees_only_its_scoped_handlers_and_traits(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(ISOLATED, POS).machine();
        var a = machine.getRecipeLogic();
        var b = machine.getRecipeLogic("b");
        var inA = (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot");
        var inB = (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot_2");
        var aInputs = a.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        var bInputs = b.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        check(h, aInputs.size() == 1 && bInputs.size() == 1, "each logic should see exactly its one input: " + aInputs + " / " + bInputs);
        check(h, aInputs.get(0) != bInputs.get(0), "both logics see the same input handler");
        var energy = ForgeEnergyRecipeCapability.CAP;
        check(h, a.getRecipeCapabilitiesProxy().contains(IO.IN, energy) && b.getRecipeCapabilitiesProxy().contains(IO.IN, energy),
                "the unscoped energy is not shared by both logics");
        List<ITrait> aTraits = machine.getRecipeLogicTraits(a);
        List<ITrait> bTraits = machine.getRecipeLogicTraits(b);
        check(h, aTraits.contains(inA) && !aTraits.contains(inB), "a's traits for conditions include b's slot: " + aTraits);
        check(h, bTraits.contains(inB) && !bTraits.contains(inA), "b's traits for conditions include a's slot: " + bTraits);
        check(h, machine.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP).size() == 2,
                "the machine-wide table lost a handler");
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_handler_serving_one_logic_goes_to_the_first(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(ISOLATED, POS).machine();
        var exclusive = new InertItemHandler(false);
        var shared = new InertItemHandler(true);
        var inputs = machine.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        inputs.add(exclusive);
        inputs.add(shared);
        machine.rebuildRecipeLogicProxies();
        var aInputs = machine.getRecipeLogic().getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        var bInputs = machine.getRecipeLogic("b").getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
        check(h, aInputs.contains(exclusive) && !bInputs.contains(exclusive), "the one-logic handler was not given to a alone");
        check(h, aInputs.contains(shared) && bInputs.contains(shared), "a shareable unscoped handler was not shared");
        h.succeed();
    }

    // endregion

    // region status, hooks and events

    /** The machine state follows the logics together: one change when the first starts, one when the last stops. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_machine_state_follows_the_logics_together(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(ISOLATED, POS);
        try (var trace = MachineEventRecorder.start(h, POS).withLogics()) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .insertItem(2, new ItemStack(Items.COBBLESTONE))
                    .runUntil(m -> b(m).isWorking(), START_BUDGET)
                    .check("the machine works while both do", m -> m.getMachineStateName().equals("working"))
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("one logic still working keeps the machine working", m -> b(m).isWorking()
                            && m.getMachineStateName().equals("working") && m.getAggregatedRecipeStatus() == RecipeLogic.Status.WORKING)
                    .runUntil(m -> b(m).isIdle(), 40)
                    .check("the machine is idle once both are", m -> m.getMachineStateName().equals("base"));
            var raw = trace.raw();
            var states = raw.stream().filter(e -> e.startsWith("StateChanged")).toList();
            check(h, states.equals(List.of("StateChanged(base->working)", "StateChanged(working->base)")), "state changes " + states);
            var statusesA = raw.stream().filter(e -> e.startsWith("RecipeStatusChanged") && e.endsWith("@a")).toList();
            var statusesB = raw.stream().filter(e -> e.startsWith("RecipeStatusChanged") && e.endsWith("@b")).toList();
            check(h, statusesA.equals(List.of("RecipeStatusChanged(IDLE->WORKING)@a", "RecipeStatusChanged(WORKING->IDLE)@a")), "a: " + statusesA);
            check(h, statusesB.equals(List.of("RecipeStatusChanged(IDLE->WORKING)@b", "RecipeStatusChanged(WORKING->IDLE)@b")), "b: " + statusesB);
            check(h, raw.stream().filter(e -> e.startsWith("OnRecipeWorking(multi_press)")).allMatch(e -> e.endsWith("@a"))
                    && raw.stream().filter(e -> e.startsWith("OnRecipeWorking(multi_crush)")).allMatch(e -> e.endsWith("@b")),
                    "a working event names the wrong logic");
            // modify before/after, before working, working per tick, after working, finish
            check(h, raw.stream().filter(e -> e.contains("multi_press")).count() == 20 + 5
                    && raw.stream().filter(e -> e.contains("multi_crush")).count() == 30 + 5, "per-logic event counts: " + trace.compressed());
        }
        s.succeed();
    }

    /**
     * A listener pausing {@code b} as the primary starts working — the wiki's KubeJS example — left the machine
     * showing idle: the pause summed the logics up while the primary's field still said idle, its new status not yet
     * taken, and nothing changed the sum afterwards.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_listener_changing_another_logic_leaves_the_machine_state_right(GameTestHelper h) {
        MultiRecipeLogicFixtures.listenForPauser();
        MBDScenario.of(h).placeMachine(ISOLATED_PAUSER, POS)
                .insertEnergy(100_000)
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .runUntil(m -> b(m).isWorking(), START_BUDGET)
                .check("b alone works the machine", m -> m.getMachineStateName().equals("working"))
                .insertItem(0, new ItemStack(Items.STONE))
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .check("b pauses while the primary works", m -> b(m).isSuspend())
                .check("and the machine still shows working", m -> m.getMachineStateName().equals("working")
                        && m.getAggregatedRecipeStatus() == RecipeLogic.Status.WORKING)
                .runUntil(m -> logic(m).isIdle(), 40)
                .check("b resumes once the primary is done", m -> b(m).isWorking()
                        && m.getMachineStateName().equals("working"))
                .runUntil(m -> b(m).isIdle(), 60)
                .check("idle once both are", m -> m.getMachineStateName().equals("base"))
                .succeed();
    }

    /** The aggregated status: working beats waiting beats all-suspended beats idle. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_aggregated_status_ranks_the_logics(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(THREADS, POS).machine();
        var t = machine.getRecipeLogics();
        check(h, machine.getAggregatedRecipeStatus() == RecipeLogic.Status.IDLE, "all idle is not idle");
        t.get(0).setStatus(RecipeLogic.Status.SUSPEND);
        t.get(1).setStatus(RecipeLogic.Status.SUSPEND);
        check(h, machine.getAggregatedRecipeStatus() == RecipeLogic.Status.IDLE, "some suspended, one idle is not idle");
        t.get(2).setStatus(RecipeLogic.Status.SUSPEND);
        check(h, machine.getAggregatedRecipeStatus() == RecipeLogic.Status.SUSPEND && machine.getMachineStateName().equals("suspend"),
                "all suspended is not suspend: " + machine.getMachineStateName());
        t.get(1).setStatus(RecipeLogic.Status.WAITING);
        check(h, machine.getAggregatedRecipeStatus() == RecipeLogic.Status.WAITING && machine.getMachineStateName().equals("waiting"),
                "one waiting is not waiting");
        t.get(0).setStatus(RecipeLogic.Status.WORKING);
        check(h, machine.getAggregatedRecipeStatus() == RecipeLogic.Status.WORKING && machine.getMachineStateName().equals("working"),
                "one working is not working");
        h.succeed();
    }

    /**
     * Legacy hooks see each logic as their context while it runs; per-logic hooks get it as an argument. Exact
     * counts: a = 31 legacy hook calls (20 working ticks), b = 41 (30); the legacy status hook fires only for the
     * machine-wide changes, so a starts it and b ends it.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void hooks_know_which_logic_they_serve(GameTestHelper h) {
        var legacy = MBDScenario.of(h).placeMachine(ISOLATED_LEGACY, POS);
        var counters = ((LegacyHookMachines.Counted) legacy.machine()).counters();
        runBoth(h, legacy);
        check(h, counters.contexts().equals(Map.of("a", 31, "b", 41)), "legacy hook contexts " + counters.contexts());
        check(h, counters.get("notifyRecipeStatusChanged(IDLE->WORKING)") == 1
                && counters.get("notifyRecipeStatusChanged(WORKING->IDLE)") == 1, "the legacy status hook did not follow the machine-wide status");

        var fresh = MBDScenario.of(h).placeMachine(ISOLATED_NEW, STONE_POS);
        var machine = (NewHookMachines.SimpleMachine) fresh.machine();
        runBoth(h, fresh);
        check(h, machine.logics.equals(Map.of("a", 32, "b", 42)), "per-logic hook logics " + machine.logics);
        h.succeed();
    }

    /** A machine class overriding the legacy getters still decides what its primary logic runs, and with what. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_primary_follows_overridden_legacy_getters(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(LEGACY_GETTERS, POS);
        var machine = (LegacyHookMachines.LegacyGettersMachine) s.machine();
        int reads = machine.tableReads;
        s.insertItem(0, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> count(h, m, 1, Items.GRAVEL) == 1, START_BUDGET + 40)
                .check("its logic read the handlers through the overridden getter", m -> machine.tableReads > reads)
                .succeed();
    }

    /** An action node left unwired under a recipe event acts on the logic that fired it, not on the primary. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unwired_logic_nodes_act_on_the_event_logic(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED_SUSPENDER, POS)
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> m.getAnalogOutputSignal() == SUSPENDED_MARKER, START_BUDGET)
                .check("b, whose event it was, suspended", m -> b(m).isSuspend())
                .check("a, the primary, was left alone", m -> logic(m).isIdle())
                .succeed();
    }

    /** Recipe Logic At finds a thread by its address, and Thread Index reads it back. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprints_find_a_logic_by_address(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(THREADS_INDEXER, POS)
                .runTicks(2)
                .check("the third thread's index is on the signal", m -> m.getAnalogOutputSignal() == 2)
                .succeed();
    }

    // endregion

    // region settings per logic

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void runtime_values_switch_one_logic(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED, POS)
                .with(m -> m.getRuntimeValues().set("recipe_logic.b.enable", false))
                .insertItem(0, new ItemStack(Items.STONE))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> count(h, m, 1, Items.DIRT) == 1, START_BUDGET)
                .check("the disabled logic never started", m -> b(m).isIdle() && count(h, m, 2, Items.COBBLESTONE) == 1)
                .with(m -> m.getRuntimeValues().set("recipe_logic.enable", false))
                .with(m -> m.getRuntimeValues().set("recipe_logic.b.enable", true))
                .insertItem(0, new ItemStack(Items.STONE))
                .runUntil(m -> count(h, m, 3, Items.GRAVEL) == 1, START_BUDGET)
                .check("disabling the primary leaves b running and a still", m -> count(h, m, 0, Items.STONE) == 1)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void consuming_after_working_is_per_logic(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(ISOLATED_AFTER, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking() && b(m).isWorking(), START_BUDGET)
                .check("a took its stone at the start", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                .check("b keeps its cobblestone until it finishes", m -> count(h, m, 2, Items.COBBLESTONE) == 1)
                .runUntil(m -> b(m).isIdle(), 40)
                .check("then takes it", m -> MBDTestHelper.readItem(h, m, 2).isEmpty() && count(h, m, 3, Items.GRAVEL) == 1)
                .succeed();
    }

    // endregion

    // region threads

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void threads_take_one_recipe_each_from_shared_inputs(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(THREADS, POS)
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .insertEnergy(100_000)
                .runUntil(m -> working(m) == 2, START_BUDGET)
                .runTicks(3)
                .check("two stones keep exactly two threads busy", m -> working(m) == 2 && MBDTestHelper.readItem(h, m, 0).isEmpty())
                .runUntil(m -> working(m) == 0, 40)
                .check("two dirt", m -> count(h, m, 3, Items.DIRT) == 2)
                .succeed();
    }

    /** However the threads interleave, every stone becomes exactly one dirt and at most three run at once. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void threads_neither_lose_nor_duplicate_inputs(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(THREADS, POS)
                .insertItem(0, new ItemStack(Items.STONE, 7))
                .insertEnergy(100_000);
        int maxWorking = 0;
        for (int tick = 0; tick < 300 && count(h, s.machine(), 3, Items.DIRT) < 7; tick++) {
            s.runTicks(1);
            maxWorking = Math.max(maxWorking, working(s.machine()));
            int stones = count(h, s.machine(), 0, Items.STONE) + count(h, s.machine(), 1, Items.STONE) + count(h, s.machine(), 2, Items.STONE);
            int inFlight = working(s.machine());
            check(h, stones + inFlight + count(h, s.machine(), 3, Items.DIRT) == 7,
                    "stone + running + dirt is not 7 at tick " + tick + ": " + stones + " + " + inFlight + " + dirt");
        }
        check(h, maxWorking == 3, "never had all three threads busy: " + maxWorking);
        s.check("all seven pressed", m -> count(h, m, 3, Items.DIRT) == 7).succeed();
    }

    /** Two threads finishing into room for one: the second waits for room rather than voiding its output. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void threads_share_one_output_without_losing_any(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(THREADS, POS)
                .with(m -> outputOf(m).storage.setStackInSlot(0, new ItemStack(Items.DIRT, 63)))
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .insertEnergy(100_000)
                .runUntil(m -> working(m) == 2, START_BUDGET)
                .runUntil(m -> working(m) == 0, 40)
                .check("one thread finished into the last free spot, the other waits for room", m -> count(h, m, 3, Items.DIRT) == 64
                        && m.getRecipeLogics().stream().filter(RecipeLogic::isWaiting).count() == 1)
                .with(m -> outputOf(m).storage.setStackInSlot(0, ItemStack.EMPTY))
                .runUntil(m -> m.getRecipeLogics().stream().allMatch(RecipeLogic::isIdle), 5)
                .check("and lands its dirt once there is room", m -> count(h, m, 3, Items.DIRT) == 1)
                .succeed();
    }

    /** With energy for one thread only, the first thread gets it and the next waits. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_first_thread_gets_scarce_energy_first(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(THREADS, POS)
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .insertEnergy(100_000)
                .runUntil(m -> working(m) == 2, START_BUDGET)
                // leave one tick of energy for one thread, whenever the two started
                .with(m -> MBDTestHelper.capability(h, m, Capabilities.EnergyStorage.BLOCK).extractEnergy(Integer.MAX_VALUE, false))
                .insertEnergy(10)
                .runTicks(1)
                .check("the first thread got the energy, the second waits", m -> logic(m).isWorking()
                        && m.getRecipeLogics().get(1).isWaiting())
                .check("and it was all used, never overdrawn", m -> energy(h, m) == 0)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void factory_threads_use_only_their_own_slots(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(FACTORY, POS)
                .insertItem(1, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> m.getRecipeLogic("main#1").isWorking(), START_BUDGET)
                .check("thread 0 has no stone of its own", m -> logic(m).isIdle())
                .runUntil(m -> m.getRecipeLogic("main#1").isIdle(), 40)
                .check("thread 1's dirt is in thread 1's output", m -> count(h, m, 3, Items.DIRT) == 1
                        && MBDTestHelper.readItem(h, m, 2).isEmpty())
                .succeed();
    }

    // endregion

    // region persistence

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void each_logic_resumes_its_own_progress_after_a_reload(GameTestHelper h) {
        var progress = new int[2];
        MBDScenario.of(h).placeMachine(ISOLATED, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .runUntil(m -> b(m).isWorking(), START_BUDGET)
                .runTicks(3)
                .with(m -> {
                    progress[0] = logic(m).getProgress();
                    progress[1] = b(m).getProgress();
                })
                .check("the logics are at different progress", m -> progress[0] != progress[1])
                .assertPersistenceRoundTrip()
                .check("both resume where they were", m -> logic(m).isWorking() && b(m).isWorking()
                        && logic(m).getProgress() == progress[0] && b(m).getProgress() == progress[1])
                .runUntil(m -> logic(m).isIdle() && b(m).isIdle(), BOTH_BUDGET)
                .check("both finish", m -> count(h, m, 1, Items.DIRT) == 1 && count(h, m, 3, Items.GRAVEL) == 1)
                .succeed();
    }

    /** The primary keeps the key it always had; the others save under recipeLogics.<name>.<thread>. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void threads_save_under_their_own_keys(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(THREADS, POS)
                .insertItem(0, new ItemStack(Items.STONE, 3))
                .insertEnergy(100_000)
                .runUntil(m -> working(m) == 3, START_BUDGET)
                .runTicks(4);
        var managed = managedTag(h);
        check(h, managed.getCompound("recipeLogic").getString("status").equals("WORKING"), "thread 0 is not under recipeLogic: " + managed);
        var threads = managed.getCompound("recipeLogics").getCompound("main");
        check(h, threads.getCompound("1").getString("status").equals("WORKING")
                && threads.getCompound("2").getString("status").equals("WORKING"), "threads 1 and 2 are not under recipeLogics.main: " + threads);
        check(h, !threads.contains("0"), "thread 0 is saved twice");
        s.assertPersistenceRoundTrip()
                .check("all three resume", m -> working(m) == 3)
                .succeed();
    }

    /** A save missing a logic's state, or holding one the machine no longer has, loads anyway. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_missing_or_unknown_logic_in_a_save_is_harmless(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(THREADS, POS)
                .insertItem(0, new ItemStack(Items.STONE, 3))
                .insertEnergy(100_000)
                .runUntil(m -> working(m) == 3, START_BUDGET)
                .assertPersistenceRoundTrip(tag -> {
                    var logics = tag.getCompound("managed").getCompound("recipeLogics");
                    logics.getCompound("main").remove("2");
                    var ghost = new CompoundTag();
                    ghost.put("0", logics.getCompound("main").getCompound("1").copy());
                    logics.put("ghost", ghost);
                })
                .check("the thread without a saved state starts idle, the others resume",
                        m -> logic(m).isWorking() && m.getRecipeLogic("main#1").isWorking() && m.getRecipeLogic("main#2").isIdle())
                .runUntil(m -> working(m) == 0, 40)
                .check("the two that resumed finish", m -> count(h, m, 3, Items.DIRT) >= 2)
                .succeed();
    }

    /**
     * A definition's recipe logics survive the save its project file goes through — an additional logic switched
     * off keeps its settings too — and a definition saved before they existed loads with none.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void definitions_keep_their_recipe_logics_through_a_save(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        var primary = ConfigRecipeLogicSettings.builder().name("press").threads(3).build();
        primary.additionalRecipeLogics().add(ConfigRecipeLogicSettings.builder()
                .name("smelt").threads(2).recipeType(TYPE_B).recipeDampingValue(5).keepWhenDisabled(true).enable(false).build());
        var tag = primary.serializeNBT(provider);
        var loaded = ConfigRecipeLogicSettings.builder().build();
        loaded.deserializeNBT(provider, tag);
        check(h, loaded.name().equals("press") && loaded.threads() == 3, "the primary's name and threads did not survive: " + tag);
        check(h, loaded.additionalRecipeLogics().size() == 1, "the list did not come back: " + tag);
        var logic = loaded.additionalRecipeLogics().get(0);
        check(h, logic.name().equals("smelt") && logic.threads() == 2 && logic.recipeDampingValue() == 5
                && logic.getRecipeType().getRegistryName().equals(TYPE_B) && !logic.isEnable(),
                "a disabled additional logic lost its settings: " + tag.get("additionalRecipeLogics"));

        tag.remove("name");
        tag.remove("threads");
        tag.remove("additionalRecipeLogics");
        var old = ConfigRecipeLogicSettings.builder().build();
        old.deserializeNBT(provider, tag);
        check(h, old.name().equals(RecipeLogic.PRIMARY_NAME) && old.threads() == 1 && old.additionalRecipeLogics().isEmpty(),
                "an old primary is not main with one thread and nothing beside it");
        h.succeed();
    }

    /** Only logics with a valid name no logic before them took run, and the editor offers no others. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void only_logics_named_validly_and_uniquely_run(GameTestHelper h) throws ReflectiveOperationException {
        var primary = ConfigRecipeLogicSettings.builder().name("a").build();
        for (var name : List.of("b", "B", "b", "a", "c")) {
            primary.additionalRecipeLogics().add(ConfigRecipeLogicSettings.builder().name(name).build());
        }
        var definition = MBDMachineDefinition.builder()
                .id(MBD2.id("multi_logic_names_probe"))
                .recipeLogicSettings(primary)
                .build();
        var runnable = definition.runnableAdditionalRecipeLogics(definition.primaryRecipeLogicName()).stream()
                .map(ConfigRecipeLogicSettings::name).toList();
        check(h, runnable.equals(List.of("b", "c")), "the logics that run: " + runnable);
        var badPrimary = MBDMachineDefinition.builder()
                .id(MBD2.id("multi_logic_bad_primary_probe"))
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().name("Main").build())
                .build();
        check(h, badPrimary.primaryRecipeLogicName().equals(RecipeLogic.PRIMARY_NAME), "an invalid primary name was kept");

        var holder = new ConfigRecipeLogicSettings.AdditionalRecipeLogics();
        var fresh = ConfigRecipeLogicSettings.builder().additionalRecipeLogics(holder).build().additionalRecipeLogics();
        var addDefault = holder.getClass().getDeclaredMethod("defaultLogic");
        addDefault.setAccessible(true);
        fresh.add((ConfigRecipeLogicSettings) addDefault.invoke(holder));
        fresh.add((ConfigRecipeLogicSettings) addDefault.invoke(holder));
        fresh.remove(0);
        var next = (ConfigRecipeLogicSettings) addDefault.invoke(holder);
        var taken = fresh.stream().map(ConfigRecipeLogicSettings::name).toList();
        check(h, !taken.contains(next.name()), "a new entry is named " + next.name() + ", already taken by " + taken);
        h.succeed();
    }

    // endregion

    // region multiblock

    private static final BlockPos PART_X = new BlockPos(2, 1, 1);
    private static final BlockPos PART_Y = new BlockPos(0, 1, 1);

    private static MBDScenario formed(GameTestHelper h, ResourceLocation partAtY) {
        return MBDScenario.of(h)
                .placeMachine(MB_PART, PART_X)
                .placeMachine(partAtY, PART_Y)
                .placeMachine(MB_CONTROLLER, POS)
                .formNow()
                .assertFormed()
                .insertEnergy(100_000);
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void pattern_predicates_give_each_logic_its_parts(GameTestHelper h) {
        formed(h, MB_PART)
                .target(PART_X).insertItem(0, new ItemStack(Items.STONE))
                .target(PART_Y).insertItem(0, new ItemStack(Items.COBBLESTONE))
                .target(POS)
                .runUntil(m -> logic(m).isWorking() && b(m).isWorking(), START_BUDGET)
                .runUntil(m -> logic(m).isIdle() && b(m).isIdle(), BOTH_BUDGET)
                .target(PART_X).check("a used the part matched for it", m -> count(h, m, 1, Items.DIRT) == 1)
                .target(PART_Y).check("b used the part matched for it", m -> count(h, m, 1, Items.GRAVEL) == 1)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_part_matched_for_one_logic_is_closed_to_the_other(GameTestHelper h) {
        formed(h, MB_PART)
                .target(PART_X).insertItem(0, new ItemStack(Items.COBBLESTONE))
                .target(POS)
                .runTicks(60)
                .check("neither logic ran", m -> logic(m).isIdle() && b(m).isIdle())
                .target(PART_X).check("the cobblestone stays in a's part", m -> count(h, m, 0, Items.COBBLESTONE) == 1)
                .succeed();
    }

    /** A part's trait scope and its predicate's scope both apply: a slot scoped to a, on a part matched for b, serves nobody. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void trait_and_predicate_scopes_both_apply(GameTestHelper h) {
        formed(h, MB_PART_FOR_A)
                .target(PART_Y).insertItem(2, new ItemStack(Items.STONE))
                .target(POS)
                .runTicks(60)
                .check("nobody may use the doubly scoped slot", m -> logic(m).isIdle() && b(m).isIdle())
                .target(PART_Y).check("its stone stays", m -> count(h, m, 2, Items.STONE) == 1)
                .insertItem(0, new ItemStack(Items.COBBLESTONE))
                .target(POS)
                .runUntil(m -> b(m).isWorking(), START_BUDGET)
                .succeed();
    }

    /** A part's traits follow its predicate's scope for conditions too, even when no logic gets its handlers. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_predicate_scope_hides_a_parts_traits_from_the_other_logics(GameTestHelper h) {
        var s = MBDScenario.of(h)
                .placeMachine(MB_PART, PART_X)
                .placeMachine(MB_PART, PART_Y)
                .placeMachine(MB_CONTROLLER_SENSOR, POS)
                .formNow()
                .assertFormed();
        var controller = s.machine();
        var sensorTraits = s.target(PART_X).machine().getAdditionalTraits();
        check(h, !sensorTraits.isEmpty(), "the part has no traits");
        var aTraits = controller.getRecipeLogicTraits(controller.getRecipeLogic());
        var bTraits = controller.getRecipeLogicTraits(controller.getRecipeLogic("b"));
        check(h, sensorTraits.stream().noneMatch(aTraits::contains), "a sees the traits of the part matched for b");
        check(h, bTraits.containsAll(sensorTraits), "b does not see the traits of the part matched for it");
        h.succeed();
    }

    /**
     * A part hears each logic's own status changes, and the machine-wide ones on the legacy hook: b starting while a
     * works changes nothing machine-wide, but is still b's change to tell.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void parts_hear_each_logics_own_status_changes(GameTestHelper h) {
        var s = formed(h, MB_STATUS_PART);
        var part = (NewHookMachines.Part) s.target(PART_Y).machine();
        s.target(PART_X).insertItem(0, new ItemStack(Items.STONE))
                .target(POS)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .target(PART_Y).insertItem(0, new ItemStack(Items.COBBLESTONE))
                .target(POS)
                .runUntil(m -> b(m).isWorking(), START_BUDGET)
                .check("a still works when b starts", m -> logic(m).isWorking())
                .runUntil(m -> logic(m).isIdle() && b(m).isIdle(), BOTH_BUDGET);
        var perLogic = part.perLogic.stream().filter(change -> !change.endsWith("IDLE->IDLE")).toList();
        check(h, perLogic.equals(List.of("a:IDLE->WORKING", "b:IDLE->WORKING", "a:WORKING->IDLE", "b:WORKING->IDLE")),
                "per logic: " + part.perLogic);
        check(h, part.perLogic.containsAll(List.of("a:IDLE->IDLE", "b:IDLE->IDLE")), "forming did not tell each logic's status");
        var machineWide = part.machineWide.stream().filter(change -> !change.equals("IDLE->IDLE")).toList();
        check(h, machineWide.equals(List.of("IDLE->WORKING", "WORKING->IDLE")), "machine-wide: " + part.machineWide);
        s.succeed();
    }

    /** Unforming lets go of the parts at once: the controller's logics stop listening to what they no longer use. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unforming_stops_listening_to_the_parts(GameTestHelper h) throws ReflectiveOperationException {
        var s = formed(h, MB_PART).runTicks(12);
        var partInput = (Trait) s.target(PART_X).machine().getTraitByName("item_slot");
        var listeners = Trait.class.getDeclaredField("listeners");
        listeners.setAccessible(true);
        int whileFormed = ((List<?>) listeners.get(partInput)).size();
        s.target(POS)
                .with(m -> h.setBlock(PART_Y, Blocks.AIR))
                .assertNotFormed()
                .runTicks(12);
        int unformed = ((List<?>) listeners.get(partInput)).size();
        check(h, whileFormed > 0 && unformed == 0, "listeners on the part went from " + whileFormed + " to " + unformed);
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void breaking_the_structure_resets_every_logic(GameTestHelper h) {
        formed(h, MB_PART)
                .target(PART_X).insertItem(0, new ItemStack(Items.STONE))
                .target(PART_Y).insertItem(0, new ItemStack(Items.COBBLESTONE))
                .target(POS)
                .runUntil(m -> logic(m).isWorking() && b(m).isWorking(), START_BUDGET)
                .with(m -> h.setBlock(PART_Y, Blocks.AIR))
                .assertNotFormed()
                .check("every logic is reset", m -> m.getRecipeLogics().stream()
                        .allMatch(l -> l.isIdle() && l.getLastRecipe() == null && l.getProgress() == 0))
                .runTicks(40)
                .check("and stays idle unformed", m -> m.getRecipeLogics().stream().allMatch(RecipeLogic::isIdle))
                .succeed();
    }

    // endregion

    // region helpers

    @Nullable
    static RecipeLogic b(MBDMachine machine) {
        return machine.getRecipeLogic("b");
    }

    static int working(MBDMachine machine) {
        return (int) machine.getRecipeLogics().stream().filter(RecipeLogic::isWorking).count();
    }

    static ItemSlotCapabilityTrait outputOf(MBDMachine machine) {
        return (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot_1");
    }

    /**
     * Run a (stone, 20 ticks), then b (cobblestone, 30 ticks) while a still works, to the end: a starts the machine
     * working and b, finishing last, ends it.
     */
    private static void runBoth(GameTestHelper h, MBDScenario s) {
        s.insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .insertItem(2, new ItemStack(Items.COBBLESTONE))
                .runUntil(m -> b(m).isWorking(), START_BUDGET)
                .check("a still works when b starts", m -> logic(m).isWorking())
                .runUntil(m -> logic(m).isIdle() && b(m).isIdle() && count(h, m, 3, Items.GRAVEL) == 1, BOTH_BUDGET);
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) {
            h.fail(message);
        }
    }

    /** An item input handling nothing, that may or may not serve several logics. */
    private record InertItemHandler(boolean shareable) implements IRecipeHandler<SizedIngredient> {
        @Override
        public @Nullable List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left,
                                                                 @Nullable String slotName, boolean simulate) {
            return left;
        }

        @Override
        public RecipeCapability<SizedIngredient> getRecipeCapability() {
            return ItemRecipeCapability.CAP;
        }

        @Override
        public boolean supportsMultipleRecipeLogics() {
            return shareable;
        }
    }

    // endregion
}

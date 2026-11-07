package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.MachineEventRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenFixtures.*;

/**
 * Golden tests: the recipe logic's observable behaviour as of the single-logic implementation, written before
 * the multi-logic rework and expected to pass unchanged after every step of it.
 *
 * <p>What is pinned: the ordered recipe-lifecycle events a machine posts (folded with {@link MachineEventRecorder}),
 * the resources a cycle consumes, how progress damps while waiting, how often each legacy hook an addon may
 * override runs, and the layout of the saved recipe logic — including a save frozen from the old build.</p>
 *
 * <p>Waits poll for an observable instead of budgeting ticks: recipe search may run on a background thread and is
 * only polled every 5 ticks, so the tick a recipe starts on is not fixed. Everything after the start is.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class RecipeLogicGoldenTests {
    static { @SuppressWarnings("unused") var ignored = RecipeLogicGoldenFixtures.MACHINE; }

    static final BlockPos POS = new BlockPos(1, 1, 1);

    /** What one run of {@link #runHookScenario} calls each recipe hook of a simple machine: 2 cycles, 3 waiting ticks. */
    static final Map<String, Integer> SIMPLE_HOOK_COUNTS = Map.ofEntries(
                Map.entry("afterWorking", 2),
                Map.entry("alwaysReSearchRecipe", 2),
                Map.entry("alwaysTryModifyRecipe", 2),
                Map.entry("applyParallel", 1),
                Map.entry("beforeWorking", 2),
                Map.entry("consumeInputsAfterWorking", 2),
                Map.entry("dampingWhenWaiting", 3),
                Map.entry("doModifyRecipe", 1),
                Map.entry("getMaxParallel", 1),
                Map.entry("getModifiedRecipe", 1),
                Map.entry("getRecipeDampingValue", 3),
                Map.entry("notifyRecipeStatusChanged(IDLE->WORKING)", 1),
                Map.entry("notifyRecipeStatusChanged(WAITING->WORKING)", 1),
                Map.entry("notifyRecipeStatusChanged(WORKING->IDLE)", 1),
                Map.entry("notifyRecipeStatusChanged(WORKING->WAITING)", 1),
                Map.entry("onRecipeFinish", 2),
                Map.entry("onWaiting", 3),
                Map.entry("onWorking", 46));

    static final BlockPos PART_POS = new BlockPos(0, 1, 1);
    static final BlockPos STONE_POS = new BlockPos(2, 1, 1);
    static final int START_BUDGET = 200;

    // region single machine

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_basic_cycle(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .check("a fresh recipe starts at progress 0 of 20", m -> logic(m).getProgress() == 0 && logic(m).getDuration() == 20)
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("the stone is consumed", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                    .check("one dirt is produced", m -> count(h, m, 2, Items.DIRT) == 1)
                    .check("20 working ticks at 10 FE/t", m -> energy(h, m) == 100_000 - 200)
                    .check("progress and duration reset", m -> logic(m).getProgress() == 0 && logic(m).getDuration() == 0);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_rerun_last_recipe(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE, 2))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isIdle(), 80)
                    .check("both stones are consumed", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                    .check("two dirt are produced", m -> count(h, m, 2, Items.DIRT) == 2)
                    .check("40 working ticks at 10 FE/t", m -> energy(h, m) == 100_000 - 400);
            // the second cycle re-runs the last recipe straight from the finish: no search, no modify events, and
            // no status change in between
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_waiting_and_damping(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100) // 10 ticks' worth
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isWaiting(), 20)
                    .check("10 ticks worked, then the first waiting tick damped 2", m -> logic(m).getProgress() == 8)
                    .check("waiting on a missing per-tick input", m -> waitingKey(m).equals("mbd2.recipe_logic.insufficient_in"))
                    .runTicks(4)
                    .check("damping stops at 0", m -> logic(m).getProgress() == 0 && logic(m).isWaiting())
                    .insertEnergy(1_000)
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("one dirt is produced", m -> count(h, m, 2, Items.DIRT) == 1)
                    .check("the damped progress had to be worked again: 30 working ticks in total", m -> energy(h, m) == 1_000 - 200)
                    .check("the waiting reason is cleared", m -> logic(m).getWaitingReason() == null);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×10",
                    "StateChanged(working->waiting)",
                    "RecipeStatusChanged(WORKING->WAITING)",
                    "OnRecipeWaiting(golden_press) ×5",
                    "StateChanged(waiting->working)",
                    "RecipeStatusChanged(WAITING->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_interrupt(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(5)
                    .with(m -> logic(m).interruptRecipe())
                    .check("interrupting goes idle at once", m -> logic(m).isIdle() && logic(m).getProgress() == 0)
                    .runTicks(10)
                    .check("the consumed stone is gone for good", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                    .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 2).isEmpty())
                    .check("only the 5 worked ticks used energy", m -> energy(h, m) == 100_000 - 50);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×5",
                    "AfterRecipeWorking(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_reset(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(5)
                    .with(m -> logic(m).resetRecipeLogic())
                    .check("reset forgets the recipe", m -> logic(m).isIdle() && logic(m).getLastRecipe() == null
                            && logic(m).getLastOriginRecipe() == null && logic(m).getProgress() == 0)
                    .runTicks(10)
                    .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 2).isEmpty());
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×5",
                    "AfterRecipeWorking(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_suspend_resume(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(5)
                    .with(m -> logic(m).setWorkingEnabled(false))
                    .check("suspended", m -> logic(m).isSuspend() && logic(m).isActive())
                    .runTicks(10)
                    .check("a suspended recipe keeps its progress", m -> logic(m).getProgress() == 5)
                    .with(m -> logic(m).setWorkingEnabled(true))
                    .check("resuming goes straight back to working", m -> logic(m).isWorking())
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("one dirt is produced", m -> count(h, m, 2, Items.DIRT) == 1)
                    .check("suspended ticks use no energy", m -> energy(h, m) == 100_000 - 200);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×5",
                    "StateChanged(working->suspend)",
                    "RecipeStatusChanged(WORKING->SUSPEND)",
                    "StateChanged(suspend->working)",
                    "RecipeStatusChanged(SUSPEND->WORKING)",
                    "OnRecipeWorking(golden_press) ×15",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_suspend_idle_machine(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.with(m -> logic(m).setWorkingEnabled(false))
                    .check("suspended without a recipe", m -> logic(m).isSuspend() && !logic(m).isActive())
                    .insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runTicks(30)
                    .check("a suspended machine does not start", m -> logic(m).isSuspend() && count(h, m, 0, Items.STONE) == 1)
                    .with(m -> logic(m).setWorkingEnabled(true))
                    .check("resuming without a recipe goes idle", m -> logic(m).isIdle())
                    .runUntil(m -> count(h, m, 2, Items.DIRT) == 1, START_BUDGET);
            trace.assertTrace(h,
                    "StateChanged(base->suspend)",
                    "RecipeStatusChanged(IDLE->SUSPEND)",
                    "StateChanged(suspend->base)",
                    "RecipeStatusChanged(SUSPEND->IDLE)",
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_consume_inputs_after_working(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(AFTER_MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.COBBLESTONE))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .check("consume-after is latched on the logic", m -> logic(m).isConsumeInputsAfterWorking())
                    .runTicks(5)
                    .check("the input stays until the recipe finishes", m -> count(h, m, 0, Items.COBBLESTONE) == 1)
                    .runUntil(m -> logic(m).isIdle(), 20)
                    .check("the input is taken at the finish", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                    .check("one gravel is produced", m -> count(h, m, 2, Items.GRAVEL) == 1)
                    .check("the latch is cleared", m -> !logic(m).isConsumeInputsAfterWorking());
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_after)",
                    "RecipeModifyAfter(golden_after)",
                    "BeforeRecipeWorking(golden_after)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_after) ×10",
                    "AfterRecipeWorking(golden_after)",
                    "OnConsumeInputsAfterWorking(golden_after)",
                    "OnRecipeFinish(golden_after)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_consume_after_input_taken_interrupts(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(AFTER_MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.COBBLESTONE))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(3)
                    .with(m -> MBDTestHelper.extractItem(h, m, 0, 1))
                    .runTicks(5)
                    .check("losing the input interrupts the recipe", m -> logic(m).isIdle() && logic(m).getProgress() == 0)
                    .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 2).isEmpty());
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_after)",
                    "RecipeModifyAfter(golden_after)",
                    "BeforeRecipeWorking(golden_after)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_after) ×3",
                    "AfterRecipeWorking(golden_after)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    // endregion

    // region fuel

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_fuel(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(FUEL_MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.SAND))
                    .insertItem(1, new ItemStack(Items.COAL))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .check("the coal is burnt at the start", m -> MBDTestHelper.readItem(h, m, 1).isEmpty()
                            && logic(m).getFuelMaxTime() == 30 && logic(m).getLastFuelRecipe() != null)
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("one glass is produced", m -> count(h, m, 2, Items.GLASS) == 1)
                    .check("the coal outlasts the recipe", m -> logic(m).getFuelTime() > 0)
                    .runUntil(m -> logic(m).getFuelTime() == 0, 40);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_smelt)",
                    "RecipeModifyAfter(golden_smelt)",
                    "FuelRecipeModify(golden_coal)",
                    "BeforeRecipeWorking(golden_smelt)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_smelt) ×20",
                    "AfterRecipeWorking(golden_smelt)",
                    "OnRecipeFinish(golden_smelt)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)",
                    "FuelBurningFinish(golden_coal)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_fuel_runs_out_mid_recipe(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(FUEL_MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.SAND, 2))
                    .insertItem(1, new ItemStack(Items.COAL))
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isWaiting(), 60)
                    .check("waiting on fuel", m -> waitingKey(m).equals("mbd2.recipe_logic.insufficient_fuel"))
                    .check("the second smelt had worked 9 ticks, then damped 2", m -> logic(m).getProgress() == 7)
                    .runTicks(2)
                    .insertItem(1, new ItemStack(Items.COAL))
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("two glass are produced", m -> count(h, m, 2, Items.GLASS) == 2);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_smelt)",
                    "RecipeModifyAfter(golden_smelt)",
                    "FuelRecipeModify(golden_coal)",
                    "BeforeRecipeWorking(golden_smelt)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_smelt) ×20",
                    "AfterRecipeWorking(golden_smelt)",
                    "OnRecipeFinish(golden_smelt)",
                    "BeforeRecipeWorking(golden_smelt)",
                    "OnRecipeWorking(golden_smelt) ×9",
                    "FuelBurningFinish(golden_coal)",
                    "StateChanged(working->waiting)",
                    "RecipeStatusChanged(WORKING->WAITING)",
                    "OnRecipeWaiting(golden_smelt) ×3",
                    "FuelRecipeModify(golden_coal)",
                    "StateChanged(waiting->working)",
                    "RecipeStatusChanged(WAITING->WORKING)",
                    "OnRecipeWorking(golden_smelt) ×17",
                    "AfterRecipeWorking(golden_smelt)",
                    "OnRecipeFinish(golden_smelt)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    // endregion

    // region multiblock

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_multiblock_cycle(GameTestHelper h) {
        var s = MBDScenario.of(h)
                .placeMachine(MB_PART, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .placeMachine(MB_CONTROLLER, POS);
        try (var trace = MachineEventRecorder.start(h).watch(h, POS, "ctrl").watch(h, PART_POS, "part")) {
            s.formNow()
                    .assertFormed()
                    .target(PART_POS)
                    .insertItem(0, new ItemStack(Items.ANDESITE))
                    .target(POS)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isIdle(), 30)
                    .target(PART_POS)
                    .check("the part's input is consumed", m -> MBDTestHelper.readItem(h, m, 0).isEmpty())
                    .check("one diorite lands in the part", m -> count(h, m, 1, Items.DIORITE) == 1);
            trace.assertTrace(h,
                    "ctrl:StateChanged(base->formed)",
                    "part:StateChanged(base->formed)",
                    "ctrl:StructureFormed",
                    "ctrl:RecipeStatusChanged(IDLE->IDLE)",
                    "ctrl:RecipeModifyBefore(golden_mb)",
                    "ctrl:RecipeModifyAfter(golden_mb)",
                    "ctrl:BeforeRecipeWorking(golden_mb)",
                    "part:StateChanged(formed->working)",
                    "ctrl:StateChanged(formed->working)",
                    "ctrl:RecipeStatusChanged(IDLE->WORKING)",
                    "ctrl:OnRecipeWorking(golden_mb) ×15",
                    "ctrl:AfterRecipeWorking(golden_mb)",
                    "ctrl:OnRecipeFinish(golden_mb)",
                    "part:StateChanged(working->formed)",
                    "ctrl:StateChanged(working->formed)",
                    "ctrl:RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_multiblock_breaks_while_working(GameTestHelper h) {
        var s = MBDScenario.of(h)
                .placeMachine(MB_PART, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .placeMachine(MB_CONTROLLER, POS)
                .formNow()
                .assertFormed()
                .target(PART_POS)
                .insertItem(0, new ItemStack(Items.ANDESITE))
                .target(POS)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(3);
        try (var trace = MachineEventRecorder.start(h).watch(h, POS, "ctrl").watch(h, PART_POS, "part")) {
            h.setBlock(STONE_POS, Blocks.AIR);
            s.assertNotFormed()
                    .check("breaking the structure resets the logic", m -> logic(m).isIdle()
                            && logic(m).getLastRecipe() == null && logic(m).getProgress() == 0)
                    .runTicks(20)
                    .target(PART_POS)
                    .check("nothing is produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty());
            trace.assertTrace(h,
                    "ctrl:StateChanged(working->base)",
                    "ctrl:AfterRecipeWorking(golden_mb)",
                    "part:StateChanged(working->formed)",
                    "ctrl:RecipeStatusChanged(WORKING->IDLE)",
                    "part:StateChanged(formed->base)",
                    "ctrl:StructureInvalid");
        }
        s.succeed();
    }

    // endregion

    // region persistence

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_persistence_resumes_mid_recipe(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(7)
                    .assertPersistenceRoundTrip()
                    .check("the reloaded logic is mid-recipe", m -> logic(m).isWorking() && logic(m).getProgress() == 7
                            && logic(m).getDuration() == 20 && logic(m).getLastRecipe() != null
                            && MBD2.id(PRESS).equals(logic(m).getLastOriginRecipe()))
                    .runUntil(m -> logic(m).isIdle(), 40)
                    .check("one dirt is produced", m -> count(h, m, 2, Items.DIRT) == 1)
                    .check("20 working ticks in total", m -> energy(h, m) == 100_000 - 200);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_world_load_resumes_mid_recipe(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(7)
                .assertWorldLoadRoundTrip()
                .check("the reloaded logic is mid-recipe", m -> logic(m).isWorking() && logic(m).getProgress() == 7)
                .runUntil(m -> logic(m).isIdle(), 40)
                .check("one dirt is produced", m -> count(h, m, 2, Items.DIRT) == 1)
                .succeed();
    }

    /**
     * Where the recipe logic lives in a save. Old saves must keep loading into the logic every machine has
     * (`managed.recipeLogic`), and a save written later must stay readable by an older build.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_save_layout(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(7);
        var managed = managedTag(h);
        var logicTag = managed.getCompound("recipeLogic");
        var keys = new TreeSet<>(logicTag.getAllKeys());
        var expected = new TreeSet<>(List.of("consumeInputsAfterWorking", "duration", "fuelMaxTime",
                "fuelTime", "lastFuelRecipe", "lastOriginRecipe", "lastRecipe", "progress", "status",
                "totalContinuousRunningTime", "waitingReason"));
        if (!keys.equals(expected)) {
            h.fail("managed.recipeLogic keys changed\n  expected: " + expected + "\n  actual:   " + keys + "\n  tag: " + logicTag);
        }
        if (logicTag.getInt("progress") != 7 || logicTag.getInt("duration") != 20) {
            h.fail("progress/duration not saved as plain ints: " + logicTag);
        }
        if (!MBD2.id(PRESS).toString().equals(logicTag.getString("lastOriginRecipe"))) {
            h.fail("lastOriginRecipe not saved as the recipe id: " + logicTag);
        }
        s.succeed();
    }

    /**
     * The {@code managed} tag a {@link RecipeLogicGoldenFixtures#MACHINE} mid-recipe saved before the multi-logic
     * rework (progress 7 of 20, 1 stone already consumed, 99930 FE). Captured once, never regenerated: it is the
     * old format, by definition.
     */
    private static final String FROZEN_MANAGED = """
            {analogOutputSignal:0b,customData:{},customName:"_NULL_",dynamicMachineLevel:-1,machineState:"working",outputDirectSignal:[B;0B,0B,0B,0B,0B,0B],outputSignal:[B;0B,0B,0B,0B,0B,0B],recipeLogic:{consumeInputsAfterWorking:0b,duration:20,fuelMaxTime:0,fuelTime:0,lastFuelRecipe:"_NULL_",lastOriginRecipe:"mbd2:golden_press",lastRecipe:{duration:20,id:"mbd2:golden_press",inputs:{forge_energy:[{content:10,perTick:1b}],item:[{content:{count:1,item:"minecraft:stone"}}]},outputs:{item:[{content:{count:1,item:"minecraft:dirt"}}]},priority:0,type:"mbd2:mbd_recipe_serializer"},progress:7,status:"WORKING",totalContinuousRunningTime:7L,waitingReason:"_NULL_"},runtimeValues:{},trait:{forge_energy_storage:{runtimeValues:{},storage:99930},item_slot:{runtimeValues:{},storage:{Items:[],Size:2}},item_slot_1:{runtimeValues:{},storage:{Items:[],Size:2}}}}
            """;

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_frozen_save_resumes(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        if (FROZEN_MANAGED.isBlank()) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(7);
            h.fail("no frozen save yet, capture this one: " + managedTag(h));
            return;
        }
        CompoundTag frozen;
        try {
            frozen = TagParser.parseTag(FROZEN_MANAGED);
        } catch (Exception e) {
            h.fail("the frozen save does not parse: " + e);
            return;
        }
        s.assertPersistenceRoundTrip(tag -> tag.put("managed", frozen.copy()))
                .check("the frozen logic is mid-recipe", m -> logic(m).isWorking() && logic(m).getProgress() == 7
                        && logic(m).getDuration() == 20 && MBD2.id(PRESS).equals(logic(m).getLastOriginRecipe()))
                .check("the frozen energy is back", m -> energy(h, m) == 100_000 - 70)
                .runUntil(m -> logic(m).isIdle(), 40)
                .check("the frozen recipe finishes", m -> count(h, m, 2, Items.DIRT) == 1)
                .check("13 more working ticks", m -> energy(h, m) == 100_000 - 200)
                .succeed();
    }

    // endregion

    // region blueprint

    /**
     * A blueprint saved before the recipe event nodes grew a {@code recipeLogic} port still loads and runs: every
     * event fires, every wire holds (a broken {@code recipe} wire would null the recipe and nothing would run).
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_frozen_blueprint_still_runs(GameTestHelper h) {
        if (FROZEN_BLUEPRINT.isBlank()) {
            h.fail("no frozen blueprint yet, capture this one: " + frozenBlueprint());
            return;
        }
        MBDScenario.of(h).placeMachine(BLUEPRINT_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .check("Before Recipe Working set its marker", m -> m.getAnalogOutputSignal() == STARTED_MARKER)
                .runUntil(m -> logic(m).isIdle(), 40)
                .check("On Recipe Finish set its marker", m -> m.getAnalogOutputSignal() == FINISHED_MARKER)
                .check("Set Event Recipe passed the recipe through", m -> count(h, m, 2, Items.DIRT) == 1)
                .succeed();
    }

    // endregion

    // region legacy hooks

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_legacy_hooks_simple(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(LEGACY_MACHINE, POS);
        var counters = ((LegacyHookMachines.Counted) s.machine()).counters();
        // a legacy override calling super still reaches MBD2's own behaviour: the trace is the plain machine's
        runHookScenario(h, s);
        assertCounts(h, "simple machine", counters.snapshot(), SIMPLE_HOOK_COUNTS);
        s.succeed();
    }

    /**
     * Two stones, 100 FE: the first recipe works 10 ticks, waits 3, gets 1000 FE, finishes, and the second runs
     * straight from the finish. Asserts the event trace of a simple {@link RecipeLogicGoldenFixtures#RECIPE_TYPE}
     * machine; the hook calls it makes are {@link #SIMPLE_HOOK_COUNTS}.
     */
    static void runHookScenario(GameTestHelper h, MBDScenario s) {
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE, 2))
                    .insertEnergy(100)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runUntil(m -> logic(m).isWaiting(), 20)
                    .runTicks(2)
                    .insertEnergy(1_000)
                    .runUntil(m -> logic(m).isIdle(), 80)
                    .check("two dirt are produced", m -> count(h, m, 2, Items.DIRT) == 2);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×10",
                    "StateChanged(working->waiting)",
                    "RecipeStatusChanged(WORKING->WAITING)",
                    "OnRecipeWaiting(golden_press) ×3",
                    "StateChanged(waiting->working)",
                    "RecipeStatusChanged(WAITING->WORKING)",
                    // three waiting ticks damped 10 down to 4
                    "OnRecipeWorking(golden_press) ×16",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "OnRecipeWorking(golden_press) ×20",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(working->base)",
                    "RecipeStatusChanged(WORKING->IDLE)");
        }
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void golden_legacy_hooks_multiblock(GameTestHelper h) {
        var s = MBDScenario.of(h)
                .placeMachine(LEGACY_PART, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .placeMachine(LEGACY_CONTROLLER, POS);
        var controller = ((LegacyHookMachines.Counted) s.machine()).counters();
        var part = ((LegacyHookMachines.Counted) MBDTestHelper.getMachine(h, PART_POS)).counters();
        s.formNow()
                .assertFormed()
                .target(PART_POS)
                .insertItem(0, new ItemStack(Items.ANDESITE))
                .target(POS)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runUntil(m -> logic(m).isIdle(), 30)
                .target(PART_POS)
                .check("one diorite lands in the part", m -> count(h, m, 1, Items.DIORITE) == 1);
        assertCounts(h, "controller", controller.snapshot(), Map.ofEntries(
                Map.entry("afterWorking", 1),
                Map.entry("alwaysTryModifyRecipe", 1),
                Map.entry("beforeWorking", 1),
                Map.entry("consumeInputsAfterWorking", 1),
                Map.entry("doModifyRecipe", 1),
                Map.entry("getMaxParallel", 1),
                Map.entry("getModifiedRecipe", 1),
                Map.entry("notifyRecipeStatusChanged(IDLE->IDLE)", 1),
                Map.entry("notifyRecipeStatusChanged(IDLE->WORKING)", 1),
                Map.entry("notifyRecipeStatusChanged(WORKING->IDLE)", 1),
                Map.entry("onRecipeFinish", 1),
                Map.entry("onWorking", 15)));
        assertCounts(h, "part", part.snapshot(), Map.ofEntries(
                Map.entry("afterControllerWorking", 1),
                Map.entry("alwaysTryModifyControllerRecipe", 1),
                Map.entry("beforeControllerWorking", 1),
                Map.entry("getMaxControllerParallel", 1),
                Map.entry("modifyControllerRecipe", 1),
                Map.entry("notifyControllerRecipeStatusChanged(IDLE->IDLE)", 1),
                Map.entry("notifyControllerRecipeStatusChanged(IDLE->WORKING)", 1),
                Map.entry("notifyControllerRecipeStatusChanged(WORKING->IDLE)", 1),
                Map.entry("onControllerWorking", 15)));
        s.succeed();
    }

    // endregion

    // region helpers

    static RecipeLogic logic(MBDMachine machine) {
        return machine.getRecipeLogic();
    }

    static int count(GameTestHelper h, MBDMachine machine, int slot, Item item) {
        var stack = MBDTestHelper.readItem(h, machine, slot);
        return stack.is(item) ? stack.getCount() : 0;
    }

    static int energy(GameTestHelper h, MBDMachine machine) {
        var storage = MBDTestHelper.capability(h, machine, Capabilities.EnergyStorage.BLOCK);
        return storage == null ? -1 : storage.getEnergyStored();
    }

    static String waitingKey(MBDMachine machine) {
        var reason = logic(machine).getWaitingReason();
        return reason != null && reason.getContents() instanceof TranslatableContents contents ? contents.getKey() : "<none>";
    }

    static CompoundTag managedTag(GameTestHelper h) {
        var be = h.getLevel().getBlockEntity(h.absolutePos(POS));
        if (be == null) {
            h.fail("no block entity at " + POS);
            throw new AssertionError();
        }
        return be.saveWithFullMetadata(h.getLevel().registryAccess()).getCompound("managed");
    }

    static void assertCounts(GameTestHelper h, String what, Map<String, Integer> actual, Map<String, Integer> expected) {
        var sortedExpected = new TreeMap<>(expected);
        if (!sortedExpected.equals(actual)) {
            h.fail(what + " hook counts differ\n  expected: " + sortedExpected + "\n  actual:   " + actual);
        }
    }

    // endregion
}

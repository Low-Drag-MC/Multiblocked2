package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end tests for the machine blueprint system.
 *
 * <p>Each test is a pair: the same machine with and without the blueprint, so a passing assertion
 * cannot be explained by the recipe simply not running. The control ({@link #plainMachineRunsRecipe})
 * is what makes the three negative assertions mean anything.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class BlueprintTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintFixtures.PLAIN_MACHINE_ID; }

    /** Control: no blueprint, so the recipe runs normally. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void plainMachineRunsRecipe(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.PLAIN_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80) // 20-tick recipe; wide slack because the recipe search is async and polls every 5 ticks
                .assertItem(1, BlueprintFixtures.dirt(1))
                .succeed();
    }

    /**
     * A blueprint hooking {@code Machine Tick} and cancelling it stops the machine ticking at all, so
     * the recipe never progresses. Proves the whole chain end to end: the binding resolved, the entry
     * node was indexed for {@code MachineTickEvent}, the exec flow reached {@code Cancel Event}, and
     * the cancel was visible to {@code MBDMachine.serverTick}.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void cancellingBlueprintStopsRecipe(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.CANCELLING_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItem(1, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * The parameterised blueprint with {@code shouldCancel} left at the variable's declared default of
     * false: the branch takes the other path, nothing cancels, the recipe runs.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void parameterDefaultLetsRecipeRun(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.PARAM_OFF_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItem(1, BlueprintFixtures.dirt(1))
                .succeed();
    }

    /**
     * The same blueprint whose binding overrides {@code shouldCancel} to true. Same graph bytes, same
     * machine shape — only the binding's stored parameter differs, which is the whole claim the
     * exposed-variable design makes.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void parameterOverrideStopsRecipe(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.PARAM_ON_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItem(1, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * A blueprint reaching a machine's traits: it extracts from the input slot and inserts into the
     * output slot every tick. The machine has no recipe type, so nothing but the blueprint could have
     * moved the items.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintMovesItemsBetweenTraits(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.TRANSFER_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(5)
                .assertItem(0, ItemStack.EMPTY)
                .assertItem(1, BlueprintFixtures.stone(4))
                .succeed();
    }

    /**
     * A blueprint gated on {@code Every N Ticks} fires periodically — not never, and not every tick.
     *
     * <h2>What this guards</h2>
     * A periodic gate does not fail by drifting, it fails by freezing: anything that stops the
     * remainder moving — a conversion lost on the way into the arithmetic, a timer too large for the
     * lane it ends up in — leaves the gate answering the same thing on every tick. Which value it
     * froze on then decides whether the machine does the work every single tick or on none of them,
     * so the assertion is that the count sits strictly between those two. Neither failure can
     * satisfy that, and being a tick out either way does not trip it.
     *
     * <p>The bounds are loose on purpose: the exact count depends on where in the interval the
     * machine's phase falls and on how many ticks the harness runs around placement, neither of which
     * this test is about.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void periodicBlueprintFiresOnItsInterval(GameTestHelper helper) {
        int ticks = 10 * BlueprintFixtures.PERIOD;
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintFixtures.PERIODIC_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(ticks);
        int count = scenario.getItem(0).getCount();
        int expected = ticks / BlueprintFixtures.PERIOD;
        if (count < expected - 2 || count > expected + 2) {
            helper.fail("Every N Ticks fired " + count + " time(s) over " + ticks
                    + " ticks; expected about " + expected
                    + (count == 0 ? " (never fired — the gate is stuck false)" : "")
                    + (count >= ticks ? " (fired every tick — the gate is stuck true)" : ""));
            return;
        }
        helper.succeed();
    }

    /**
     * The machine timer survives a trip through a {@code float}.
     *
     * <p>The arithmetic nodes take their lane from their operands, so a graph doing whole-number math
     * on the timer keeps it exact. The nodes that are genuinely float — {@code Lerp}, {@code Remap},
     * trig, anything feeding a renderer — cannot, and a timer they cannot tell apart from the next
     * one stops moving silently, because the math still runs and still produces a number. It used to
     * be too large for that, because the per-machine offset was a full {@code nextLong()}. Asserting
     * that {@code timer} and {@code timer + 1} are still different floats is the smallest statement
     * of what {@link IMachineBlockEntity#randomTickOffset()} has to keep true.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void machineTimerSurvivesAFloat(GameTestHelper helper) {
        var machine = MBDTestHelper.placeMachine(helper, BlueprintFixtures.PLAIN_MACHINE_ID, new BlockPos(1, 1, 1));
        long timer = machine.getOffsetTimer();
        if ((float) timer == (float) (timer + 1)) {
            helper.fail("offset timer " + timer + " is too large for a float — consecutive ticks "
                    + "round to " + (float) timer + ", so any float-lane math on it is frozen");
            return;
        }
        helper.succeed();
    }
}

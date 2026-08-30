package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import static com.lowdragmc.mbd2.test.tests.blueprint.BuiltinBlueprintBehaviourFixtures.*;

/**
 * Do the shipped built-in blueprints actually do what their notes say?
 *
 * <h2>Why these are separate from {@link BuiltinBlueprintTests}</h2>
 * That class asks whether each built-in <em>exists</em> — builds, round-trips, exposes its parameters.
 * All of that can be true of a blueprint that is wired wrongly and does nothing. These place a real
 * machine, run it, and read the outcome, which is the only kind of test that can tell a graph that
 * computes the right number from one whose result never reaches the machine.
 *
 * <h2>Every claim has a control</h2>
 * "The machine produced dirt" is not evidence that redstone control works — an unbound machine also
 * produces dirt. So each behaviour is asserted as a <em>difference</em>: powered against unpowered,
 * overclocked against the same machine at the same tier with no blueprint, chance 1 against chance 0.
 * A blueprint that silently stopped running would break the pair, not just one side of it.
 *
 * <h2>Durations, not throughput</h2>
 * The recipe-modifying blueprints are asserted on {@code getMaxProgress()} once the machine is working,
 * rather than on how much it produced in N ticks. Recipe search is asynchronous and polled every few
 * ticks, so a throughput count is timing-sensitive in a way that has nothing to do with the blueprint
 * — see the note in {@code BlueprintTests}. The duration is the number the blueprint computes, and it
 * is exact.
 */
@GameTestHolder(MBD2.MOD_ID)
public class BuiltinBlueprintBehaviourTests {
    static { @SuppressWarnings("unused") var ignored = BuiltinBlueprintBehaviourFixtures.RECIPE_TYPE_ID; }

    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    /** Long enough for the async recipe search to land and a 20-tick recipe to finish twice over. */
    private static final int RUN_TICKS = 60;

    // ---- redstone_control --------------------------------------------------------------------

    /** Unpowered, at the defaults, the machine runs: the blueprint enables it rather than doing nothing. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void redstoneControlLetsAnUnpoweredMachineRun(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(REDSTONE_DEFAULT_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .succeed();
    }

    /**
     * Powered, at the defaults, the machine stops.
     *
     * <p>The redstone block goes in <em>before</em> the stone, so the machine is disabled for the whole
     * run: this asserts that nothing was produced, and a machine that was allowed to start and then
     * stopped would have produced its first dirt already.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void redstoneControlStopsAPoweredMachine(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(REDSTONE_DEFAULT_ID, MACHINE)
                .withNeighbor(Direction.NORTH, Blocks.REDSTONE_BLOCK.defaultBlockState())
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS);
        var produced = scenario.getItem(1);
        if (!produced.isEmpty()) {
            helper.fail("a powered machine still ran: output slot holds " + produced);
            return;
        }
        scenario.succeed();
    }

    /**
     * With {@code requiresSignal} overridden the polarity flips — powered runs, unpowered does not.
     *
     * <p>Both halves in one test because either alone is satisfiable by a blueprint that simply stopped
     * working: "unpowered does nothing" is what a broken blueprint that always disables looks like.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void requiresSignalInvertsRedstoneControl(GameTestHelper helper) {
        var unpowered = MBDScenario.of(helper)
                .placeMachine(REDSTONE_REQUIRES_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS);
        var idle = unpowered.getItem(1);
        if (!idle.isEmpty()) {
            helper.fail("requiresSignal=true ran without a signal: output slot holds " + idle);
            return;
        }

        var powered = MBDScenario.of(helper)
                .placeMachine(REDSTONE_REQUIRES_ID, new BlockPos(3, 1, 1))
                .withNeighbor(Direction.NORTH, Blocks.REDSTONE_BLOCK.defaultBlockState())
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS);
        powered.assertItemCountAtLeast(1, Items.DIRT, 1).succeed();
    }

    // ---- comparator_progress -----------------------------------------------------------------

    /**
     * An idle machine reads 0, and the same blueprint with {@code invert} reads 15.
     *
     * <p>The two endpoints of the mapping, which is where an off-by-one in the Remap would show: a
     * blueprint that emitted {@code progress * 15} without the remap also reads 0 when idle, so the
     * inverted machine reading exactly 15 is the half that pins it.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void comparatorProgressReportsTheIdleEndpoints(GameTestHelper helper) {
        var plain = MBDScenario.of(helper)
                .placeMachine(COMPARATOR_ID, MACHINE)
                .runTicks(10);
        if (plain.machine().getAnalogOutputSignal() != 0) {
            helper.fail("an idle machine reported " + plain.machine().getAnalogOutputSignal()
                    + ", expected 0");
            return;
        }

        var inverted = MBDScenario.of(helper)
                .placeMachine(COMPARATOR_INVERTED_ID, new BlockPos(3, 1, 1))
                .runTicks(10);
        if (inverted.machine().getAnalogOutputSignal() != 15) {
            helper.fail("an idle machine with invert=true reported "
                    + inverted.machine().getAnalogOutputSignal() + ", expected 15");
            return;
        }
        inverted.succeed();
    }

    /** A working machine reports a signal that is neither of the endpoints — progress really is read. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void comparatorProgressRisesWhileWorking(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(COMPARATOR_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 16))
                // Part-way through a 20-tick recipe: far enough in that the signal has left 0, not so
                // far that it has reached 15 and become indistinguishable from a stuck maximum.
                .runUntil(machine -> machine.getRecipeLogic().getProgress() >= DURATION / 2, RUN_TICKS);
        var signal = scenario.machine().getAnalogOutputSignal();
        if (signal <= 0 || signal >= 15) {
            helper.fail("a half-finished recipe reported " + signal + ", expected between 1 and 14");
            return;
        }
        scenario.succeed();
    }

    // ---- overclock ---------------------------------------------------------------------------

    /**
     * A tier-2 machine runs the recipe in a quarter of its authored duration, and the same machine
     * without the blueprint does not.
     *
     * <p>The exact number is the point: {@code 20 / 2^2 = 5}. An overclock that applied once instead of
     * per tier gives 10, one that used the tier as a multiplier rather than an exponent gives 10 as
     * well, and neither is distinguishable from "faster".</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void overclockDividesTheDurationPerTier(GameTestHelper helper) {
        var control = workingMachine(helper, OVERCLOCK_CONTROL_ID, MACHINE);
        if (control == null) return;
        if (control.machine().getRecipeLogic().getMaxProgress() != DURATION) {
            helper.fail("the control machine's duration was "
                    + control.machine().getRecipeLogic().getMaxProgress() + ", expected " + DURATION);
            return;
        }

        var overclocked = workingMachine(helper, OVERCLOCK_ID, new BlockPos(3, 1, 1));
        if (overclocked == null) return;
        var duration = overclocked.machine().getRecipeLogic().getMaxProgress();
        if (duration != OVERCLOCKED_DURATION) {
            helper.fail("a tier-" + OVERCLOCK_TIER + " overclocked machine ran a " + duration
                    + "-tick recipe, expected " + OVERCLOCKED_DURATION);
            return;
        }
        overclocked.succeed();
    }

    // ---- part_count_bonus --------------------------------------------------------------------

    /**
     * A formed multiblock with two parts halves its duration: {@code 20 / (1 + 2 * 0.5)}.
     *
     * <p>Only the formed case is asserted. An unformed controller does not run a recipe at all, so
     * "no bonus when unformed" has nothing to read — the blueprint's own zero-part behaviour is
     * covered by the fact that it is bound to a machine that is otherwise a plain processor.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void partCountBonusScalesWithTheStructure(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(PART_BONUS_CONTROLLER_ID, MACHINE)
                .placeMachine(PART_BONUS_PART_ID, new BlockPos(0, 1, 1))
                .placeMachine(PART_BONUS_PART_ID, new BlockPos(2, 1, 1))
                .target(MACHINE)
                .formNow()
                .assertFormed()
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        if (!scenario.machine().getRecipeLogic().isWorking()) {
            helper.fail("the formed multiblock never started a recipe");
            return;
        }
        var duration = scenario.machine().getRecipeLogic().getMaxProgress();
        if (duration != PART_BONUS_DURATION) {
            helper.fail("a " + PART_COUNT + "-part structure ran a " + duration
                    + "-tick recipe, expected " + PART_BONUS_DURATION);
            return;
        }
        scenario.succeed();
    }

    // ---- chance_output -----------------------------------------------------------------------

    /**
     * At {@code chance = 1} the bonus is always given; at {@code chance = 0} it never is.
     *
     * <p>Both ends, because the roll is the one thing here that cannot be observed directly: a
     * blueprint that ignored {@code chance} and always inserted would pass the first half alone.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void chanceOutputHonoursItsChance(GameTestHelper helper) {
        var always = MBDScenario.of(helper)
                .placeMachine(CHANCE_ALWAYS_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS);
        // The bonus lands in the same output trait as the recipe's own dirt, and an ItemStackHandler
        // will not stack two different items in one slot — so a one-slot output means the dirt is
        // there and the nugget was rejected, or the other way round. Asserting the count of the bonus
        // across the whole handler is what actually says "the insert happened".
        if (countInOutput(always, Items.GOLD_NUGGET) == 0) {
            helper.fail("chance=1 gave no bonus; output holds " + always.getItem(1) + " / " + always.getItem(2));
            return;
        }

        var never = MBDScenario.of(helper)
                .placeMachine(CHANCE_NEVER_ID, new BlockPos(3, 1, 1))
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runTicks(RUN_TICKS);
        if (countInOutput(never, Items.GOLD_NUGGET) != 0) {
            helper.fail("chance=0 still gave a bonus; output holds " + never.getItem(1) + " / " + never.getItem(2));
            return;
        }
        // The control that says the machine ran at all — without it, "no bonus" is also what a
        // machine that never completed a recipe looks like.
        never.assertItemCountAtLeast(1, Items.DIRT, 1).succeed();
    }

    // ---- environment_gate --------------------------------------------------------------------

    /**
     * Clear weather runs; rain does not.
     *
     * <p>The rain is set on the server level, so this is the real predicate the blueprint reads rather
     * than a stand-in. Both halves, because "it rained and nothing happened" is also what a blueprint
     * that always cancels looks like.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void environmentGateStopsTheMachineInTheRain(GameTestHelper helper) {
        var level = helper.getLevel();
        var wasRaining = level.isRaining();
        try {
            // setWeatherParameters alone is not enough: Level.isRaining() reads the interpolated
            // rainLevel, which ramps by 0.01 a tick and only crosses the 0.2 threshold ~20 ticks
            // later. The blueprint would read "clear" for the first part of the run and the test
            // would be measuring the ramp rather than the graph.
            level.setWeatherParameters(0, 20_000, true, false);
            level.setRainLevel(1.0f);
            if (!level.isRaining()) {
                helper.fail("could not make it rain, so there is nothing to test");
                return;
            }
            var wet = MBDScenario.of(helper)
                    .placeMachine(ENV_GATE_ID, MACHINE)
                    .insertItem(0, new ItemStack(Items.STONE, 4))
                    .runTicks(RUN_TICKS);
            var produced = wet.getItem(1);
            if (!produced.isEmpty()) {
                helper.fail("the machine ran in the rain: output slot holds " + produced);
                return;
            }

            level.setWeatherParameters(20_000, 0, false, false);
            level.setRainLevel(0f);
            MBDScenario.of(helper)
                    .placeMachine(ENV_GATE_ID, new BlockPos(3, 1, 1))
                    .insertItem(0, new ItemStack(Items.STONE, 4))
                    .runTicks(RUN_TICKS)
                    .assertItemCountAtLeast(1, Items.DIRT, 1)
                    .succeed();
        } finally {
            // The level is shared with every other test in the batch, and one left raining would
            // change what an unrelated machine does.
            level.setWeatherParameters(wasRaining ? 0 : 20_000, wasRaining ? 20_000 : 0,
                    wasRaining, false);
            level.setRainLevel(wasRaining ? 1.0f : 0f);
        }
    }

    // ---- upkeep ------------------------------------------------------------------------------

    /** With coolant in the tank the machine runs, and the coolant is actually consumed. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void upkeepBurnsCoolantWhileWorking(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(UPKEEP_ID, MACHINE)
                .insertFluid(new FluidStack(Fluids.WATER, UPKEEP_TANK))
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        if (!scenario.machine().getRecipeLogic().isWorking()) {
            helper.fail("the machine never started a recipe with a full tank");
            return;
        }
        scenario.runTicks(5);
        var left = fluidAmount(scenario);
        if (left >= UPKEEP_TANK) {
            helper.fail("no coolant was consumed after 5 working ticks: tank holds " + left);
            return;
        }
        scenario.succeed();
    }

    /**
     * An empty tank stalls the machine rather than letting it finish.
     *
     * <p>Asserted as <em>waiting</em> and not merely "no output": a machine that never started would
     * also produce nothing, and the whole point of this blueprint over a cancel is that the state it
     * leaves behind says why.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void upkeepStallsWithoutCoolant(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(UPKEEP_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 4))
                .runUntil(machine -> machine.getRecipeLogic().isWaiting(), RUN_TICKS);
        if (!scenario.machine().getRecipeLogic().isWaiting()) {
            helper.fail("a dry machine did not go into waiting; status is "
                    + scenario.machine().getRecipeLogic().getStatus());
            return;
        }
        var produced = scenario.getItem(1);
        if (!produced.isEmpty()) {
            helper.fail("a dry machine still finished a recipe: output holds " + produced);
            return;
        }
        scenario.succeed();
    }

    // ---- heat_buildup ------------------------------------------------------------------------

    /**
     * Heat rises while working, is capped, and falls back while idle.
     *
     * <p>All three in one test because they are one number: a blueprint that only ever added would
     * pass a rise-only assertion, and one that only ever clamped would pass a cap-only assertion.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void heatBuildupRisesAndFalls(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(HEAT_ID, MACHINE)
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> heat(machine) > 0f, RUN_TICKS);
        var warmed = heat(scenario.machine());
        if (warmed <= 0f) {
            helper.fail("a working machine never gained heat");
            return;
        }

        // Long enough to be well past the cap if nothing stopped it.
        scenario.runTicks(60);
        var capped = heat(scenario.machine());
        if (capped > MAX_HEAT) {
            helper.fail("heat ran past maxHeat: " + capped + " > " + MAX_HEAT);
            return;
        }

        // Take the input away and it should cool. Emptying the slot is what makes the machine idle.
        var input = MBDTestHelper.capability(helper, scenario.machine(), Capabilities.ItemHandler.BLOCK);
        if (input != null) input.extractItem(0, Integer.MAX_VALUE, false);
        var beforeCooling = heat(scenario.machine());
        scenario.runUntil(machine -> heat(machine) < beforeCooling, RUN_TICKS);
        var cooled = heat(scenario.machine());
        if (cooled >= beforeCooling) {
            helper.fail("an idle machine did not cool: heat stayed at " + cooled);
            return;
        }
        scenario.succeed();
    }

    /**
     * A hot machine runs a shorter recipe than a cold one — the heat is read back, not just stored.
     *
     * <h2>Why the heat is planted rather than earned</h2>
     * Letting the machine warm itself up would make this test depend on the whole tick half as well,
     * and on the periodic {@code Mark Recipe Dirty} landing inside the budget. Writing the heat
     * straight into the machine's custom data isolates the half under test: given this much heat, is
     * the recipe shorter? {@link #heatBuildupRisesAndFalls} covers the writing half separately.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void heatBuildupSpeedsTheRecipeUp(GameTestHelper helper) {
        var cold = workingMachine(helper, HEAT_STEADY_ID, MACHINE);
        if (cold == null) return;
        var coldDuration = cold.machine().getRecipeLogic().getMaxProgress();
        if (coldDuration != DURATION) {
            helper.fail("a cold machine ran a " + coldDuration + "-tick recipe, expected " + DURATION);
            return;
        }

        var hot = MBDScenario.of(helper)
                .placeMachine(HEAT_STEADY_ID, new BlockPos(3, 1, 1))
                .with(machine -> machine.getCustomData().putFloat(HEAT_KEY, MAX_HEAT))
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        if (!hot.machine().getRecipeLogic().isWorking()) {
            helper.fail("the hot machine never started a recipe");
            return;
        }
        var hotDuration = hot.machine().getRecipeLogic().getMaxProgress();
        // bonusAtMaxHeat defaults to 2, so at full heat the factor is 2 and 20 ticks become 10.
        if (hotDuration != DURATION / 2) {
            helper.fail("a machine at full heat ran a " + hotDuration + "-tick recipe, expected "
                    + (DURATION / 2));
            return;
        }
        hot.succeed();
    }

    /**
     * A machine whose heat is not moving does not write its custom data.
     *
     * <p>Custom data is {@code @DescSynced} and its setter copies the tag, so an unconditional
     * per-tick write is a tag copy, an event and a packet <em>per machine</em> — and an idle machine
     * rests at 0 and a busy one at {@code maxHeat}, which is most of a machine's life. The blueprint
     * gates the write on the heat actually changing; this is what keeps that gate there.</p>
     *
     * <p>Identity is the observable: {@code Merge Custom Data} builds a fresh tag and hands it to the
     * setter, so the machine's tag is a different object exactly on the ticks that wrote.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void heatBuildupDoesNotWriteWhenNothingChanged(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(HEAT_ID, MACHINE)
                .runTicks(10);
        // Idle and already at the floor, so every tick from here computes the same heat it read.
        if (heat(scenario.machine()) != 0f) {
            helper.fail("an idle machine did not settle at 0 heat; heat is " + heat(scenario.machine()));
            return;
        }
        var before = scenario.machine().getCustomData();
        scenario.runTicks(40);
        if (scenario.machine().getCustomData() != before) {
            helper.fail("custom data was rewritten on ticks where the heat never moved");
            return;
        }
        scenario.succeed();
    }

    // ---- upgrade_slots -----------------------------------------------------------------------

    /**
     * Two upgrades halve the duration; an empty slot leaves it alone; a wrong item does nothing.
     *
     * <p>The wrong-item case is the one worth having. Counting the stack without checking what it is
     * would make a stack of cobblestone a speed upgrade, and that mistake produces a machine that is
     * faster rather than one that is broken — nobody reports it.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void upgradeSlotsScaleWithTheUpgradeItem(GameTestHelper helper) {
        var bare = workingMachine(helper, UPGRADE_ID, MACHINE);
        if (bare == null) return;
        if (bare.machine().getRecipeLogic().getMaxProgress() != DURATION) {
            helper.fail("an empty upgrade slot changed the duration to "
                    + bare.machine().getRecipeLogic().getMaxProgress());
            return;
        }

        var wrong = MBDScenario.of(helper)
                .placeMachine(UPGRADE_ID, new BlockPos(3, 1, 1))
                .with(machine -> setUpgrade(machine, new ItemStack(Items.COBBLESTONE, 2)))
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        if (wrong.machine().getRecipeLogic().getMaxProgress() != DURATION) {
            helper.fail("cobblestone in the upgrade slot acted as an upgrade: duration was "
                    + wrong.machine().getRecipeLogic().getMaxProgress());
            return;
        }

        var upgraded = MBDScenario.of(helper)
                .placeMachine(UPGRADE_ID, new BlockPos(5, 1, 1))
                .with(machine -> setUpgrade(machine, new ItemStack(UPGRADE_ITEM, 2)))
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        var duration = upgraded.machine().getRecipeLogic().getMaxProgress();
        if (duration != UPGRADED_DURATION) {
            helper.fail("two upgrades gave a " + duration + "-tick recipe, expected "
                    + UPGRADED_DURATION);
            return;
        }
        // The upgrades must still be there: the blueprint peeks with simulate, it does not consume.
        var stillThere = upgradeStack(upgraded.machine());
        if (stillThere.getCount() != 2) {
            helper.fail("the upgrades were consumed; slot holds " + stillThere);
            return;
        }
        upgraded.succeed();
    }

    // ---- debug_probe -------------------------------------------------------------------------

    /**
     * Holding the probe consumes the right-click; holding anything else does not.
     *
     * <p>The chat message itself is not asserted — a mock player swallows it — but the interaction
     * result is the same flow's last node, so reaching {@code SUCCESS} means the whole chain ran:
     * the item test matched, the branch took its true side, {@code Send Message} did not throw, and
     * {@code Set Item Interaction Result} wrote back onto the event.</p>
     *
     * <p>The stick is the interesting negative: it is the blueprint's <em>default</em> probe item, so
     * a machine that ignored the override would consume it. Passing both halves is what says the
     * parameter reached the graph.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void debugProbeConsumesOnlyItsProbeItem(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper).placeMachine(DEBUG_PROBE_ID, MACHINE);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var pos = helper.absolutePos(MACHINE);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        var withProbe = use(scenario, player, pos, hit, new ItemStack(PROBE_ITEM));
        if (withProbe != ItemInteractionResult.SUCCESS) {
            helper.fail("right-clicking with the probe returned " + withProbe + ", expected SUCCESS");
            return;
        }

        var withStick = use(scenario, player, pos, hit, new ItemStack(Items.STICK));
        if (withStick == ItemInteractionResult.SUCCESS) {
            helper.fail("right-clicking with a stick was consumed too — probeItem was not read");
            return;
        }
        scenario.succeed();
    }

    /**
     * Right-click the machine holding {@code held}.
     *
     * <p>The stack goes into the player's hand rather than only into the call, because the blueprint
     * reads {@code heldItem} — which {@code UseItemOnEventNode} derives from player and hand, not from
     * the item the block method was handed.</p>
     */
    private static ItemInteractionResult use(MBDScenario scenario, Player player, BlockPos pos,
                                             BlockHitResult hit, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var machine = scenario.machine();
        return machine.useItemOn(held, machine.getBlockState(), scenario.helper().getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit);
    }

    // ---- helpers -----------------------------------------------------------------------------

    /** The heat the {@code heat_buildup} blueprint has stored, or 0 before it has written anything. */
    private static float heat(MBDMachine machine) {
        return machine.getCustomData().getFloat(HEAT_KEY);
    }

    /** How much fluid the machine's tank holds. */
    private static int fluidAmount(MBDScenario scenario) {
        var handler = MBDTestHelper.capability(scenario.helper(), scenario.machine(),
                Capabilities.FluidHandler.BLOCK);
        return handler == null ? 0 : handler.getFluidInTank(0).getAmount();
    }

    /**
     * The upgrade trait's slot 0.
     *
     * <p>Reached through the named trait rather than through the machine's aggregated item handler:
     * the upgrade trait is {@code IO.NONE}, and the aggregate exposes slots in trait order, so an
     * index into it would silently mean a different slot the moment the fixture gained a trait.</p>
     */
    private static IItemHandler upgradeHandler(MBDMachine machine) {
        return machine.getTraitByName(UPGRADE_TRAIT) instanceof SimpleCapabilityTrait<?, ?> trait
                && trait.getCapContent(IO.BOTH) instanceof IItemHandler handler ? handler : null;
    }

    private static void setUpgrade(MBDMachine machine, ItemStack stack) {
        var handler = upgradeHandler(machine);
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(0, stack);
        } else {
            throw new IllegalStateException("the upgrade trait is not writable");
        }
    }

    private static ItemStack upgradeStack(MBDMachine machine) {
        var handler = upgradeHandler(machine);
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(0);
    }

    /**
     * How many of {@code item} sit in the output trait — slots 1 and 2 on the two-slot-output machines
     * the chance fixtures use. Which of the two the bonus lands in is the handler's business, not the
     * blueprint's, so the assertion should not care.
     */
    private static int countInOutput(MBDScenario scenario, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 1; slot <= 2; slot++) {
            var stack = scenario.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /**
     * A placed machine that has actually started a recipe, or {@code null} after failing the test.
     *
     * <p>{@code getMaxProgress()} only means anything once the logic is working — read before that it
     * reports whatever the last recipe left behind, which for a fresh machine is zero and would make
     * every duration assertion here fail for the wrong reason.</p>
     */
    private static MBDScenario workingMachine(GameTestHelper helper,
                                              net.minecraft.resources.ResourceLocation id,
                                              BlockPos pos) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(id, pos)
                .insertItem(0, new ItemStack(Items.STONE, 16))
                .runUntil(machine -> machine.getRecipeLogic().isWorking(), RUN_TICKS);
        if (!scenario.machine().getRecipeLogic().isWorking()) {
            helper.fail(id + " never started a recipe");
            return null;
        }
        return scenario;
    }
}

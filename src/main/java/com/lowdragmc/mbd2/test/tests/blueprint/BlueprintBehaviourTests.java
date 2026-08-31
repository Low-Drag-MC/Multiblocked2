package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One behaviour test per node group: does the group's effect actually reach the machine?
 *
 * <p>{@link BlueprintNodeCatalogueTests} already proves every node spawns, round-trips and is
 * documented, which is the part that scales. These cover what that cannot: that a write lands, that a
 * read returns the right value rather than merely a value, and that the event it hangs off fires.</p>
 *
 * <h2>Why every fixture here hooks Machine Tick</h2>
 * {@code MachineOnLoadEvent} reaches a blueprint through a server {@code TickTask}, and the gametest
 * harness ticks the level rather than the server — so when it arrives relative to a test's assertions
 * is not deterministic. {@code OnLoadEventNode} is covered structurally by
 * {@link BlueprintNodeCatalogueTests} instead; a behaviour test for it would be flaky, which is worse
 * than an honest gap.
 */
@GameTestHolder(MBD2.MOD_ID)
public class BlueprintBehaviourTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintBehaviourFixtures.TIER_MACHINE_ID; }

    /**
     * Machine action write, Machine Info context read, and the context's fallback to the blueprint's
     * own machine — all three in one round trip.
     *
     * <p>Asserting the exact tier matters: a read that returns nothing stages no output and the signal
     * ends up zero, which this catches and a "was set at all" assertion would not.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void machineTierRoundTripsThroughInfoContext(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.TIER_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var machine = scenario.machine();
        if (machine.getMachineLevel() != BlueprintBehaviourFixtures.TIER) {
            helper.fail("Set Machine Tier did not apply, tier is " + machine.getMachineLevel());
            return;
        }
        if (machine.getAnalogOutputSignal() != BlueprintBehaviourFixtures.TIER) {
            helper.fail("tier read back through Machine Info was " + machine.getAnalogOutputSignal());
            return;
        }
        scenario.succeed();
    }

    /** Set Custom Data then Merge Custom Data: the merge must keep what the set wrote. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void customDataIsWrittenAndMerged(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.CUSTOM_DATA_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var data = scenario.machine().getCustomData();
        if (data.getInt(BlueprintBehaviourFixtures.COUNTER_KEY) != 1) {
            helper.fail("Set Custom Data did not apply, tag was " + data);
            return;
        }
        if (!data.getBoolean(BlueprintBehaviourFixtures.FLAG_KEY)) {
            helper.fail("Merge Custom Data did not apply, tag was " + data);
            return;
        }
        scenario.succeed();
    }

    /** The trait capability bridge plus Receive Energy: the machine fills its own buffer. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintFillsOwnEnergyBuffer(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.ENERGY_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5)
                .assertEnergyAtLeast(5_000)
                .succeed();
    }

    /**
     * Recipe Modify (Before) plus the recipe build nodes plus Set Event Recipe: a blueprint rewrites
     * every recipe to two thousand ticks, so nothing finishes in forty.
     *
     * <p>{@code BlueprintTests.plainMachineRunsRecipe} is the control that makes this mean something —
     * the same recipe on the same machine shape does finish in forty ticks without the blueprint.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintRewritesRecipeDuration(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.SLOW_RECIPE_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItem(1, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * A blueprint holding Set Working Enabled(false) every tick keeps the machine from running.
     *
     * <p>Every tick rather than once, because suspending is not a latch - see the fixture.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintDisablesRecipeLogic(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.DISABLED_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80);
        var machine = scenario.machine();
        // Marker first: it separates "the blueprint never ran" from "it ran and the suspend did not
        // hold", which are different bugs in different places.
        if (machine.getAnalogOutputSignal() != BlueprintBehaviourFixtures.DISABLED_MARKER) {
            helper.fail("the flow never reached Set Working Enabled (marker signal was "
                    + machine.getAnalogOutputSignal() + ")");
            return;
        }
        if (!machine.getRecipeLogic().isSuspend()) {
            helper.fail("expected the recipe logic to be suspended, was "
                    + machine.getRecipeLogic().getStatus());
            return;
        }
        scenario.assertItem(1, ItemStack.EMPTY).succeed();
    }

    /**
     * A blueprint holding a client-only action loads and dispatches on a dedicated server.
     *
     * <p>This is the question "can a blueprint crash a server" in its concrete form: {@code Play State
     * Sound} calls an {@code @OnlyIn(Dist.CLIENT)} method whose body touches a client-only class. The
     * marker downstream of it proves both halves — the server survived, and the skipped action still
     * passed the flow on rather than dead-ending it.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clientOnlyActionIsSkippedOnServer(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.CLIENT_ONLY_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        int signal = scenario.machine().getAnalogOutputSignal();
        if (signal != BlueprintBehaviourFixtures.PAST_CLIENT_ONLY_MARKER) {
            helper.fail("the flow did not survive the client-only action: marker signal was " + signal);
            return;
        }
        scenario.succeed();
    }

    /**
     * Two entry nodes for the same event both run.
     *
     * <p>Asserted through two independent effects rather than one shared one, because the order they run
     * in is node-creation order and is not visible on the canvas — the blueprint warns about exactly
     * that. What is guaranteed, and what this pins down, is that neither is silently dropped.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyEntryForAnEventRuns(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.DOUBLE_ENTRY_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var machine = scenario.machine();
        if (machine.getCustomData().getInt(BlueprintBehaviourFixtures.COUNTER_KEY) != 1) {
            helper.fail("the first Machine Tick entry did not run, custom data was "
                    + machine.getCustomData());
            return;
        }
        if (machine.getAnalogOutputSignal() != BlueprintBehaviourFixtures.SECOND_ENTRY_MARKER) {
            helper.fail("the second Machine Tick entry did not run, signal was "
                    + machine.getAnalogOutputSignal());
            return;
        }
        scenario.succeed();
    }

    /**
     * A blueprint on a multiblock controller: Structure Formed fires once the pattern matches, and its
     * flow reaches a machine action.
     */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void multiblockBlueprintReactsToForming(GameTestHelper helper) {
        BlockPos controller = new BlockPos(5, 1, 5);
        var stone = Blocks.STONE.defaultBlockState();
        var scenario = MBDScenario.of(helper)
                .placeBlock(controller.offset(-1, 0, -1), stone)
                .placeBlock(controller.offset(0, 0, -1), stone)
                .placeBlock(controller.offset(1, 0, -1), stone)
                .placeBlock(controller.offset(-1, 0, 0), stone)
                .placeBlock(controller.offset(1, 0, 0), stone)
                .placeBlock(controller.offset(-1, 0, 1), stone)
                .placeBlock(controller.offset(0, 0, 1), stone)
                .placeBlock(controller.offset(1, 0, 1), stone)
                .placeMachine(BlueprintBehaviourFixtures.MULTIBLOCK_MACHINE_ID, controller)
                .assertFormed()
                .runTicks(3);
        int signal = scenario.machine().getAnalogOutputSignal();
        if (signal != BlueprintBehaviourFixtures.FORMED_SIGNAL) {
            helper.fail("Structure Formed blueprint did not run: expected signal "
                    + BlueprintBehaviourFixtures.FORMED_SIGNAL + ", got " + signal);
            return;
        }
        scenario.succeed();
    }

    /**
     * Every runtime value a blueprint can write, it can read back.
     *
     * <p>The setters shipped without getters, which made the whole system write-only: a blueprint
     * could put a mode on a machine and never act on it again. Three values by three different
     * routes, because they fail independently — the generic int getter, the dedicated capability-IO
     * one, and the generic IO one.</p>
     *
     * <p>The values are ones nothing else in the machine produces, so a getter that reads nothing
     * lands on zero or {@code NONE} rather than accidentally on the right answer.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyRuntimeValueWrittenCanBeReadBack(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.RUNTIME_READBACK_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(5);
        var machine = scenario.machine();

        int interval = machine.getAnalogOutputSignal();
        if (interval != BlueprintBehaviourFixtures.READBACK_INTERVAL) {
            helper.fail("Get Runtime Int read back " + interval + ", expected "
                    + BlueprintBehaviourFixtures.READBACK_INTERVAL);
            return;
        }
        var data = machine.getCustomData();
        var capability = data.getString(BlueprintBehaviourFixtures.CAPABILITY_IO_KEY);
        if (!"OUT".equals(capability)) {
            helper.fail("Get Capability IO Side read back '" + capability + "', expected OUT");
            return;
        }
        var generic = data.getString(BlueprintBehaviourFixtures.RUNTIME_IO_KEY);
        if (!"IN".equals(generic)) {
            helper.fail("Get Runtime IO read back '" + generic + "', expected IN");
            return;
        }
        if (!data.getBoolean(BlueprintBehaviourFixtures.RUNTIME_BOOL_KEY)) {
            helper.fail("Get Runtime Bool read back false for a flag the same graph had just set");
            return;
        }
        scenario.succeed();
    }

    /**
     * A part can reach its controller and read it.
     *
     * <p>{@code Part Controllers} hands back controllers and, until {@code Controller Machine}
     * existed, nothing else in the graph accepted one — a part could find what it belonged to and
     * then ask it nothing. The tier is the controller's own and the part has no way to produce it
     * otherwise, so the signal is only nine if every step of the hop worked.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aPartCanReadItsController(GameTestHelper helper) {
        var controller = new BlockPos(1, 1, 1);
        var part = new BlockPos(0, 1, 1);
        var scenario = MBDScenario.of(helper)
                .placeBlock(new BlockPos(2, 1, 1), Blocks.STONE.defaultBlockState())
                .placeMachine(BlueprintBehaviourFixtures.PART_READS_CONTROLLER_ID, part)
                .placeMachine(BlueprintBehaviourFixtures.CONTROLLER_TIER_ID, controller)
                .assertFormed()
                .runTicks(5);

        var partMachine = MBDTestHelper.getMachine(helper, part);
        int signal = partMachine.getAnalogOutputSignal();
        if (signal != BlueprintBehaviourFixtures.CONTROLLER_TIER) {
            helper.fail("the part read " + signal + " off its controller, expected "
                    + BlueprintBehaviourFixtures.CONTROLLER_TIER
                    + " — Part Controllers → Controller Machine → Tier did not complete");
            return;
        }
        helper.succeed();
    }

    // ---- recipe content editing -----------------------------------------------------------------

    /**
     * The control for the three below: with no blueprint, the two-output recipe makes both items.
     *
     * <p>Without it, "the diamond is missing" would be equally consistent with a working Remove node
     * and with a recipe that never ran, which are not the same news.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void twoOutputRecipeMakesBothItems(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.TWO_OUTPUT_CONTROL_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .assertItemCountAtLeast(2, Items.DIAMOND, 1)
                .succeed();
    }

    /**
     * Remove Recipe Content drops the content it is pointed at and leaves the rest alone.
     *
     * <p>The point of the pair of assertions is that removing one is not the same as clearing the
     * side: a node that dropped every item output would make the diamond vanish just the same, and
     * only the surviving dirt tells the two apart.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintRemovesOneOutputAndKeepsTheOther(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.REMOVE_ONE_OUTPUT_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .assertItem(2, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * Content Index Of Slot turns a slot name into the position the write nodes take.
     *
     * <p>The bonus output is deliberately the second one, so the three possible answers land in three
     * different worlds: the right index removes the diamond, a hardcoded zero removes the dirt, and a
     * lookup that finds nothing (-1) removes neither.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintRemovesTheOutputNamedByItsSlot(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.REMOVE_BY_SLOT_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .assertItem(2, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * Content At, Content With and Set Recipe Content edit a content the graph cannot interpret.
     *
     * <p>This is the case the whole capability-generic design exists for: nothing in the blueprint
     * knows the content holds a {@code SizedIngredient}, it only reads a chance, changes it and puts
     * the content back. A chance of zero is never rolled, so the diamond stops appearing while the
     * untouched dirt keeps coming.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintRewritesOneOutputsChance(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.ZERO_CHANCE_OUTPUT_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(4))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .assertItem(2, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * Content Value hands back what a content holds, well enough to build another content from it.
     *
     * <p>Reading a payload is the half of the generic design that was missing: a blueprint could add
     * and remove contents without knowing their capability, but could not look inside one, which made
     * "double whatever this recipe makes" inexpressible. The blueprint here names no item — it reads
     * the first output's payload, builds a content from it and adds that, so two stone yield four
     * dirt where the control yields two.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintReadsAContentsPayloadAndRebuildsIt(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.ECHO_FIRST_OUTPUT_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(2))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 4)
                .succeed();
    }

    /**
     * A content survives a trip out to NBT and back, which is how a blueprint reaches a payload MBD2
     * has no typed nodes for.
     *
     * <p>The capability's own codec is the only thing that can read another mod's payload, and it is
     * also the only thing that can build one. Exercising it on an item content is deliberate: what is
     * under test is the codec path, and an item is the payload whose end result is observable.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintRoundTripsAContentThroughNbt(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.NBT_ROUND_TRIP_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(2))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 4)
                .succeed();
    }

    /**
     * Ingredient Info resolves a payload into the stack and count it stands for.
     *
     * <p>Passing a payload along proves only that it was not dropped; this requires it to have been
     * understood, because the content that reaches the recipe is rebuilt from the unpacked stack. An
     * ingredient that resolved to nothing rebuilds as one matching nothing, and the extra dirt never
     * appears.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprintUnpacksAnIngredientIntoItsStack(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(BlueprintBehaviourFixtures.UNPACK_INGREDIENT_ID, new BlockPos(1, 1, 1))
                .insertItem(0, BlueprintFixtures.stone(2))
                .runTicks(80)
                .assertItemCountAtLeast(1, Items.DIRT, 4)
                .succeed();
    }

    // ---- payload makers and readers --------------------------------------------------------------

    /**
     * The item capability's payload pair survives a full round trip through the generic nodes.
     *
     * <p>See {@link PayloadRoundTrip} for the shape and why it goes through NBT. Three is a count no
     * default in the chain produces: the tag maker's own default is one, and every failure along the
     * way ends at zero.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void itemPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        assertSignal(helper, BlueprintBehaviourFixtures.ITEM_PAYLOAD_ID,
                BlueprintBehaviourFixtures.ITEM_PAYLOAD_COUNT);
    }

    /** The fluid capability's payload pair. @see #itemPayloadSurvivesTheRoundTrip */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluidPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        assertSignal(helper, BlueprintBehaviourFixtures.FLUID_PAYLOAD_ID,
                BlueprintBehaviourFixtures.FLUID_PAYLOAD_AMOUNT);
    }

    /**
     * The entity capability's payload pair, plus the type-taking maker.
     *
     * <p>Five rather than the two the tag step asked for is the point: it can only be five if the
     * entity type resolved out of the tag ingredient and the rebuild actually happened.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void entityPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        assertSignal(helper, BlueprintBehaviourFixtures.ENTITY_PAYLOAD_ID,
                BlueprintBehaviourFixtures.ENTITY_PAYLOAD_COUNT);
    }

    /**
     * Runs a payload round-trip machine and requires the number to come out the far end.
     *
     * <p>Shared with the mod-gated payload tests, which cannot live in this class: NeoForge
     * force-loads every {@code @GameTestHolder}, so a class naming a Mekanism type would throw before
     * the mod check ever ran.</p>
     */
    public static void assertSignal(GameTestHelper helper, ResourceLocation machineId, int expected) {
        var scenario = MBDScenario.of(helper)
                .placeMachine(machineId, new BlockPos(1, 1, 1))
                .runTicks(10);
        var actual = scenario.machine().getAnalogOutputSignal();
        if (actual != expected) {
            helper.fail("expected the payload round trip to emit " + expected + ", got " + actual
                    + " (a non-zero but wrong number means one node in the chain lost the value; zero"
                    + " means either the chain produced nothing or the blueprint never ran at all,"
                    + " and the other tests in this class are the ones that tell those apart)");
            return;
        }
        scenario.succeed();
    }
}

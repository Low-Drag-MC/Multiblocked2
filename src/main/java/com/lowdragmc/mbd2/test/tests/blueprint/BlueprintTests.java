package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
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
}

package com.lowdragmc.mbd2.test.tests;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Smoke tests for the MBD2 test framework. Each test demonstrates one core capability of the
 * framework; they also double as compile-time documentation for how to write MBD tests.
 *
 * <p>Run with {@code gradlew runGameTestServer}. Requires the empty structure NBTs to have
 * been generated once via {@link com.lowdragmc.mbd2.test.framework.EmptyStructureGenerator}.
 *
 * <p>Loaded via static initializer in {@link MBDSmokeFixtures}; ensure that class is touched
 * before the gametest server starts (referencing any field here forces class init).
 */
@GameTestHolder(MBD2.MOD_ID)
public class MBDSmokeTests {
    static { @SuppressWarnings("unused") var ignored = MBDSmokeFixtures.SIMPLE_MACHINE_ID; }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void placeSimpleMachine(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, new BlockPos(1, 1, 1))
                .assertExposes(Capabilities.ItemHandler.BLOCK, null)
                .assertExposes(Capabilities.EnergyStorage.BLOCK, null)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void runSimpleRecipe(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, MBDSmokeFixtures.stone(4))
                .insertEnergy(10_000)
                .runTicks(40) // recipe duration is 20 ticks; allow some slack
                .assertItem(1, MBDSmokeFixtures.dirt(1))
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void formMultiblockController(GameTestHelper helper) {
        // Place the 3x1x3 stone floor with the controller in the middle, then trigger
        // formation. The controller's pattern wants 'S' (stone) around the controller cell.
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(helper)
                .placeBlock(controller.offset(-1, 0, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(0, 0, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(1, 0, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(-1, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(1, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(-1, 0, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(0, 0, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeBlock(controller.offset(1, 0, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                .placeMachine(MBDSmokeFixtures.MULTIBLOCK_MACHINE_ID, controller)
                .assertFormed();
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void persistenceRoundTrip(GameTestHelper helper) {
        MBDScenario.of(helper)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, new BlockPos(1, 1, 1))
                .insertItem(0, MBDSmokeFixtures.stone(7))
                .insertEnergy(5_000)
                .assertPersistenceRoundTrip()
                .assertItem(0, MBDSmokeFixtures.stone(7))
                .assertEnergyAtLeast(5_000)
                .succeed();
    }
}

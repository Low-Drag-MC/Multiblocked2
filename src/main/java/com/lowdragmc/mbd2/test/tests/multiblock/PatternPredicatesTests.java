package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PatternPredicatesTests {
    static { @SuppressWarnings("unused") var ignored = PatternPredicatesFixtures.ANY_ID; }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void any_matches_dirt(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.DIRT.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.DIRT.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.ANY_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_matches_stone(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_matches_dirt(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.DIRT.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.DIRT.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_rejects_iron(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.IRON_BLOCK.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void air_matches_air(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        // surrounding cells are already air (default template)
        MBDScenario.of(h)
                .placeMachine(PatternPredicatesFixtures.AIR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void air_rejects_solid_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.AIR_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }
}

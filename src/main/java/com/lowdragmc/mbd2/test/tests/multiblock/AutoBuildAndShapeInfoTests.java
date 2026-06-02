package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests for {@code BlockPattern.autoBuild} (player-driven gadget placement) and
 * {@code BlockPattern.getPreview} (editor / JEI preview generation), covering both ordinary
 * blocks and {@code rotateFollowController} predicates.
 */
@GameTestHolder(MBD2.MOD_ID)
public class AutoBuildAndShapeInfoTests {
    static {
        @SuppressWarnings("unused") var ignored = PatternRotationFixtures.MACHINE_STAIRS_ID;
    }

    /** Creative mock player drives the autoBuild path that picks the first candidate from each
     *  predicate's {@code candidates} array (no inventory lookup needed). */
    private static Player creativePlayer(GameTestHelper h) {
        return h.makeMockPlayer(GameType.CREATIVE);
    }

    // ===== autoBuild =====

    /** Auto-build on a NORTH-facing controller with the rotation fixture must place a
     *  NORTH-facing stair (canonical state) and a stone block at the expected offsets. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_places_canonical_blocks_facing_north(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, Direction.NORTH);
        // Pattern is "CSX" with charDir=LEFT → S at controller.offset(-1,0,0), X at (-2,0,0)
        if (scenario.machine() instanceof IMultiController controllerMachine) {
            var state = new MultiblockState(h.getLevel(), h.absolutePos(controller));
            controllerMachine.getPattern().autoBuild(creativePlayer(h), state);
        }
        // Assert stair is placed with its canonical NORTH facing (rotateFollowController is on
        // but facing is NORTH so rotation == NONE).
        BlockState stairState = h.getBlockState(controller.offset(-1, 0, 0));
        if (!(stairState.getBlock() instanceof StairBlock) || stairState.getValue(StairBlock.FACING) != Direction.NORTH) {
            h.fail("Expected NORTH-facing stair at west of controller, got " + stairState);
        }
        if (!h.getBlockState(controller.offset(-2, 0, 0)).is(Blocks.STONE)) {
            h.fail("Expected stone at controller.offset(-2,0,0)");
        }
        h.succeed();
    }

    /** Auto-build on an EAST-facing controller. Pattern offsets rotate CW90: the S cell that
     *  was at (-1,0,0) for NORTH ends up at (0,0,-1) for EAST. The placed stair must be
     *  EAST-facing (canonical NORTH rotated CW90). */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_rotates_stair_for_facing_east(GameTestHelper h) {
        runAutoBuildRotationTest(h, Direction.EAST);
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_rotates_stair_for_facing_south(GameTestHelper h) {
        runAutoBuildRotationTest(h, Direction.SOUTH);
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_rotates_stair_for_facing_west(GameTestHelper h) {
        runAutoBuildRotationTest(h, Direction.WEST);
    }

    private static void runAutoBuildRotationTest(GameTestHelper h, Direction facing) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, facing);
        if (scenario.machine() instanceof IMultiController controllerMachine) {
            var state = new MultiblockState(h.getLevel(), h.absolutePos(controller));
            controllerMachine.getPattern().autoBuild(creativePlayer(h), state);
        }
        BlockPos stairPos = controller.offset(RotationHelper.rotateOffset(new BlockPos(-1, 0, 0), facing));
        BlockPos stonePos = controller.offset(RotationHelper.rotateOffset(new BlockPos(-2, 0, 0), facing));
        BlockState stairState = h.getBlockState(stairPos);
        BlockState expectedStair = PatternRotationFixtures.STAIRS_NORTH
                .rotate(RotationHelper.rotationFromFacing(facing));
        if (!stairState.equals(expectedStair)) {
            h.fail("Expected " + expectedStair + " at " + stairPos + " for facing " + facing
                    + ", got " + stairState);
        }
        if (!h.getBlockState(stonePos).is(Blocks.STONE)) {
            h.fail("Expected stone at " + stonePos + " for facing " + facing);
        }
        // Verify the rotated pattern now actually forms — round-trip the auto-build into
        // a successful structure form, which exercises the same predicate path.
        scenario.assertFormed().succeed();
    }

    /** Fluids must be placed after all solid blocks. If a water cell were placed before its
     *  neighbouring stone cell, the water would flow into the still-empty stone slot and the
     *  stone block would end up replacing a water source — observable as a waterlogged cell. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_places_fluids_last(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachineFacing(PatternRotationFixtures.MACHINE_FLUID_AUTOBUILD, controller, Direction.NORTH);
        if (scenario.machine() instanceof IMultiController controllerMachine) {
            var state = new MultiblockState(h.getLevel(), h.absolutePos(controller));
            controllerMachine.getPattern().autoBuild(creativePlayer(h), state);
        }
        // Pattern "CWSS" with charDir=LEFT: water at -x, stone at -2x, stone at -3x.
        BlockPos waterPos = controller.offset(-1, 0, 0);
        BlockPos stone1Pos = controller.offset(-2, 0, 0);
        BlockPos stone2Pos = controller.offset(-3, 0, 0);
        if (!h.getBlockState(waterPos).is(Blocks.WATER)) {
            h.fail("Expected water at " + waterPos + ", got " + h.getBlockState(waterPos));
        }
        // The stone cells must be pure stone (not flooded / displaced). If water had been
        // placed first it would either occupy the stone slots when they were air, or the stone
        // would arrive with a waterlogged property — neither is acceptable.
        if (!h.getBlockState(stone1Pos).is(Blocks.STONE)) {
            h.fail("Expected stone at " + stone1Pos + ", got " + h.getBlockState(stone1Pos));
        }
        if (!h.getBlockState(stone2Pos).is(Blocks.STONE)) {
            h.fail("Expected stone at " + stone2Pos + ", got " + h.getBlockState(stone2Pos));
        }
        h.succeed();
    }

    /** Auto-build must not overwrite blocks the player has already placed manually. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void autobuild_does_not_overwrite_existing_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stonePos = controller.offset(-2, 0, 0);
        h.setBlock(stonePos, Blocks.IRON_BLOCK.defaultBlockState()); // wrong block, but pre-existing
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, Direction.NORTH);
        if (scenario.machine() instanceof IMultiController controllerMachine) {
            var state = new MultiblockState(h.getLevel(), h.absolutePos(controller));
            controllerMachine.getPattern().autoBuild(creativePlayer(h), state);
        }
        // Pre-existing iron block should still be there.
        if (!h.getBlockState(stonePos).is(Blocks.IRON_BLOCK)) {
            h.fail("autoBuild overwrote pre-existing iron block at " + stonePos);
        }
        h.succeed();
    }

    // ===== getPreview =====

    /** {@code getPreview} always returns the canonical NORTH-facing block info — preview is
     *  for editor / JEI which display the as-authored layout. In-world rotation is the
     *  preview renderer's job. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void preview_returns_canonical_stair_state(GameTestHelper h) {
        // Build the pattern by placing a transient controller so we can grab its pattern.
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, Direction.NORTH);
        BlockInfo[][][] preview = null;
        if (scenario.machine() instanceof IMultiController controllerMachine) {
            preview = controllerMachine.getPattern().getPreview(new int[]{1});
        }
        if (preview == null) {
            h.fail("getPreview returned null");
            return;
        }
        // Find the stair cell; must be the canonical NORTH-facing state.
        boolean stairFound = false;
        boolean stoneFound = false;
        for (BlockInfo[][] yLayer : preview) {
            for (BlockInfo[] zRow : yLayer) {
                for (BlockInfo info : zRow) {
                    if (info == null) continue;
                    BlockState state = info.getBlockState();
                    if (state == null) continue;
                    if (state.getBlock() instanceof StairBlock) {
                        if (state.getValue(StairBlock.FACING) != Direction.NORTH) {
                            h.fail("Preview stair should be canonical NORTH-facing, got " + state);
                        }
                        stairFound = true;
                    } else if (state.is(Blocks.STONE)) {
                        stoneFound = true;
                    }
                }
            }
        }
        if (!stairFound) h.fail("Stair candidate not found in preview");
        if (!stoneFound) h.fail("Stone candidate not found in preview");
        h.succeed();
    }
}

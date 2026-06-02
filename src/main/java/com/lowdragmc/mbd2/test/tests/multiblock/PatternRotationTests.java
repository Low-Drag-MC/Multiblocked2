package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PatternRotationTests {
    static { @SuppressWarnings("unused") var ignored = PatternRotationFixtures.MACHINE_STAIRS_ID; }

    /** NORTH-frame offsets — pattern {@code .aisle("CSX")} with default LEFT char direction. */
    private static final BlockPos STAIRS_OFFSET_NORTH = new BlockPos(-1, 0, 0);
    private static final BlockPos STONE_OFFSET_NORTH = new BlockPos(-2, 0, 0);

    private static void runFacingTest(GameTestHelper h, Direction facing) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stairsPos = controller.offset(RotationHelper.rotateOffset(STAIRS_OFFSET_NORTH, facing));
        BlockPos stonePos = controller.offset(RotationHelper.rotateOffset(STONE_OFFSET_NORTH, facing));
        var rotatedStairs = PatternRotationFixtures.STAIRS_NORTH.rotate(RotationHelper.rotationFromFacing(facing));
        MBDScenario.of(h)
                .placeBlock(stairsPos, rotatedStairs)
                .placeBlock(stonePos, Blocks.STONE.defaultBlockState())
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, facing)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void forms_with_controller_facing_north(GameTestHelper h) {
        runFacingTest(h, Direction.NORTH);
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void forms_with_controller_facing_east(GameTestHelper h) {
        runFacingTest(h, Direction.EAST);
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void forms_with_controller_facing_south(GameTestHelper h) {
        runFacingTest(h, Direction.SOUTH);
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void forms_with_controller_facing_west(GameTestHelper h) {
        runFacingTest(h, Direction.WEST);
    }

    /** Controller facing EAST expects EAST-facing stairs; placing NORTH-facing stairs must not form. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void wrong_rotation_does_not_form(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        Direction facing = Direction.EAST;
        BlockPos stairsPos = controller.offset(RotationHelper.rotateOffset(STAIRS_OFFSET_NORTH, facing));
        BlockPos stonePos = controller.offset(RotationHelper.rotateOffset(STONE_OFFSET_NORTH, facing));
        MBDScenario.of(h)
                .placeBlock(stairsPos, PatternRotationFixtures.STAIRS_NORTH)
                .placeBlock(stonePos, Blocks.STONE.defaultBlockState())
                .placeMachineFacing(PatternRotationFixtures.MACHINE_STAIRS_ID, controller, facing)
                .formNow()
                .assertNotFormed()
                .succeed();
    }
}

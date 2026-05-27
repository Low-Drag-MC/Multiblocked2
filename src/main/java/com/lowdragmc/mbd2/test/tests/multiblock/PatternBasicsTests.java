package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PatternBasicsTests {
    static { @SuppressWarnings("unused") var ignored = PatternBasicsFixtures.MACHINE_FLOOR_ID; }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void floor_pattern_forms(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h);
        // 3x1x3 stone floor around the controller
        MBDTestHelper.fillVolume(h, controller.offset(-1, 0, -1), controller.offset(1, 0, 1), Blocks.STONE.defaultBlockState());
        scenario.placeMachine(PatternBasicsFixtures.MACHINE_FLOOR_ID, controller).assertFormed().succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void controller_alone_does_not_form(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PatternBasicsFixtures.MACHINE_FLOOR_ID, new BlockPos(5, 1, 5))
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void wrong_block_does_not_form(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDTestHelper.fillVolume(h, controller.offset(-1, 0, -1), controller.offset(1, 0, 1), Blocks.DIRT.defaultBlockState());
        MBDScenario.of(h)
                .placeMachine(PatternBasicsFixtures.MACHINE_FLOOR_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }
}

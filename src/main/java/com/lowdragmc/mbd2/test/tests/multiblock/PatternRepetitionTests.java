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
public class PatternRepetitionTests {
    static { @SuppressWarnings("unused") var ignored = PatternRepetitionFixtures.MACHINE_ID; }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void forms_with_min_repetition(GameTestHelper h) {
        // controller in the middle, one row of stone N and S
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDTestHelper.fillVolume(h, controller.offset(-1, 0, -1), controller.offset(1, 0, -1), Blocks.STONE.defaultBlockState());
        MBDTestHelper.fillVolume(h, controller.offset(-1, 0, 1), controller.offset(1, 0, 1), Blocks.STONE.defaultBlockState());
        MBDTestHelper.fillVolume(h, controller.offset(-1, 0, 0), controller.offset(-1, 0, 0), Blocks.STONE.defaultBlockState());
        MBDTestHelper.fillVolume(h, controller.offset(1, 0, 0), controller.offset(1, 0, 0), Blocks.STONE.defaultBlockState());
        MBDScenario.of(h)
                .placeMachine(PatternRepetitionFixtures.MACHINE_ID, controller)
                .assertFormed()
                .succeed();
    }
}

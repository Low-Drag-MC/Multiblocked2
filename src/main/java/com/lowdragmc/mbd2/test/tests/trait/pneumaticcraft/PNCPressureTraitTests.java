package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PNCPressureTraitTests {
    static { @SuppressWarnings("unused") var ignored = PNCPressureTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void air_handler_machine_capability_exposed_on_north(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.AIR_HANDLER_MACHINE, Direction.NORTH)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void air_handler_machine_capability_exposed_on_null_side(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.AIR_HANDLER_MACHINE, null)
                .succeed();
    }
}

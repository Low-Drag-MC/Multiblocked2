package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class PNCHeatTraitTests {
    static { @SuppressWarnings("unused") var ignored = PNCHeatTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_exchanger_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCHeatTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.HEAT_EXCHANGER_BLOCK, null)
                .succeed();
    }
}

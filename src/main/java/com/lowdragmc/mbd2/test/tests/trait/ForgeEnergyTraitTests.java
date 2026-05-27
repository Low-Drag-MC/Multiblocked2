package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class ForgeEnergyTraitTests {
    static { @SuppressWarnings("unused") var ignored = ForgeEnergyTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void energy_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ForgeEnergyTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.EnergyStorage.BLOCK, null)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void energy_insert_and_assert(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ForgeEnergyTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(25_000)
                .assertEnergyAtLeast(25_000)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_energy(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ForgeEnergyTraitFixtures.MACHINE_ID, POS)
                .insertEnergy(40_000)
                .assertPersistenceRoundTrip()
                .assertEnergyAtLeast(40_000)
                .succeed();
    }
}

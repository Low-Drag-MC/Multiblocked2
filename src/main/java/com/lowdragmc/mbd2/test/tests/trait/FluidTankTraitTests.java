package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class FluidTankTraitTests {
    static { @SuppressWarnings("unused") var ignored = FluidTankTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_handler_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.FluidHandler.BLOCK, null)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fill_tank_then_assert(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 4000))
                .assertFluid(0, new FluidStack(Fluids.WATER, 4000))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_fluid(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidTankTraitFixtures.MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 4000))
                .assertPersistenceRoundTrip()
                .assertFluid(0, new FluidStack(Fluids.WATER, 4000))
                .succeed();
    }
}

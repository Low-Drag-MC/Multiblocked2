package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCHeatExchangerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class PNCHeatTraitTests {
    static { @SuppressWarnings("unused") var ignored = PNCHeatTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos EAST = POS.relative(Direction.EAST);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_exchanger_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCHeatTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.HEAT_EXCHANGER_BLOCK, null)
                .succeed();
    }

    /**
     * Auto IO has to reach the neighbour's exchanger, not its own — resolving the capability at its own
     * position made {@code transferHeat} run source-against-source, hit the zero-delta guard and move
     * nothing.
     *
     * <p>Driven by calling {@code handleAutoIO} rather than by ticking: {@code serverTick} also runs
     * {@code initializeAsHull(.., ALL_BLOCKS, Direction.values())} and {@code handler.tick()}, and
     * PneumaticCraft's own hull network equalises adjacent exchangers on its own. A ticked test would
     * pass whether or not auto IO did anything, which is precisely how this stayed hidden.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_auto_output_warms_the_neighbour(GameTestHelper h) {
        var target = MBDScenario.of(h).placeMachine(PNCHeatTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(PNCHeatTraitFixtures.MACHINE_ID, POS).machine();
        if (target == null || machine == null) { h.fail("Machines were not placed"); return; }

        heatTrait(machine).handler.setTemperatureWithoutNotify(1000);
        double targetBefore = heatTrait(target).handler.getTemperature();
        double machineBefore = heatTrait(machine).handler.getTemperature();

        heatTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.OUT);

        if (heatTrait(target).handler.getTemperature() <= targetBefore) {
            h.fail("Auto output moved no heat to the neighbour; it sits at "
                    + heatTrait(target).handler.getTemperature());
            return;
        }
        if (heatTrait(machine).handler.getTemperature() >= machineBefore) {
            h.fail("The heat came from nowhere — the machine did not cool down");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_auto_input_draws_from_the_neighbour(GameTestHelper h) {
        var source = MBDScenario.of(h).placeMachine(PNCHeatTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(PNCHeatTraitFixtures.MACHINE_ID, POS).machine();
        if (source == null || machine == null) { h.fail("Machines were not placed"); return; }

        heatTrait(source).handler.setTemperatureWithoutNotify(1000);
        double machineBefore = heatTrait(machine).handler.getTemperature();

        heatTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.IN);

        if (heatTrait(machine).handler.getTemperature() <= machineBefore) {
            h.fail("Auto input drew no heat from the neighbour");
            return;
        }
        h.succeed();
    }

    private static PNCHeatExchangerTrait heatTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCHeatExchangerTrait heatTrait) return heatTrait;
        }
        throw new AssertionError("fixture machine has no PNC heat exchanger trait");
    }
}

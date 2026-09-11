package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class MekHeatTraitTests {
    static { @SuppressWarnings("unused") var ignored = MekHeatTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos EAST = POS.relative(Direction.EAST);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MekHeatTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.HEAT, null)
                .succeed();
    }

    /**
     * Auto IO has to find the <i>neighbour's</i> heat handler.
     *
     * <p>{@code handleAutoIO} used to resolve the capability at its own position, which made
     * {@code transfer(storage, neighbor)} a transfer into itself: the temperature delta was zero, the
     * method returned at its first guard, and no heat ever moved. Driven directly rather than through
     * {@code serverTick} so that {@code simulateEnvironment}'s ambient bleed cannot muddy the reading.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_auto_output_warms_the_neighbour(GameTestHelper h) {
        var target = MBDScenario.of(h).placeMachine(MekHeatTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(MekHeatTraitFixtures.MACHINE_ID, POS).machine();
        if (target == null || machine == null) { h.fail("Machines were not placed"); return; }

        heatTrait(machine).storage.handleHeat(0, 100_000);
        double machineBefore = heatTrait(machine).storage.getTemperature(0);
        double targetBefore = heatTrait(target).storage.getTemperature(0);

        heatTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.OUT);

        if (heatTrait(target).storage.getTemperature(0) <= targetBefore) {
            h.fail("Auto output moved no heat to the neighbour; it sits at "
                    + heatTrait(target).storage.getTemperature(0));
            return;
        }
        if (heatTrait(machine).storage.getTemperature(0) >= machineBefore) {
            h.fail("The heat came from nowhere — the machine did not cool down");
            return;
        }
        h.succeed();
    }

    /** The mirror image: IN has to pull from the neighbour rather than from itself. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void heat_auto_input_draws_from_the_neighbour(GameTestHelper h) {
        var source = MBDScenario.of(h).placeMachine(MekHeatTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(MekHeatTraitFixtures.MACHINE_ID, POS).machine();
        if (source == null || machine == null) { h.fail("Machines were not placed"); return; }

        heatTrait(source).storage.handleHeat(0, 100_000);
        double machineBefore = heatTrait(machine).storage.getTemperature(0);

        heatTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.IN);

        if (heatTrait(machine).storage.getTemperature(0) <= machineBefore) {
            h.fail("Auto input drew no heat from the neighbour");
            return;
        }
        h.succeed();
    }

    private static MekHeatCapabilityTrait heatTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof MekHeatCapabilityTrait heatTrait) return heatTrait;
        }
        throw new AssertionError("fixture machine has no mekanism heat trait");
    }
}

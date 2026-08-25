package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class PNCPressureTraitTests {
    static { @SuppressWarnings("unused") var ignored = PNCPressureTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void air_handler_machine_capability_exposed_on_north(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.AIR_HANDLER_MACHINE, Direction.NORTH)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void air_handler_machine_capability_exposed_on_null_side(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .assertExposes(PNCCapabilities.AIR_HANDLER_MACHINE, null)
                .succeed();
    }

    /**
     * {@code connection_io} decides which faces the air handler will connect through, and
     * {@code updateHullAirHandlers()} only recomputes that list when the machine's facing changed. So an
     * override has to reset {@code lastFront} itself, or it stays invisible until the machine is rotated
     * — air keeps flowing through a side the override just closed.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void connection_io_override_reaches_the_air_handler(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .runTicks(2)
                .check("every side connects by default",
                        m -> connectableFaces(m).containsAll(List.of(Direction.values())))
                .with(m -> pressureTrait(m).connectionIO.top.set(false))
                .runTicks(2)
                .check("the closed side drops out without waiting for a rotation",
                        m -> !connectableFaces(m).contains(Direction.UP))
                .check("and the others are untouched",
                        m -> connectableFaces(m).contains(Direction.NORTH))
                .with(m -> pressureTrait(m).connectionIO.top.clear())
                .runTicks(2)
                .check("clearing brings it back",
                        m -> connectableFaces(m).contains(Direction.UP))
                .succeed();
    }

    private static List<Direction> connectableFaces(MBDMachine machine) {
        return pressureTrait(machine).getHandler().getConnectableFaces();
    }

    private static PNCPressureAirHandlerTrait pressureTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCPressureAirHandlerTrait pressure) return pressure;
        }
        throw new AssertionError("fixture machine has no pressure trait");
    }
}

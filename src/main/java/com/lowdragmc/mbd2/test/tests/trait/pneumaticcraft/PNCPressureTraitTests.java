package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.CopiableAirHandler;
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

    /**
     * The four float runtime values, which are the only ones in the mod — and so the production check
     * that {@code RuntimeValueStorage.ofFloat} round-trips and that a fraction survives.
     *
     * <p>{@code danger} and {@code critical} reach the handler through a {@link me.desht.pneumaticcraft.api.pressure.PressureTier}
     * that reads these slots: {@code MachineAirHandler} keeps the tier it was constructed with forever
     * and calls through it on every query, so a tier built from the definition's fields — as it used to
     * be — could never be overridden per machine.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pressure_threshold_overrides_reach_the_air_handler(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .check("max pressure starts on the definition",
                        m -> handler(m).maxPressure() == 10f)
                // zero means "same as max pressure" on the definition, and must keep meaning that here
                .check("danger pressure defaults to the max",
                        m -> handler(m).getDangerPressure() == 10f)
                .check("and critical follows danger",
                        m -> handler(m).getCriticalPressure() == 10f)
                .with(m -> pressureTrait(m).maxPressure.set(12.5f))
                .check("a fractional max pressure should reach the handler",
                        m -> handler(m).maxPressure() == 12.5f)
                .check("and danger should follow it while unset",
                        m -> handler(m).getDangerPressure() == 12.5f)
                .with(m -> pressureTrait(m).dangerPressure.set(8.5f))
                .check("an explicit danger pressure should win",
                        m -> handler(m).getDangerPressure() == 8.5f)
                .check("and critical should follow danger while unset",
                        m -> handler(m).getCriticalPressure() == 8.5f)
                .with(m -> pressureTrait(m).criticalPressure.set(9.25f))
                .check("an explicit critical pressure should win too",
                        m -> handler(m).getCriticalPressure() == 9.25f)
                .assertPersistenceRoundTrip()
                .check("the fractions should survive a save/load cycle",
                        m -> handler(m).maxPressure() == 12.5f
                                && handler(m).getDangerPressure() == 8.5f
                                && handler(m).getCriticalPressure() == 9.25f)
                .with(m -> {
                    pressureTrait(m).maxPressure.clear();
                    pressureTrait(m).dangerPressure.clear();
                    pressureTrait(m).criticalPressure.clear();
                })
                .check("clearing should go back to the definition",
                        m -> handler(m).maxPressure() == 10f && handler(m).getCriticalPressure() == 10f)
                .succeed();
    }

    /**
     * Shrinking the tank scales the stored air down with it, the way PneumaticCraft's own
     * {@code setVolumeUpgrades} does — keeping the air instead would raise the pressure, so "give this
     * machine a smaller tank" would mean "make this machine explode".
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void volume_override_keeps_the_pressure_when_shrinking(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS)
                .check("the tank starts at its authored volume", m -> handler(m).getVolume() == 2000)
                .with(m -> handler(m).addAir(4000))
                .check("2 bar in a 2000mL tank", m -> handler(m).getPressure() == 2f)
                .with(m -> pressureTrait(m).volume.set(1000))
                .check("the volume override should reach the handler", m -> handler(m).getVolume() == 1000)
                .check("and the pressure should be unchanged, not doubled",
                        m -> handler(m).getPressure() == 2f)
                .assertPersistenceRoundTrip()
                .check("the volume override should survive a save/load cycle",
                        m -> handler(m).getVolume() == 1000)
                .with(m -> pressureTrait(m).volume.clear())
                .check("clearing should go back to the definition", m -> handler(m).getVolume() == 2000)
                .succeed();
    }

    private static CopiableAirHandler handler(MBDMachine machine) {
        return pressureTrait(machine).getHandler();
    }

    private static List<Direction> connectableFaces(MBDMachine machine) {
        return handler(machine).getConnectableFaces();
    }

    private static PNCPressureAirHandlerTrait pressureTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCPressureAirHandlerTrait pressure) return pressure;
        }
        throw new AssertionError("fixture machine has no pressure trait");
    }
}

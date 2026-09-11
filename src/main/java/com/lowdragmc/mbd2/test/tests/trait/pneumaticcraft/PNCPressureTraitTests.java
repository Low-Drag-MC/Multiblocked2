package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
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
    private static final BlockPos EAST = POS.relative(Direction.EAST);

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

    /**
     * Auto IO has to reach the neighbour's air handler. Resolving the capability at its own position
     * ran {@code transferAir} source-against-source, so the pressure comparison was never greater and
     * no air moved.
     *
     * <p>Driven by calling {@code handleAutoIO} rather than by ticking: {@code serverTick} also runs
     * {@code handler.tick(holder)}, and PneumaticCraft's own machine network equalises adjacent air
     * handlers by itself. A ticked test would pass regardless of what auto IO did.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void air_auto_output_pushes_to_the_neighbour(GameTestHelper h) {
        var target = MBDScenario.of(h).placeMachine(PNCPressureTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS).machine();
        if (target == null || machine == null) { h.fail("Machines were not placed"); return; }

        handler(machine).addAir(8000);
        int targetBefore = handler(target).getAir();
        int machineBefore = handler(machine).getAir();

        pressureTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.OUT);

        if (handler(target).getAir() <= targetBefore) {
            h.fail("Auto output moved no air to the neighbour; it holds " + handler(target).getAir());
            return;
        }
        if (handler(machine).getAir() >= machineBefore) {
            h.fail("The air came from nowhere — the machine did not lose any");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void air_auto_input_pulls_from_the_neighbour(GameTestHelper h) {
        var source = MBDScenario.of(h).placeMachine(PNCPressureTraitFixtures.MACHINE_ID, EAST).machine();
        var machine = MBDScenario.of(h).placeMachine(PNCPressureTraitFixtures.MACHINE_ID, POS).machine();
        if (source == null || machine == null) { h.fail("Machines were not placed"); return; }

        handler(source).addAir(8000);
        int machineBefore = handler(machine).getAir();

        pressureTrait(machine).handleAutoIO(h.absolutePos(POS), Direction.EAST, IO.IN);

        if (handler(machine).getAir() <= machineBefore) {
            h.fail("Auto input pulled no air from the neighbour");
            return;
        }
        h.succeed();
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

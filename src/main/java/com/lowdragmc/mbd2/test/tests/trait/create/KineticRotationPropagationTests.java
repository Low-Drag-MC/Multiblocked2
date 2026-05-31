package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests that the kinetic-machine integration actually participates in Create's rotation network:
 * generator-mode machines produce rotation when scheduled, and that rotation propagates to
 * adjacent Create shafts placed on the correct axis. This is the difference between "kinetic
 * trait is registered" (covered elsewhere) and "the BE works as a real generator."
 */
@GameTestHolder(MBD2.MOD_ID)
public class KineticRotationPropagationTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.GENERATOR_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Place a generator kinetic machine, schedule it to produce rotation, then assert that
     * the BE's speed actually becomes non-zero (i.e., updateGeneratedRotation + applyNewSpeed
     * + attachKinetics correctly wires it into a kinetic network).
     *
     * The generator fixture has torque=8, maxRPM=256; scheduling 1000 stress should yield
     * speed = min(256, 1000/8) = 125 RPM.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void generator_machine_rotates_when_scheduled(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }
        if (kineticBE.getSpeed() != 0f) {
            h.fail("Expected initial speed 0 but got " + kineticBE.getSpeed());
            return;
        }
        // Trigger generator-side scheduling. torque=8 -> expected speed = 1000/8 = 125 RPM
        float scheduledStress = kineticBE.scheduleWorking(1000f, false);
        if (scheduledStress <= 0f) {
            h.fail("scheduleWorking returned non-positive: " + scheduledStress);
            return;
        }
        if (kineticBE.getSpeed() != 125f) {
            h.fail("Expected speed=125 after scheduleWorking(1000), got " + kineticBE.getSpeed());
            return;
        }
        if (!kineticBE.hasNetwork()) {
            h.fail("Generator BE has no kinetic network after scheduleWorking");
            return;
        }
        h.succeed();
    }

    /**
     * Place a generator kinetic machine + an adjacent Create shaft on the rotation axis,
     * trigger generation, and assert the shaft picks up rotation from the network.
     *
     * The machine's default front facing is NORTH with FRONT rotation facing -> rotation axis = Z.
     * So the shaft must sit at POS.north() (or south) with its own axis set to Z.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void generator_drives_adjacent_shaft(GameTestHelper h) {
        BlockPos shaftPos = POS.relative(Direction.NORTH);
        var shaftState = AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z);

        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .placeBlock(shaftPos, shaftState)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }
        var shaftBE = h.getBlockEntity(shaftPos);
        if (!(shaftBE instanceof KineticBlockEntity shaftKineticBE)) {
            h.fail("Shaft BE is not a KineticBlockEntity (placement may be wrong, got " + shaftBE + ")");
            return;
        }

        // Schedule the generator to produce rotation. attachKinetics() inside applyNewSpeed
        // should propagate immediately to the shaft via RotationPropagator.handleAdded.
        kineticBE.scheduleWorking(1000f, false);

        // Tick once so any deferred propagation completes.
        MBDScenario.of(h).runTicks(2);

        // Re-fetch the shaft BE in case Create rebuilt it via the propagator.
        shaftBE = h.getBlockEntity(shaftPos);
        if (!(shaftBE instanceof KineticBlockEntity refreshedShaft)) {
            h.fail("Shaft BE disappeared after propagation");
            return;
        }
        float shaftSpeed = refreshedShaft.getSpeed();
        if (shaftSpeed == 0f) {
            h.fail("Shaft did not pick up rotation from the generator. "
                    + "Generator speed=" + kineticBE.getSpeed()
                    + ", generator hasNetwork=" + kineticBE.hasNetwork()
                    + ", shaft hasSource=" + refreshedShaft.hasSource()
                    + ", shaft hasNetwork=" + refreshedShaft.hasNetwork());
            return;
        }
        // Both should share the same speed magnitude (Create may flip sign for orientation,
        // so just assert non-zero magnitude rather than exact equality).
        if (Math.abs(shaftSpeed) != Math.abs(kineticBE.getSpeed())) {
            h.fail("Shaft speed magnitude " + Math.abs(shaftSpeed)
                    + " does not match generator speed magnitude " + Math.abs(kineticBE.getSpeed()));
            return;
        }
        h.succeed();
    }

    /**
     * Stopping a running generator (via stopWorking) should also tear down the network -
     * verify that the adjacent shaft's speed drops back to 0.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void stopping_generator_stops_adjacent_shaft(GameTestHelper h) {
        BlockPos shaftPos = POS.relative(Direction.NORTH);
        var shaftState = AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z);

        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .placeBlock(shaftPos, shaftState)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity");
            return;
        }

        kineticBE.scheduleWorking(1000f, false);
        MBDScenario.of(h).runTicks(2);

        var shaftBE = h.getBlockEntity(shaftPos);
        if (!(shaftBE instanceof KineticBlockEntity shaftKineticBE) || shaftKineticBE.getSpeed() == 0f) {
            h.fail("Precondition failed: shaft did not receive rotation before stop");
            return;
        }

        kineticBE.stopWorking();
        MBDScenario.of(h).runTicks(2);

        shaftBE = h.getBlockEntity(shaftPos);
        if (shaftBE instanceof KineticBlockEntity stoppedShaft && stoppedShaft.getSpeed() != 0f) {
            h.fail("Shaft still rotating after stopWorking: " + stoppedShaft.getSpeed());
            return;
        }
        if (kineticBE.getSpeed() != 0f) {
            h.fail("Generator still rotating after stopWorking: " + kineticBE.getSpeed());
            return;
        }
        h.succeed();
    }
}

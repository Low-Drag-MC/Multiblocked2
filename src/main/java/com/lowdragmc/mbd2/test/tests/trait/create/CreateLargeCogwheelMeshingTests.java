package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.create.machine.ConfigKineticMachineSettings;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlock;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine set to {@code ConnectionType.LARGE_COGWHEEL} answers {@code ICogWheel.isLargeCog() == true},
 * which sends Create's propagator down code paths that read {@code BlockStateProperties.AXIS} straight
 * off the block state. MBD machine states never carry that property — they store orientation in their
 * own rotation state — so every one of those reads used to throw
 * {@code IllegalArgumentException: Cannot get property axis ... as it does not exist} on the server
 * thread, crashing the world as soon as a large cogwheel was placed within meshing range.
 *
 * <p>Fixed by giving large-cog machines the {@code axis} property Create's contract implies
 * ({@link MBDKineticMachineBlock#AXIS}). These tests pin both halves: that nothing throws, and that
 * the axis carried is the right one — a wrong-but-non-throwing value would silently stop the cogs
 * from meshing, which is the failure mode that makes a derived property worth testing.</p>
 *
 * @see MBDKineticMachineBlock#withRotationAxis(net.minecraft.world.level.block.state.BlockState)
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateLargeCogwheelMeshingTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.LARGE_COG_GENERATOR_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Large-to-large meshing needs the two cogs on different axes, diagonally offset in the plane
     * spanned by those two axes. The machine's default facing is NORTH with FRONT rotation, so its
     * rotation axis is Z; pairing it with an axis=Y cogwheel means the offset has to be non-zero on
     * Y and Z and zero on X.
     */
    private static final BlockPos DIAGONAL_COG = POS.offset(0, 1, 1);

    /** The exact shape from the crash report: a Create large cogwheel ticking next to the machine. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void neighbouring_large_cogwheel_does_not_crash_the_propagator(GameTestHelper h) {
        var cogState = AllBlocks.LARGE_COGWHEEL.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);

        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.LARGE_COG_CONSUMER_ID, POS)
                .placeBlock(DIAGONAL_COG, cogState)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity machineBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var cogBE = h.getBlockEntity(DIAGONAL_COG);
        if (!(cogBE instanceof KineticBlockEntity cogKineticBE)) {
            h.fail("Large cogwheel BE is not a KineticBlockEntity (got " + cogBE + ")"); return;
        }

        // Before the fix this threw out of KineticBlockEntity#tick -> attachKinetics.
        MBDScenario.of(h).runTicks(3);

        // Reaching the axis fallback is not enough — it has to report Z for a NORTH-facing machine,
        // or Create decides the gears do not mesh at all.
        if (!RotationPropagator.isConnected(cogKineticBE, machineBE)
                && !RotationPropagator.isConnected(machineBE, cogKineticBE)) {
            h.fail("Diagonal large cogwheel should mesh with a LARGE_COGWHEEL machine on a different axis");
            return;
        }
        h.succeed();
    }

    /** The machine driving the network, i.e. the propagation direction the machine itself starts. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void large_cog_generator_drives_diagonal_large_cogwheel(GameTestHelper h) {
        var cogState = AllBlocks.LARGE_COGWHEEL.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);

        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.LARGE_COG_GENERATOR_ID, POS)
                .placeBlock(DIAGONAL_COG, cogState)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }

        // torque=8 -> speed = 1000/8 = 125 RPM, and large-to-large meshing keeps the magnitude.
        kineticBE.scheduleWorking(1000f, false);
        MBDScenario.of(h).runTicks(2);

        var cogBE = h.getBlockEntity(DIAGONAL_COG);
        if (!(cogBE instanceof KineticBlockEntity cogKineticBE)) {
            h.fail("Large cogwheel BE disappeared after propagation"); return;
        }
        if (cogKineticBE.getSpeed() == 0f) {
            h.fail("Diagonal large cogwheel did not pick up rotation. "
                    + "Machine speed=" + kineticBE.getSpeed()
                    + ", machine hasNetwork=" + kineticBE.hasNetwork()
                    + ", cog hasSource=" + cogKineticBE.hasSource());
            return;
        }
        if (Math.abs(cogKineticBE.getSpeed()) != Math.abs(kineticBE.getSpeed())) {
            h.fail("Large-to-large meshing should keep the speed magnitude: cog="
                    + cogKineticBE.getSpeed() + ", machine=" + kineticBE.getSpeed());
            return;
        }
        h.succeed();
    }

    /**
     * Large-to-small meshing reads the axis off the large cog, so it crashes through
     * {@code isLargeToSmallCog} instead. A small cog meshes with a large one when it sits on the same
     * axis, offset diagonally in the other two.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void large_cog_generator_drives_diagonal_small_cogwheel(GameTestHelper h) {
        // Machine rotation axis is Z; the small cog shares it and offsets on X and Y.
        BlockPos smallCogPos = POS.offset(1, 1, 0);
        var cogState = AllBlocks.COGWHEEL.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);

        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.LARGE_COG_GENERATOR_ID, POS)
                .placeBlock(smallCogPos, cogState)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }

        kineticBE.scheduleWorking(1000f, false);
        MBDScenario.of(h).runTicks(2);

        var cogBE = h.getBlockEntity(smallCogPos);
        if (!(cogBE instanceof KineticBlockEntity cogKineticBE)) {
            h.fail("Small cogwheel BE disappeared after propagation"); return;
        }
        // Large -> small steps the speed up by 2.
        float expected = Math.abs(kineticBE.getSpeed()) * 2f;
        if (Math.abs(cogKineticBE.getSpeed()) != expected) {
            h.fail("Small cogwheel should spin at 2x the large cog: expected " + expected
                    + ", got " + cogKineticBE.getSpeed()
                    + " (machine speed=" + kineticBE.getSpeed() + ")");
            return;
        }
        h.succeed();
    }

    /**
     * {@code axis} is derived from the facing, so it has to be re-derived every time the facing moves.
     * A stale axis does not throw — the cog just stops meshing — so nothing else would catch this.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void axis_follows_the_front_facing(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.LARGE_COG_CONSUMER_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }

        // Default facing NORTH with FRONT rotation -> rotation axis Z.
        if (h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.Z) {
            h.fail("Freshly placed NORTH-facing machine should carry axis=Z, got "
                    + h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS));
            return;
        }

        machine.setFrontFacing(Direction.EAST);
        if (h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.X) {
            h.fail("setFrontFacing(EAST) should leave axis=X, got "
                    + h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS));
            return;
        }

        // Contraptions rotate block states wholesale, so Block#rotate has to keep it in step too.
        var rotated = h.getBlockState(POS).rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        if (rotated.getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.Z) {
            h.fail("Rotating an EAST-facing machine 90 degrees should leave axis=Z, got "
                    + rotated.getValue(MBDKineticMachineBlock.AXIS));
            return;
        }
        h.succeed();
    }

    /**
     * Machines saved before {@code axis} existed load with the block's default value while their
     * facing restores correctly, so the block entity re-derives it on its first tick.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void stale_axis_from_an_older_save_is_repaired_on_load(GameTestHelper h) {
        var definition = MBDRegistries.MACHINE_DEFINITIONS.get(CreateKineticMachineFixtures.LARGE_COG_CONSUMER_ID);
        if (definition == null) { h.fail("Large-cog fixture is not registered"); return; }
        // Facing stays NORTH (axis Z); the axis is deliberately wrong, as an old palette entry would load.
        var stale = definition.block().defaultBlockState()
                .setValue(MBDKineticMachineBlock.AXIS, Direction.Axis.X);

        MBDScenario.of(h).placeBlock(POS, stale).runTicks(2);

        var repaired = h.getBlockState(POS);
        if (repaired.getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.Z) {
            h.fail("Stale axis should have been re-derived to Z on the first tick, got "
                    + repaired.getValue(MBDKineticMachineBlock.AXIS));
            return;
        }
        h.succeed();
    }

    /**
     * A block's state definition is frozen at mod construction, but {@code kineticMachineSettings} is
     * not — {@code /mbd2 reload_machine_projects} re-reads it into the live definition. So the axis
     * property has to be on every kinetic machine, not only the ones that were large cogs at startup,
     * and already-placed machines have to notice when the reload moves their rotation facing.
     *
     * <p>Mutating the live settings is exactly what that command does; this fixture is not shared with
     * any other test, and the settings are put back before returning.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void definition_reload_takes_effect_without_a_restart(GameTestHelper h) {
        var definition = MBDRegistries.MACHINE_DEFINITIONS.get(CreateKineticMachineFixtures.RUNTIME_RELOAD_MACHINE_ID);
        if (!(definition instanceof CreateKineticMachineDefinition kineticDefinition)) {
            h.fail("Runtime-reload fixture is not a CreateKineticMachineDefinition"); return;
        }
        var settings = kineticDefinition.kineticMachineSettings();
        var previousConnection = settings.connectionType;
        var previousRotation = settings.frontRotation;
        try {
            // Registered as SHAFT, yet it still carries the axis — otherwise the flip below could
            // never take effect, because the property cannot appear at runtime.
            if (!definition.block().defaultBlockState().hasProperty(MBDKineticMachineBlock.AXIS)) {
                h.fail("Every kinetic machine should carry the axis property, even a plain SHAFT one");
                return;
            }

            settings.connectionType = ConfigKineticMachineSettings.ConnectionType.LARGE_COGWHEEL;

            var cogState = AllBlocks.LARGE_COGWHEEL.getDefaultState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
            var machine = MBDScenario.of(h)
                    .placeMachine(CreateKineticMachineFixtures.RUNTIME_RELOAD_MACHINE_ID, POS)
                    .placeBlock(DIAGONAL_COG, cogState)
                    .machine();
            if (machine == null) { h.fail("Machine was not placed"); return; }
            if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity machineBE)) {
                h.fail("BE is not MBDKineticMachineBlockEntity"); return;
            }
            if (!(h.getBlockEntity(DIAGONAL_COG) instanceof KineticBlockEntity cogBE)) {
                h.fail("Large cogwheel BE is not a KineticBlockEntity"); return;
            }
            MBDScenario.of(h).runTicks(3);
            if (!RotationPropagator.isConnected(cogBE, machineBE)
                    && !RotationPropagator.isConnected(machineBE, cogBE)) {
                h.fail("Switching connectionType to LARGE_COGWHEEL should mesh without a restart");
                return;
            }

            // Same story for the rotation facing: FRONT on a NORTH-facing machine is axis Z, UP is Y.
            // An already-placed machine has to pick that up, which is what the lazy tick is for.
            settings.frontRotation = ConfigKineticMachineSettings.RotationFacing.UP;
            MBDScenario.of(h).runTicks(12);
            if (h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.Y) {
                h.fail("A placed machine should re-derive its axis after the rotation facing changed, got "
                        + h.getBlockState(POS).getValue(MBDKineticMachineBlock.AXIS));
                return;
            }
            h.succeed();
        } finally {
            settings.connectionType = previousConnection;
            settings.frontRotation = previousRotation;
        }
    }
}

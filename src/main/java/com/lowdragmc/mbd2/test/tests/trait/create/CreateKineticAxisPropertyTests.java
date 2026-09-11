package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlock;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * {@code axis} is on every kinetic machine, including plain shaft ones, so that switching a machine to
 * {@code LARGE_COGWHEEL} can take effect without a restart. Create treats {@code hasProperty(AXIS)} as
 * "this block is pillar-shaped" in a handful of places that have nothing to do with cogwheels, so this
 * pins the ones that could otherwise change behaviour under machines that are not cogs at all.
 *
 * @see MBDKineticMachineBlock#AXIS
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateKineticAxisPropertyTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.CONSUMER_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * {@code IRotate extends IWrenchable}, and {@code IWrenchable#getRotatedBlockState} reaches for
     * {@code AXIS} before it would reach for a facing. Without an override the wrench would spin the
     * derived axis and leave the machine pointing the same way.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void wrench_turns_the_machine_not_just_the_axis(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var state = h.getBlockState(POS);
        if (!(state.getBlock() instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock"); return;
        }

        // Default NON_Y_AXIS machine faces NORTH; a wrench on the top face turns it clockwise to EAST.
        var wrenched = kineticBlock.getRotatedBlockState(state, Direction.UP);
        var facing = kineticBlock.getFrontFacing(wrenched).orElse(null);
        if (facing != Direction.EAST) {
            h.fail("Wrenching the top face should turn a NORTH-facing machine to EAST, got " + facing);
            return;
        }
        if (wrenched.getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.X) {
            h.fail("The axis should follow the new facing (EAST -> X), got "
                    + wrenched.getValue(MBDKineticMachineBlock.AXIS));
            return;
        }
        h.succeed();
    }

    /**
     * {@code StructureTransform#apply} checks {@code AXIS} before anything that rotates a facing, so a
     * machine on a bearing would ride round without turning. {@code TransformableBlock} takes that back.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void contraption_rotation_turns_the_machine(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var state = h.getBlockState(POS);
        if (!(state.getBlock() instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock"); return;
        }

        var transform = new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, null);
        var transformed = transform.apply(state);
        var facing = kineticBlock.getFrontFacing(transformed).orElse(null);
        if (facing != Direction.EAST) {
            h.fail("A quarter turn should take a NORTH-facing machine to EAST, got " + facing);
            return;
        }
        if (transformed.getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.X) {
            h.fail("The axis should follow the rotated facing (EAST -> X), got "
                    + transformed.getValue(MBDKineticMachineBlock.AXIS));
            return;
        }

        // A mirror with no rotation has to work on its own, since Create hands both through the
        // same hook and skips its own mirror handling once we take it.
        var mirrored = new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.NONE, Mirror.FRONT_BACK)
                .apply(kineticBlock.withFrontFacing(state, Direction.EAST));
        if (kineticBlock.getFrontFacing(mirrored).orElse(null) != Direction.WEST) {
            h.fail("Mirroring an EAST-facing machine should give WEST, got "
                    + kineticBlock.getFrontFacing(mirrored).orElse(null));
            return;
        }

        // Block#mirror itself, which a vanilla structure block goes through and StructureTransform does
        // not. It was left on Block's no-op default while rotate was overridden, so a mirrored
        // structure used to bring every machine back facing the way it started.
        var blockMirrored = kineticBlock.withFrontFacing(state, Direction.EAST).mirror(Mirror.FRONT_BACK);
        if (kineticBlock.getFrontFacing(blockMirrored).orElse(null) != Direction.WEST) {
            h.fail("Block#mirror should flip an EAST-facing machine to WEST, got "
                    + kineticBlock.getFrontFacing(blockMirrored).orElse(null));
            return;
        }
        if (blockMirrored.getValue(MBDKineticMachineBlock.AXIS) != Direction.Axis.X) {
            h.fail("The axis should follow the mirrored facing, got "
                    + blockMirrored.getValue(MBDKineticMachineBlock.AXIS));
            return;
        }
        h.succeed();
    }

    /**
     * The question the property raises: a shaft machine now looks pillar-shaped to Create. It must not
     * look like a <i>cogwheel</i>, which is what actually governs whether a player may place one beside
     * it — {@code isValidCogwheelPosition} only rejects neighbours that are cogs.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void a_shaft_machine_does_not_block_cogwheel_placement(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var state = h.getBlockState(POS);
        if (ICogWheel.isSmallCog(state) || ICogWheel.isLargeCog(state)) {
            h.fail("A SHAFT machine must not report as any kind of cogwheel"); return;
        }

        // Both cogwheel sizes, right beside the machine, on each axis perpendicular to the offset.
        var absolute = h.absolutePos(POS.east());
        for (var large : new boolean[]{false, true}) {
            for (var axis : Direction.Axis.values()) {
                if (!CogWheelBlock.isValidCogwheelPosition(large, h.getLevel(), absolute, axis)) {
                    h.fail("A " + (large ? "large" : "small") + " cogwheel on axis " + axis
                            + " should be placeable next to a shaft machine");
                    return;
                }
            }
        }

        // And the cog actually survives being put there.
        var cogPos = POS.east();
        MBDScenario.of(h).placeBlock(cogPos,
                AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        MBDScenario.of(h).runTicks(2);
        if (!(h.getBlockState(cogPos).getBlock() instanceof CogWheelBlock)) {
            h.fail("The cogwheel popped off next to a shaft machine: " + h.getBlockState(cogPos));
            return;
        }
        h.succeed();
    }

    /** Small cogs never read the axis — Create asks {@link com.simibubi.create.content.kinetics.base.IRotate} for theirs. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void small_cog_machines_mesh_without_reading_the_axis(GameTestHelper h) {
        var definition = MBDRegistries.MACHINE_DEFINITIONS.get(CreateKineticMachineFixtures.SMALL_COG_CONSUMER_ID);
        if (definition == null) { h.fail("Small-cog fixture is not registered"); return; }
        var state = definition.block().defaultBlockState();
        if (!(state.getBlock() instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock"); return;
        }
        // The axis is present because every kinetic machine carries it, but the small-cog rules in
        // RotationPropagator go through getRotationAxis, so the two must agree rather than the
        // property being authoritative.
        if (state.getValue(MBDKineticMachineBlock.AXIS) != kineticBlock.getRotationAxis(state)) {
            h.fail("Stored axis " + state.getValue(MBDKineticMachineBlock.AXIS)
                    + " disagrees with getRotationAxis " + kineticBlock.getRotationAxis(state));
            return;
        }
        h.succeed();
    }
}

package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlock;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the block-side wiring that determines whether an MBD kinetic machine accepts shaft
 * input, small-cogwheel meshing, large-cogwheel meshing, or some combination. The actual
 * rotation propagation is Create's {@code RotationPropagator}; we verify the IRotate/ICogWheel
 * contract our blocks expose so that propagator sees the right answers.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateCogwheelInputTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.SMALL_COG_CONSUMER_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Default shaft-only machine: shaft on axis, no cog meshing. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void shaft_only_block_has_shaft_no_cog(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var blockState = h.getBlockState(POS);
        var block = blockState.getBlock();
        if (!(block instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock: " + block); return;
        }
        var axis = ((IRotate) kineticBlock).getRotationAxis(blockState);
        var alongAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        if (!kineticBlock.hasShaftTowards(h.getLevel(), h.absolutePos(POS), blockState, alongAxis)) {
            h.fail("Shaft-only block should accept shaft along rotation axis"); return;
        }
        if (ICogWheel.isSmallCog(blockState)) { h.fail("Shaft-only block should NOT be a small cog"); return; }
        if (ICogWheel.isLargeCog(blockState)) { h.fail("Shaft-only block should NOT be a large cog"); return; }
        h.succeed();
    }

    /** SMALL_COGWHEEL machine: no shaft, isSmallCog=true. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void small_cog_block_reports_small_cog_no_shaft(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.SMALL_COG_CONSUMER_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var blockState = h.getBlockState(POS);
        var block = blockState.getBlock();
        if (!(block instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock"); return;
        }
        if (!ICogWheel.isSmallCog(blockState)) {
            h.fail("SMALL_COGWHEEL block should report isSmallCog=true"); return;
        }
        if (ICogWheel.isLargeCog(blockState)) {
            h.fail("SMALL_COGWHEEL block should NOT report isLargeCog=true"); return;
        }
        // Cogwheel-only mode: no shaft acceptance on any face.
        for (Direction d : Direction.values()) {
            if (kineticBlock.hasShaftTowards(h.getLevel(), h.absolutePos(POS), blockState, d)) {
                h.fail("SMALL_COGWHEEL block should NOT accept shaft on " + d); return;
            }
        }
        h.succeed();
    }

    /** LARGE_COGWHEEL machine: no shaft, isLargeCog=true (and isSmallCog must report false). */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void large_cog_block_reports_large_cog_only(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.LARGE_COG_CONSUMER_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        var blockState = h.getBlockState(POS);
        var block = blockState.getBlock();
        if (!(block instanceof MBDKineticMachineBlock kineticBlock)) {
            h.fail("Block is not MBDKineticMachineBlock"); return;
        }
        if (!ICogWheel.isLargeCog(blockState)) {
            h.fail("LARGE_COGWHEEL block should report isLargeCog=true"); return;
        }
        if (ICogWheel.isSmallCog(blockState)) {
            // ICogWheel's default isSmallCog() returns !isLargeCog(); we override to honor config.
            h.fail("LARGE_COGWHEEL block should NOT report isSmallCog=true (override must gate on config)"); return;
        }
        for (Direction d : Direction.values()) {
            if (kineticBlock.hasShaftTowards(h.getLevel(), h.absolutePos(POS), blockState, d)) {
                h.fail("LARGE_COGWHEEL block should NOT accept shaft on " + d); return;
            }
        }
        h.succeed();
    }
}

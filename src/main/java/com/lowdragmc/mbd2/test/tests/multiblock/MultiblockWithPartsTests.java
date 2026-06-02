package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests for multiblock controllers that have no traits of their own — the recipe handlers
 * come from {@link com.lowdragmc.mbd2.api.machine.IMultiPart} part machines inside the
 * structure, aggregated into the controller's capabilitiesProxy on formation.
 */
@GameTestHolder(MBD2.MOD_ID)
public class MultiblockWithPartsTests {
    static { @SuppressWarnings("unused") var ignored = MultiblockWithPartsFixtures.CONTROLLER_ID; }

    // Pattern is a 3-block line "PCS" → part / controller / stone along +X.
    private static final BlockPos CONTROLLER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PART_POS = new BlockPos(0, 1, 1);
    private static final BlockPos STONE_POS = new BlockPos(2, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void controller_recipe_runs_via_part_trait(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MultiblockWithPartsFixtures.CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(MultiblockWithPartsFixtures.PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed()
                .target(PART_POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .target(CONTROLLER_POS)
                .runTicks(40)
                .target(PART_POS)
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void pattern_requires_part_block(GameTestHelper h) {
        // Replace the part slot with plain stone — pattern's 'P' predicate must reject this,
        // and formation must fail.
        MBDScenario.of(h)
                .placeMachine(MultiblockWithPartsFixtures.CONTROLLER_ID, CONTROLLER_POS)
                .placeBlock(PART_POS, Blocks.STONE.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_on_part_keeps_part_block_and_uses_proxy_state(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MultiblockWithPartsFixtures.PROXY_CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(MultiblockWithPartsFixtures.PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        if (h.getBlockState(PART_POS).is(ProxyPartBlock.BLOCK)) {
            h.fail("Part position should not be replaced by ProxyPartBlock");
            return;
        }

        var part = getPart(h);
        if (part.isDisableRendering()) {
            h.fail("proxyWhileFormed should drive the part state, not disable part rendering");
            return;
        }
        if (part.getMachineState().getLightLevel() != 11) {
            h.fail("Part should use proxy formed light 11, got " + part.getMachineState().getLightLevel());
            return;
        }

        if (scenario.machine() instanceof MBDMultiblockMachine controller) {
            controller.setMachineState("waiting");
        } else {
            h.fail("Expected multiblock controller");
            return;
        }
        if (part.getMachineState().getLightLevel() != 13) {
            h.fail("Part should follow controller waiting state through proxy light 13, got " + part.getMachineState().getLightLevel());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_on_part_falls_back_to_part_state_when_proxy_state_missing(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MultiblockWithPartsFixtures.PROXY_CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(MultiblockWithPartsFixtures.PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        var part = getPart(h);
        if (scenario.machine() instanceof MBDMultiblockMachine controller) {
            controller.setMachineState("working");
        } else {
            h.fail("Expected multiblock controller");
            return;
        }
        part.setMachineState("working");
        if (part.getMachineState().getLightLevel() != 6) {
            h.fail("Missing proxy working state should fall back to part working light 6, got " + part.getMachineState().getLightLevel());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_on_part_clears_when_structure_invalidates(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(MultiblockWithPartsFixtures.PROXY_CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(MultiblockWithPartsFixtures.PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        var part = getPart(h);
        if (!(scenario.machine() instanceof MBDMultiblockMachine controller)) {
            h.fail("Expected multiblock controller");
            return;
        }
        controller.onStructureInvalid(false);
        part.setMachineState("working");
        if (part.getMachineState().getLightLevel() != 6) {
            h.fail("Invalidated proxy state should be cleared and part working light should be 6, got " + part.getMachineState().getLightLevel());
            return;
        }
        h.succeed();
    }

    private static MBDPartMachine getPart(GameTestHelper h) {
        if (h.getBlockEntity(PART_POS) instanceof IMachineBlockEntity machineBlockEntity &&
                machineBlockEntity.getMetaMachine() instanceof MBDPartMachine part) {
            return part;
        }
        h.fail("Expected MBDPartMachine at " + PART_POS);
        throw new AssertionError();
    }
}

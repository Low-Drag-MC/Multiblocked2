package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
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
}

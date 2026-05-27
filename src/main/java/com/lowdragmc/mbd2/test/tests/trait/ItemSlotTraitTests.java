package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests for {@link com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait}:
 * capability exposure, insert/extract, persistence.
 */
@GameTestHolder(MBD2.MOD_ID)
public class ItemSlotTraitTests {
    static { @SuppressWarnings("unused") var ignored = ItemSlotTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void item_handler_capability_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemSlotTraitFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.ItemHandler.BLOCK, null)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void insert_then_assert_present(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemSlotTraitFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.IRON_INGOT, 32))
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 32))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_items(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemSlotTraitFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.IRON_INGOT, 32))
                .insertItem(2, new ItemStack(Items.DIAMOND, 5))
                .assertPersistenceRoundTrip()
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 32))
                .assertItem(2, new ItemStack(Items.DIAMOND, 5))
                .succeed();
    }
}

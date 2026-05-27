package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class ItemDurabilityRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ItemDurabilityRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_pickaxe(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemDurabilityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000)
                .runTicks(40)
                .assertItem(1, ItemStack.EMPTY)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void durability_input_consumed_but_item_kept(GameTestHelper h) {
        // Bug regression: SizedIngredient.test() applies a count check (stack.getCount() >= sized.count()),
        // but for durability recipes the "count" is damage points to consume, not stack size required.
        // The handler must only check the ingredient predicate, ignoring count semantics.
        // Recipe consumes 10 durability per cycle (20 ticks) and produces 1 dirt.
        // After 60 ticks at least 1 cycle must have completed; tick alignment is non-deterministic
        // across runs, so we assert "at least N" rather than exact equality.
        MBDScenario.of(h)
                .placeMachine(ItemDurabilityRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.IRON_PICKAXE))
                .insertEnergy(10_000)
                .runTicks(60)
                // recipe ran at least once → some dirt produced
                .assertItemCountAtLeast(1, Items.DIRT, 1)
                // pickaxe still in slot 0 with some durability consumed
                .assertItemDamageAtLeast(0, Items.IRON_PICKAXE, 10)
                .succeed();
    }
}

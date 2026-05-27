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

/**
 * Tests for {@link com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability}.
 * Covers typed-helper recipes, tag inputs, notConsumable, chance, and stalling on missing/full slots.
 */
@GameTestHolder(MBD2.MOD_ID)
public class ItemRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ItemRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void typed_helper_recipe_runs(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.STONE, 1))
                .insertEnergy(10_000)
                .runTicks(40)
                .assertItem(2, new ItemStack(Items.DIRT, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void tag_input_matches_any_log(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.OAK_LOG, 1))
                .insertEnergy(10_000)
                .runTicks(40)
                .assertItem(2, new ItemStack(Items.STICK, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void not_consumable_input_is_kept(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.IRON_INGOT, 1))
                .insertItem(1, new ItemStack(Items.DIAMOND, 1))
                .insertEnergy(10_000)
                .runTicks(40)
                .assertItem(2, new ItemStack(Items.GOLD_INGOT, 1))
                // diamond NOT consumed
                .assertItem(1, new ItemStack(Items.DIAMOND, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void zero_chance_output_never_appears(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.OAK_SAPLING, 4))
                .insertEnergy(10_000)
                .runTicks(120)
                // chance=0 → never produced. Output slot must remain empty.
                .assertItem(2, ItemStack.EMPTY)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void no_recipe_runs_without_input(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ItemRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000)
                .runTicks(40)
                .assertItem(2, ItemStack.EMPTY)
                .succeed();
    }
}

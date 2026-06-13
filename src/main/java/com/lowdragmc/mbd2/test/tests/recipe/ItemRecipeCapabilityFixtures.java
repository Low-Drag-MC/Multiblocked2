package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

public class ItemRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_item_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_item_cap_recipes");
    public static final ResourceLocation SPLIT_STACK_MACHINE_ID = MBD2.id("test_item_cap_split_stack_machine");
    public static final ResourceLocation SPLIT_STACK_RECIPE_TYPE_ID = MBD2.id("test_item_cap_split_stack_recipes");

    public static MBDRecipeType recipeType;
    public static MBDRecipeType splitStackRecipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // direct item -> item
                .recipe("item_cap_stone_to_dirt", b -> b.inputItems(Items.STONE).outputItems(Items.DIRT).duration(20))
                // tag input
                .recipe("item_cap_logs_to_stick", b -> b.inputItems(ItemTags.LOGS, 1).outputItems(Items.STICK).duration(20))
                // not-consumable input + normal output
                .recipe("item_cap_catalyst_diamond", b -> b
                        .inputItems(Items.IRON_INGOT)
                        .notConsumable(Items.DIAMOND)
                        .outputItems(Items.GOLD_INGOT)
                        .duration(20))
                // probabilistic output (chance = 0.0 → never produced). chance(0f) must come BEFORE outputItems.
                .recipe("item_cap_zero_chance", b -> b.inputItems(Items.OAK_SAPLING).chance(0f).outputItems(Items.DIAMOND).duration(20))
                .register(event);

        splitStackRecipeType = TestRecipeTypeBuilder.of(SPLIT_STACK_RECIPE_TYPE_ID)
                .recipe("item_cap_split_stack_iron", b -> b.inputItems(Items.IRON_INGOT, 30).outputItems(Items.EMERALD).duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // input slot, output slot, energy, bound to our recipe type
        TestMachineBuilder.simple(MACHINE_ID)
                .withItemSlots(2, IO.IN)   // slots 0,1 → input (handles 1-input + 1-catalyst case)
                .withItemSlots(2, IO.OUT)  // slots 2,3 → output
                .withEnergy(100_000)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);

        TestMachineBuilder.simple(SPLIT_STACK_MACHINE_ID)
                .withItemSlots(2, IO.IN, def -> def.setAllowSameItems(false))
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(SPLIT_STACK_RECIPE_TYPE_ID)
                .register(event);
    }
}

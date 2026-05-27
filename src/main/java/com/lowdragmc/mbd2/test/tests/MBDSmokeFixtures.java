package com.lowdragmc.mbd2.test.tests;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Test fixtures shared by the MBD2 smoke tests. Registers a simple machine, a multiblock
 * machine, and a recipe type with one built-in recipe.
 */
public class MBDSmokeFixtures implements TestFixtureProvider {

    public static final ResourceLocation SIMPLE_MACHINE_ID = MBD2.id("smoke_simple_machine");
    public static final ResourceLocation MULTIBLOCK_MACHINE_ID = MBD2.id("smoke_multiblock_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("smoke_recipe_type");
    public static final ResourceLocation STONE_TO_DIRT_RECIPE_ID = MBD2.id("stone_to_dirt");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe(STONE_TO_DIRT_RECIPE_ID, b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // simple single-block machine: slot 0 = input, slot 1 = output, 10k FE storage,
        // bound to our test recipe type.
        TestMachineBuilder.simple(SIMPLE_MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withEnergy(10_000)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);

        // simple multiblock controller: 3x1x3 floor of stone with the controller in the middle.
        // aisle convention: each aisle is one Z layer; each string in the aisle is one Y row; each char is one X cell.
        TestMachineBuilder.multiblock(MULTIBLOCK_MACHINE_ID)
                .withItemSlots(2)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("SSS")
                        .aisle("SCS")
                        .aisle("SSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }

    public static ItemStack stone(int count) { return new ItemStack(Items.STONE, count); }
    public static ItemStack dirt(int count) { return new ItemStack(Items.DIRT, count); }
}

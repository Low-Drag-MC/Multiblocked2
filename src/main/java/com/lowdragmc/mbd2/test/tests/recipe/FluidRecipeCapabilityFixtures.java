package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_fluid_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_fluid_cap_recipes");
    public static final ResourceLocation SPLIT_TANK_MACHINE_ID = MBD2.id("test_fluid_cap_split_tank_machine");
    public static final ResourceLocation SPLIT_TANK_RECIPE_TYPE_ID = MBD2.id("test_fluid_cap_split_tank_recipes");

    public static MBDRecipeType recipeType;
    public static MBDRecipeType splitTankRecipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // 1000 water -> dirt (item output)
                .recipe("fluid_cap_water_to_dirt", b -> b
                        .inputFluids(new FluidStack(Fluids.WATER, 1000))
                        .outputItems(Items.DIRT)
                        .duration(20))
                // 500 lava -> 500 water (distinct from above to avoid recipe-match ambiguity)
                .recipe("fluid_cap_lava_to_water", b -> b
                        .inputFluids(new FluidStack(Fluids.LAVA, 500))
                        .outputFluids(new FluidStack(Fluids.WATER, 500))
                        .duration(20))
                .register(event);

        splitTankRecipeType = TestRecipeTypeBuilder.of(SPLIT_TANK_RECIPE_TYPE_ID)
                .recipe("fluid_cap_split_tank_water", b -> b
                        .inputFluids(new FluidStack(Fluids.WATER, 1000))
                        .outputItems(Items.EMERALD)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                // tank 0 = IN (consumed by recipe), tank 1 = OUT (recipe output target)
                .withFluidTanks(1, 8000, def -> def.setRecipeHandlerIO(IO.IN))
                .withFluidTanks(1, 8000, def -> def.setRecipeHandlerIO(IO.OUT))
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);

        TestMachineBuilder.simple(SPLIT_TANK_MACHINE_ID)
                .withFluidTanks(2, 500, def -> def.setRecipeHandlerIO(IO.IN))
                .withItemSlots(1, IO.OUT)
                .withRecipeType(SPLIT_TANK_RECIPE_TYPE_ID)
                .register(event);
    }
}

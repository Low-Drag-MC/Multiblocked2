package com.lowdragmc.mbd2.test.tests.trait.ae2;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class MEInterfaceTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_ae2_me_interface_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_ae2_me_interface_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe("ae2_me_interface_split_item_input", b -> b
                        .inputItems(Items.IRON_INGOT, 30)
                        .outputItems(Items.EMERALD)
                        .duration(20))
                .recipe("ae2_me_interface_split_fluid_input", b -> b
                        .inputFluids(new FluidStack(Fluids.WATER, 1000))
                        .outputItems(Items.DIAMOND)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var meInterface = new MEInterfaceTraitDefinition();
        meInterface.setSlotSize(2);
        meInterface.setRecipeHandlerIO(IO.IN);

        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(meInterface)
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }
}

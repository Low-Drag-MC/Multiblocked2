package com.lowdragmc.mbd2.test.tests.trait.ae2;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class MEPatternProviderTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_ae2_me_pattern_provider_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_ae2_me_pattern_provider_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe("ae2_me_pattern_provider_item_output", b -> b
                        .inputItems(Items.IRON_INGOT)
                        .outputItems(Items.EMERALD)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var provider = new MEPatternProviderTraitDefinition();
        provider.setSlotSize(1);
        provider.setPatternSize(2);
        provider.setRecipeHandlerIO(IO.BOTH);
        provider.setItemCapacity(16);
        provider.setFluidCapacity(8000);

        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(provider)
                .withEnergy(100_000)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);

    }
}

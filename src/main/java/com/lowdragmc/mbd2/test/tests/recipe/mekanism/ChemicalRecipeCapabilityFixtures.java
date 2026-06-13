package com.lowdragmc.mbd2.test.tests.recipe.mekanism;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class ChemicalRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_mek_chemical_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_mek_chemical_cap_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe("mek_chemical_split_tank_hydrogen", b -> b
                        .input(MekanismChemicalRecipeCapability.CAP, hydrogen(1000))
                        .outputItems(Items.EMERALD)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var chemicalIn = new ChemicalTankCapabilityTraitDefinition();
        chemicalIn.setRecipeHandlerIO(IO.IN);
        chemicalIn.setTankSize(2);
        chemicalIn.setCapacity(500);

        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(chemicalIn)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }

    public static ChemicalStackIngredient hydrogen(long amount) {
        return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN), amount);
    }
}

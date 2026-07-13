package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;

@EmiEntrypoint
public class MBDEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MultiblockInfoEmiCategory.CATEGORY);
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registry.addCategory(MBDRecipeTypeEmiCategory.CATEGORIES.apply(recipeType));
            }
        }
        try {
            MultiblockInfoEmiCategory.registerDisplays(registry);
        } catch (NullPointerException ignored) {}
        MBDRecipeTypeEmiCategory.registerDisplays(registry);
        MultiblockInfoEmiCategory.registerWorkstations(registry);
        MBDRecipeTypeEmiCategory.registerWorkstations(registry);
    }

    public static void lookupRecipeType(MBDRecipeType recipeType) {
        EmiApi.displayRecipeCategory(MBDRecipeTypeEmiCategory.CATEGORIES.apply(recipeType));
    }

    public static void lookupIngredient(EmiIngredient ingredient, boolean isRecipe) {
        if (isRecipe) {
            EmiApi.displayRecipes(ingredient);
        } else {
            EmiApi.displayUses(ingredient);
        }
    }
}

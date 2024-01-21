package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import net.minecraft.world.item.crafting.Ingredient;

public class MBDRecipeCapabilities {

    public final static RecipeCapability<Ingredient> ITEM = ItemRecipeCapability.CAP;
    public final static RecipeCapability<FluidIngredient> FLUID = FluidRecipeCapability.CAP;

    public static void init() {
        MBDRegistries.RECIPE_CAPABILITIES.unfreeze();
        MBDRegistries.RECIPE_CAPABILITIES.register(ITEM.name, ITEM);
        MBDRegistries.RECIPE_CAPABILITIES.register(FLUID.name, FLUID);
        MBDRegistries.RECIPE_CAPABILITIES.freeze();
    }
}

package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class MBDRecipeTypeRegistryEventJS extends StartupEventJS {

    public MBDRecipeType createRecipeType(ResourceLocation id) {
        var recipeType = new MBDRecipeType(id);
        MBDRegistries.RECIPE_TYPES.register(id, recipeType);
        return recipeType;
    }

    @Nullable
    public MBDRecipeType getRecipeType(ResourceLocation id) {
        return MBDRegistries.RECIPE_TYPES.get(id);
    }

    public void removeRecipeType(ResourceLocation id) {
        MBDRegistries.RECIPE_TYPES.remove(id);
    }

}

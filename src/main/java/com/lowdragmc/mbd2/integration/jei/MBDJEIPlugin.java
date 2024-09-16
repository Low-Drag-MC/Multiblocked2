package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return MBD2.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return;
        MBD2.LOGGER.info("JEI register categories");
        var jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new MultiblockInfoCategory(jeiHelpers));
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registry.addRecipeCategories(new MBDRecipeTypeCategory(jeiHelpers, recipeType));
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return;
        MBD2.LOGGER.info("JEI register catalysts");
        MultiblockInfoCategory.registerRecipeCatalysts(registration);
        MBDRecipeTypeCategory.registerRecipeCatalysts(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return;
        MBD2.LOGGER.info("JEI register recipes");
        MultiblockInfoCategory.registerRecipes(registration);
        MBDRecipeTypeCategory.registerRecipes(registration);
//        GTOreProcessingInfoCategory.registerRecipes(registration);
//        GTOreVeinInfoCategory.registerRecipes(registration);
//        GTBedrockFluidInfoCategory.registerRecipes(registration);
    }
}

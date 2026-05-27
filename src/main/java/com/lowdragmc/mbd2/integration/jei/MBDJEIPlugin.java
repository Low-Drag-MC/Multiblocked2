package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

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
        if (LDLib2.isReiLoaded() || LDLib2.isEmiLoaded()) return;
        var jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new MultiblockInfoCategory(jeiHelpers));
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registry.addRecipeCategories(new MBDRecipeTypeCategory(jeiHelpers, recipeType));
            }
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (LDLib2.isReiLoaded() || LDLib2.isEmiLoaded()) return;
        try {
            MultiblockInfoCategory.registerRecipes(registration);
        } catch (NullPointerException ignored) {}
        MBDRecipeTypeCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (LDLib2.isReiLoaded() || LDLib2.isEmiLoaded()) return;
        MultiblockInfoCategory.registerCatalysts(registration);
        MBDRecipeTypeCategory.registerCatalysts(registration);
    }

    public static void lookupRecipeType(MBDRecipeType recipeType) {
        if (LDLibJEIPlugin.jeiRuntime == null) return;
        LDLibJEIPlugin.jeiRuntime.getRecipesGui().showTypes(List.of(MBDRecipeTypeCategory.TYPES.apply(recipeType)));
    }
}

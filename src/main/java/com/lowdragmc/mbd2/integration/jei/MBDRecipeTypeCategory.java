package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDRecipeTypeCategory extends ModularUIRecipeCategory<MBDRecipeTypeCategory.RecipeWrapper> {
    public static class RecipeWrapper extends ModularWrapper<Widget> {

        public final MBDRecipe recipe;

        public RecipeWrapper(MBDRecipe recipe) {
            super(recipe.recipeType.createRecipeUI(recipe));
            this.recipe = recipe;
        }
    }

    public static final Function<MBDRecipeType, RecipeType<RecipeWrapper>> TYPES = Util.memoize(recipeMap -> new RecipeType<>(recipeMap.getRegistryName(), RecipeWrapper.class));

    private final MBDRecipeType recipeType;
    @Getter
    private final IDrawable background;
    @Getter
    private final IDrawable icon;

    public MBDRecipeTypeCategory(IJeiHelpers helpers, MBDRecipeType recipeType) {
        this.recipeType = recipeType;
        IGuiHelper guiHelper = helpers.getGuiHelper();
        var size = recipeType.getUiSize();
        this.background = guiHelper.createBlankDrawable(size.width, size.height);
        icon = IGui2IDrawable.toDrawable(recipeType.getIcon(), 16, 16);
    }

    @Override
    public RecipeType<RecipeWrapper> getRecipeType() {
        return TYPES.apply(recipeType);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registration.addRecipes(MBDRecipeTypeCategory.TYPES.apply(recipeType),
                        Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType)
                                .stream()
                                .filter(recipe -> !recipe.isFuel && !recipe.isXEIHidden)
                                .map(RecipeWrapper::new)
                                .collect(Collectors.toList()));
            }
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var mbdRecipeType : MBDRegistries.RECIPE_TYPES) {
            if (mbdRecipeType.isXEIVisible()) {
                for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                    var recipeType = definition.recipeLogicSettings().getRecipeType();
                    if (recipeType == mbdRecipeType) {
                        registration.addRecipeCatalyst(definition.item().getDefaultInstance(), MBDRecipeTypeCategory.TYPES.apply(mbdRecipeType));
                    }
                }
            }
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(RecipeWrapper wrapper) {
        return wrapper.recipe.id;
    }
}

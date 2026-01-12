package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.lowdraglib.emi.IGui2Renderable;
import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Function;

public class MBDRecipeTypeEmiCategory extends EmiRecipeCategory {
    public static class MBDEmiRecipe extends ModularEmiRecipe<Widget> {
        final MBDRecipeTypeEmiCategory category;
        final MBDRecipe recipe;

        public MBDEmiRecipe(MBDRecipeTypeEmiCategory category, MBDRecipe recipe) {
            super(() -> recipe.recipeType.createRecipeUI(recipe));
            this.category = category;
            this.recipe = recipe;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return category;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return recipe.getId();
        }
    }

    public static final Function<MBDRecipeType, MBDRecipeTypeEmiCategory> CATEGORIES = Util.memoize(MBDRecipeTypeEmiCategory::new);
    public final MBDRecipeType recipeType;

    public MBDRecipeTypeEmiCategory(MBDRecipeType recipeType) {
        super(recipeType.getRegistryName(), IGui2Renderable.toDrawable(recipeType.getIcon(), 16,  16));
        this.recipeType = recipeType;
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType).stream()
                        .filter(recipe -> !recipe.isFuel && !recipe.isXEIHidden)
                        .map(recipe -> new MBDEmiRecipe(CATEGORIES.apply(recipeType), recipe))
                        .forEach(registry::addRecipe);
            }
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        for (var mbdRecipeType : MBDRegistries.RECIPE_TYPES) {
            if (mbdRecipeType.isXEIVisible()) {
                    for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                        var recipeType = definition.recipeLogicSettings().getRecipeType();
                        if (recipeType == mbdRecipeType) {
                            registry.addWorkstation(MBDRecipeTypeEmiCategory.CATEGORIES.apply(mbdRecipeType), EmiStack.of(definition.item()));
                        }
                    }
            }
        }
    }

    @Override
    public Component getName() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey());
    }
}

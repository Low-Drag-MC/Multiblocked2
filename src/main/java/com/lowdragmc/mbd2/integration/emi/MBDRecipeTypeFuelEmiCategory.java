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

public class MBDRecipeTypeFuelEmiCategory extends EmiRecipeCategory {
    public static class MBDEmiRecipe extends ModularEmiRecipe<Widget> {
        final MBDRecipeTypeFuelEmiCategory category;
        final MBDRecipe recipe;

        public MBDEmiRecipe(MBDRecipeTypeFuelEmiCategory category, MBDRecipe recipe) {
            super(() -> recipe.recipeType.createFuelUI(recipe));
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

    public static final Function<MBDRecipeType, MBDRecipeTypeFuelEmiCategory> CATEGORIES = Util.memoize(MBDRecipeTypeFuelEmiCategory::new);
    public final MBDRecipeType recipeType;

    public MBDRecipeTypeFuelEmiCategory(MBDRecipeType recipeType) {
        super(recipeType.getFuelRegistryName(), IGui2Renderable.toDrawable(recipeType.getFuelIcon(), 16,  16));
        this.recipeType = recipeType;
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible() && recipeType.isRequireFuelForWorking()) {
                Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType).stream()
                        .filter(recipe -> recipe.isFuel && !recipe.isXEIHidden)
                        .map(recipe -> new MBDEmiRecipe(CATEGORIES.apply(recipeType), recipe))
                        .forEach(registry::addRecipe);
            }
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        for (var mbdRecipeType : MBDRegistries.RECIPE_TYPES) {
            if (mbdRecipeType.isXEIVisible() && mbdRecipeType.isRequireFuelForWorking()) {
                for (var machine : MBDRegistries.MACHINE_DEFINITIONS) {
                    for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                        var recipeType = definition.recipeLogicSettings().getRecipeType();
                        if (recipeType == mbdRecipeType) {
                            registry.addWorkstation(MBDRecipeTypeFuelEmiCategory.CATEGORIES.apply(mbdRecipeType), EmiStack.of(machine.item()));
                        }
                    }

                }
            }
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("recipe_type.fuel", Component.translatable(recipeType.getRegistryName().toLanguageKey()));
    }
}

package com.lowdragmc.mbd2.integration.rei;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

public class MBDRecipeTypeFuelDisplayCategory extends ModularUIDisplayCategory<MBDRecipeTypeFuelDisplayCategory.MBDRecipeDisplay> {

    public static class MBDRecipeDisplay extends ModularDisplay<Widget> {

        private final MBDRecipe recipe;

        public MBDRecipeDisplay(MBDRecipe recipe) {
            super(() -> recipe.recipeType.createFuelUI(recipe), MBDRecipeTypeFuelDisplayCategory.CATEGORIES.apply(recipe.recipeType));
            this.recipe = recipe;
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.of(recipe.id);
        }
    }

    public static final Function<MBDRecipeType, CategoryIdentifier<MBDRecipeDisplay>> CATEGORIES = Util.memoize(recipeType -> CategoryIdentifier.of(recipeType.getFuelRegistryName()));

    private final MBDRecipeType recipeType;
    @Getter
    private final Renderer icon;
    @Getter
    private final Size size;

    public MBDRecipeTypeFuelDisplayCategory(MBDRecipeType recipeType) {
        this.recipeType = recipeType;
        var size = recipeType.getFuelUISize();
        this.size = new Size(size.width + 8, size.height + 8);
        icon = IGui2Renderer.toDrawable(recipeType.getFuelIcon());
    }

    @Override
    public CategoryIdentifier<? extends MBDRecipeDisplay> getCategoryIdentifier() {
        return CATEGORIES.apply(recipeType);
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(MBDRecipeDisplay display) {
        return getSize().width;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return Component.translatable("recipe_type.fuel", Component.translatable(recipeType.getRegistryName().toLanguageKey()));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible() && recipeType.isRequireFuelForWorking()) {
                registry.registerRecipeFiller(MBDRecipe.class, rt -> rt == recipeType, recipe -> recipe.isFuel && !recipe.isXEIHidden, MBDRecipeDisplay::new);
            }
        }
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (var mbdRecipeType : MBDRegistries.RECIPE_TYPES) {
            if (mbdRecipeType.isXEIVisible() && mbdRecipeType.isRequireFuelForWorking()) {
                for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                    var recipeType = definition.recipeLogicSettings().getRecipeType();
                    if (recipeType == mbdRecipeType) {
                        registry.addWorkstations(MBDRecipeTypeFuelDisplayCategory.CATEGORIES.apply(mbdRecipeType), EntryStack.of(VanillaEntryTypes.ITEM, definition.item().getDefaultInstance()));
                    }
                }
            }
        }
    }

}

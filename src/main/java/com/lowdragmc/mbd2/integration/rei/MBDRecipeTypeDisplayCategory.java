package com.lowdragmc.mbd2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplayCategory;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDRecipeTypeDisplayCategory extends ModularUIDisplayCategory<MBDRecipeTypeDisplayCategory.MBDRecipeDisplay> {

    public static final Function<MBDRecipeType, CategoryIdentifier<MBDRecipeDisplay>> CATEGORIES =
            Util.memoize(rt -> CategoryIdentifier.of(rt.getRegistryName()));

    private final MBDRecipeType recipeType;
    @Getter
    private final Renderer icon;

    public MBDRecipeTypeDisplayCategory(MBDRecipeType recipeType) {
        super(display -> ModularUI.of(display.recipe.recipeType.createRecipeUI(display.recipe)));
        this.recipeType = recipeType;
        this.icon = EntryStacks.of(pickIconItem(recipeType));
    }

    @Override
    public CategoryIdentifier<MBDRecipeDisplay> getCategoryIdentifier() {
        return CATEGORIES.apply(recipeType);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey("recipe_type"));
    }

    @Override
    public int getDisplayWidth(MBDRecipeDisplay display) {
        return recipeType.getUiSize().width;
    }

    @Override
    public int getDisplayHeight() {
        return recipeType.getUiSize().height;
    }

    public static void registerDisplays(DisplayRegistry registry) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        var recipeManager = connection.getRecipeManager();
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            var seen = new LinkedHashSet<ResourceLocation>();
            for (var holder : recipeManager.getAllRecipesFor(recipeType)) {
                var recipe = holder.value();
                if (recipe.isXEIHidden) continue;
                if (seen.add(holder.id())) {
                    registry.add(new MBDRecipeDisplay(recipe));
                }
            }
        }
    }

    public static void registerWorkstations(CategoryRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                if (definition.recipeLogicSettings().getRecipeType() == recipeType && definition.item() != null) {
                    registry.addWorkstations(CATEGORIES.apply(recipeType), EntryStacks.of(definition.item()));
                }
            }
        }
    }

    private static net.minecraft.world.level.ItemLike pickIconItem(MBDRecipeType recipeType) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition.recipeLogicSettings().getRecipeType() == recipeType && definition.item() != null) {
                return definition.item();
            }
        }
        return Items.BARRIER;
    }

    public static class MBDRecipeDisplay implements ModularUIDisplay {
        public final MBDRecipe recipe;

        public MBDRecipeDisplay(MBDRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CATEGORIES.apply(recipe.recipeType);
        }
    }
}

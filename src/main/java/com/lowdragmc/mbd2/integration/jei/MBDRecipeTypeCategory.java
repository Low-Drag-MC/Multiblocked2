package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDRecipeTypeCategory extends ModularUIRecipeCategory<MBDRecipe> {

    public static final Function<MBDRecipeType, RecipeType<MBDRecipe>> TYPES =
            Util.memoize(rt -> new RecipeType<>(rt.getRegistryName(), MBDRecipe.class));

    private final MBDRecipeType recipeType;
    @Getter
    private final IDrawable icon;

    public MBDRecipeTypeCategory(IJeiHelpers helpers, MBDRecipeType recipeType) {
        super(recipe -> ModularUI.of(recipe.recipeType.createRecipeUI(recipe)));
        this.recipeType = recipeType;
        this.icon = helpers.getGuiHelper().createDrawableItemLike(pickIconItem(recipeType));
    }

    @Override
    public RecipeType<MBDRecipe> getRecipeType() {
        return TYPES.apply(recipeType);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey("recipe_type"));
    }

    @Override
    public int getWidth() {
        return recipeType.getUiSize().width;
    }

    @Override
    public int getHeight() {
        return recipeType.getUiSize().height;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        var recipeManager = connection.getRecipeManager();
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            var recipes = new ArrayList<MBDRecipe>();
            var seen = new LinkedHashSet<ResourceLocation>();
            for (var holder : recipeManager.getAllRecipesFor(recipeType)) {
                var recipe = holder.value();
                if (recipe.isXEIHidden) continue;
                if (seen.add(holder.id())) {
                    recipes.add(recipe);
                }
            }
            registration.addRecipes(TYPES.apply(recipeType), recipes);
        }
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                if (definition.recipeLogicSettings().getRecipeType() == recipeType && definition.item() != null) {
                    registration.addRecipeCatalyst(new ItemStack(definition.item()), TYPES.apply(recipeType));
                }
            }
        }
    }

    private static ItemLike pickIconItem(MBDRecipeType recipeType) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition.recipeLogicSettings().getRecipeType() == recipeType && definition.item() != null) {
                return definition.item();
            }
        }
        return Items.BARRIER;
    }
}

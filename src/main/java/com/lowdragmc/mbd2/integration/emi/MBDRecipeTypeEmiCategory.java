package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDRecipeTypeEmiCategory extends EmiRecipeCategory {

    public static final Function<MBDRecipeType, MBDRecipeTypeEmiCategory> CATEGORIES =
            Util.memoize(MBDRecipeTypeEmiCategory::new);

    @Getter
    private final MBDRecipeType recipeType;

    public MBDRecipeTypeEmiCategory(MBDRecipeType recipeType) {
        super(recipeType.getRegistryName(), EmiStack.of(pickIconItem(recipeType)));
        this.recipeType = recipeType;
    }

    @Override
    public Component getName() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey("recipe_type"));
    }

    public static class MBDEmiRecipe extends ModularUIEMIRecipe {
        @Getter
        private final MBDRecipeTypeEmiCategory category;
        private final MBDRecipe recipe;
        private final ResourceLocation id;

        public MBDEmiRecipe(MBDRecipeTypeEmiCategory category, MBDRecipe recipe, ResourceLocation id) {
            super(self -> ModularUI.of(((MBDEmiRecipe) self).recipe.recipeType.createRecipeUI(((MBDEmiRecipe) self).recipe)));
            this.category = category;
            this.recipe = recipe;
            this.id = id;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return id;
        }

        @Override
        public int getDisplayWidth() {
            return category.recipeType.getUiSize().width;
        }

        @Override
        public int getDisplayHeight() {
            return category.recipeType.getUiSize().height;
        }
    }

    public static void registerDisplays(EmiRegistry registry) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        var recipeManager = connection.getRecipeManager();
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            var category = CATEGORIES.apply(recipeType);
            var seen = new LinkedHashSet<ResourceLocation>();
            for (var holder : recipeManager.getAllRecipesFor(recipeType)) {
                var recipe = holder.value();
                if (recipe.isXEIHidden) continue;
                if (seen.add(holder.id())) {
                    registry.addRecipe(new MBDEmiRecipe(category, recipe, holder.id()));
                }
            }
        }
    }

    public static void registerWorkstations(EmiRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (!recipeType.isXEIVisible()) continue;
            var category = CATEGORIES.apply(recipeType);
            for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
                if (definition.recipeLogicSettings().getRecipeType() == recipeType && definition.item() != null) {
                    registry.addWorkstation(category, EmiStack.of(definition.item()));
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
}

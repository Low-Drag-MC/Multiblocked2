package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote DimensionCondition, specific dimension
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "dimension", registry = "mbd2:recipe_condition")
public class DimensionCondition extends RecipeCondition {
    @Configurable(name = "recipe.condition.dimension")
    @ConfigSearch(searchConfiguratorMethod = "searchConfigurator")
    private ResourceLocation dimension = ResourceLocation.parse("dummy");

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public DimensionCondition(boolean isReverse, ResourceLocation dimension) {
        super(isReverse);
        this.dimension = dimension;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.dimension.tooltip", dimension);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null && dimension.equals(level.dimension().location());
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchConfigurator() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public @Nonnull ResourceLocation defaultValue() {
                return ResourceLocation.parse("dummy");
            }

            @Override
            public @Nonnull String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var wordLower = word.toLowerCase();
                for (var biomeEntry : Platform.getClientRegistryAccess().registry(Registries.DIMENSION_TYPE).map(Registry::keySet).orElseGet(Collections::emptySet)) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (biomeEntry.toString().contains(wordLower)) {
                        searchHandler.accept(biomeEntry);
                    }
                }
            }
        };
    }
}

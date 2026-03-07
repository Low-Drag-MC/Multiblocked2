package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
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
@LDLRegister(name = "biome", registry = "mbd2:recipe_condition")
public class BiomeCondition extends RecipeCondition {
    @Configurable(name = "recipe.condition.biome")
    @ConfigSearch(searchConfiguratorMethod = "searchConfigurator")
    private ResourceLocation biome = ResourceLocation.parse("dummy");

    public BiomeCondition(ResourceLocation biome) {
        this(false, biome);
    }

    public BiomeCondition(boolean isReverse, ResourceLocation biome) {
        super(isReverse);
        this.biome = biome;
    }

    @Override
    public String getType() {
        return "biome";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.biome.tooltip", LocalizationUtils.format("biome.%s.%s", biome.getNamespace(), biome.getPath()));
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        if (level == null) return false;
        Holder<Biome> biome = level.getBiome(recipeLogic.machine.getPos());
        return biome.is(this.biome);
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
                for (var biomeEntry : Platform.getClientRegistryAccess().registry(Registries.BIOME).map(Registry::keySet).orElseGet(Collections::emptySet)) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (biomeEntry.toString().contains(wordLower)) {
                        searchHandler.accept(biomeEntry);
                    }
                }
            }
        };
    }
}

package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@Builder
@Accessors(fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigRecipeLogicSettings implements IToggleConfigurable {
    @Builder.Default
    @Setter
    @Accessors(fluent = false)
    private boolean enable = true;
    @Builder.Default
    @Configurable(name = "editor.machine.recipe_type")
    @ConfigSearch(searchConfiguratorMethod = "searchRecipeType")
    private ResourceLocation recipeType = MBDRecipeType.DUMMY.getRegistryName();
    @Builder.Default
    @Getter
    @Configurable(subConfigurable = true)
    protected final RecipeModifier.RecipeModifiers recipeModifiers = new RecipeModifier.RecipeModifiers();
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.recipe_damping_value", tips = "config.recipe_logic_settings.recipe_damping_value.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    protected int recipeDampingValue = 2;
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.consume_inputs_after_working", tips = {
            "config.recipe_logic_settings.consume_inputs_after_working.tooltip.0",
            "config.recipe_logic_settings.consume_inputs_after_working.tooltip.1"
    })
    protected boolean consumeInputsAfterWorking = false;
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.always_search_recipe", tips = {
            "config.recipe_logic_settings.always_search_recipe.tooltip.0",
            "config.recipe_logic_settings.always_search_recipe.tooltip.1"
    })
    protected boolean alwaysSearchRecipe = false;
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.always_modify_recipe", tips = {
            "config.recipe_logic_settings.always_modify_recipe.tooltip.0",
            "config.recipe_logic_settings.always_modify_recipe.tooltip.1"
    })
    protected boolean alwaysModifyRecipe = false;

    public MBDRecipeType getRecipeType() {
        return MBDRegistries.RECIPE_TYPES.getOrDefault(recipeType, MBDRecipeType.DUMMY);
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchRecipeType() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public ResourceLocation defaultValue() {
                return MBDRecipeType.DUMMY.getRegistryName();
            }

            @Override
            public String resultText(ResourceLocation value) {
                return value.toString();
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                MBDRecipeType.searchRecipeTypeIds(word, null, searchHandler);
            }
        };
    }
}

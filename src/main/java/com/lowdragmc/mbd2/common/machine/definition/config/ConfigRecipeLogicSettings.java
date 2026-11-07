package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    /** Lowercase letters, digits and {@code _} only: it is part of saved keys, where a dot would nest. */
    @Builder.Default
    @Setter
    @Configurable(name = "config.recipe_logic_settings.name", tips = {
            "config.recipe_logic_settings.name.tooltip.0",
            "config.recipe_logic_settings.name.tooltip.1"
    })
    private String name = RecipeLogic.PRIMARY_NAME;
    @Builder.Default
    @Setter
    @Configurable(name = "config.recipe_logic_settings.threads", tips = {
            "config.recipe_logic_settings.threads.tooltip.0",
            "config.recipe_logic_settings.threads.tooltip.1"
    })
    @ConfigNumber(range = {1, 64})
    private int threads = 1;
    /**
     * Save every setting while disabled, as an additional logic must: {@link IToggleConfigurable} would save only
     * the toggle. Not saved itself; whoever makes the entry sets it.
     */
    @Builder.Default
    @Setter
    @Accessors(fluent = false)
    private boolean keepWhenDisabled = false;
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
    /** Only the definition's primary logic shows and runs these. */
    @Builder.Default
    @Persisted(subPersisted = true)
    private final AdditionalRecipeLogics additionalRecipeLogics = new AdditionalRecipeLogics();

    public MBDRecipeType getRecipeType() {
        return MBDRegistries.RECIPE_TYPES.getOrDefault(recipeType, MBDRecipeType.DUMMY);
    }

    /** The recipe logics besides this one, in the order they tick, after it. */
    public List<ConfigRecipeLogicSettings> additionalRecipeLogics() {
        return additionalRecipeLogics.logics;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        buildSettingsConfigurator(father);
        additionalRecipeLogics.buildConfigurator(father);
    }

    private void buildSettingsConfigurator(ConfiguratorGroup father) {
        IToggleConfigurable.super.buildConfigurator(father);
    }

    @Override
    public boolean skipDisableSerialize() {
        return !keepWhenDisabled;
    }

    /** Whether {@code name} may name a recipe logic: lowercase letters, digits and {@code _}. */
    public static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && name.chars().allMatch(c -> (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_');
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

    public static class AdditionalRecipeLogics implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.recipe_logic_settings.additional_recipe_logics", tips = {
                "config.recipe_logic_settings.additional_recipe_logics.tooltip.0",
                "config.recipe_logic_settings.additional_recipe_logics.tooltip.1",
        })
        @ConfigList(configuratorMethod = "logicConfigurator", addDefaultMethod = "defaultLogic")
        @ReadOnlyManaged(serializeMethod = "logicsSerialize", deserializeMethod = "logicsDeserialize")
        private final List<ConfigRecipeLogicSettings> logics = new ArrayList<>();

        protected IntTag logicsSerialize(List<ConfigRecipeLogicSettings> logics) {
            return IntTag.valueOf(logics.size());
        }

        protected List<ConfigRecipeLogicSettings> logicsDeserialize(IntTag tag) {
            var logics = new ArrayList<ConfigRecipeLogicSettings>();
            for (int i = 0; i < tag.getAsInt(); i++) {
                logics.add(defaultLogic());
            }
            return logics;
        }

        protected Configurator logicConfigurator(Supplier<ConfigRecipeLogicSettings> getter,
                                                 Consumer<ConfigRecipeLogicSettings> setter) {
            // titled: an IToggleConfigurable's enable toggle lives in the title line
            var group = new ConfiguratorGroup("config.recipe_logic_settings.additional_recipe_logic", false);
            getter.get().buildSettingsConfigurator(group);
            return group;
        }

        /** A new entry: enabled, and named {@code logic_<n>} with the first number no entry uses yet. */
        protected ConfigRecipeLogicSettings defaultLogic() {
            var names = new HashSet<String>();
            for (var logic : logics) {
                names.add(logic.name());
            }
            int n = 1;
            while (names.contains("logic_" + n)) n++;
            return ConfigRecipeLogicSettings.builder()
                    .name("logic_" + n)
                    .keepWhenDisabled(true)
                    .build();
        }
    }
}

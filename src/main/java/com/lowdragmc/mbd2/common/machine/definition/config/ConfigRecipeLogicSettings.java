package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.utils.FileUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.HashSet;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigRecipeLogicSettings implements IToggleConfigurable, IPersistedSerializable {
    @Builder.Default
    @Setter
    @Accessors(fluent = false)
    private boolean enable = true;
    @Builder.Default
    @Persisted
    private ResourceLocation recipeType = MBDRecipeType.DUMMY.getRegistryName();
    @Builder.Default
    @Getter
    protected final RecipeModifier.RecipeModifiers recipeModifiers = new RecipeModifier.RecipeModifiers();
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.recipe_damping_value", tips = "config.recipe_logic_settings.recipe_damping_value.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    protected int recipeDampingValue = 2;
    @Getter
    @Builder.Default
    @Configurable(name = "config.recipe_logic_settings.always_search_recipe", tips = {
            "config.recipe_logic_settings.always_search_recipe.tooltip.0",
            "config.recipe_logic_settings.always_search_recipe.tooltip.1"
    })
    @NumberRange(range = {0, Integer.MAX_VALUE})
    protected boolean alwaysSearchRecipe = false;

    public MBDRecipeType getRecipeType() {
        return MBDRegistries.RECIPE_TYPES.getOrDefault(recipeType, MBDRecipeType.DUMMY);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = IPersistedSerializable.super.serializeNBT(provider);
        tag.put("recipeModifiers", recipeModifiers.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(provider, tag);
        recipeModifiers.deserializeNBT(tag.getList("recipeModifiers", Tag.TAG_COMPOUND));
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IToggleConfigurable.super.buildConfigurator(father);
        // add recipe type configurator
        var candidates = new HashSet<ResourceLocation>();
        candidates.add(MBDRecipeType.DUMMY.getRegistryName());
        // add all loaded recipe types
        candidates.addAll(MBDRegistries.RECIPE_TYPES.keys());
        // add from files
        var path = new File(MBD2.getLocation(), "recipe_type");
        FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> {
            var recipeType = tag.getCompound("recipe_type").getString("registryName");
            if (!recipeType.isEmpty() && ResourceLocation.isValidPath(recipeType)) {
                candidates.add(ResourceLocation.parse(recipeType));
            }
        });

        father.addConfigurators(new SelectorConfigurator<>("editor.machine.recipe_type",
                () -> recipeType,
                (type) -> recipeType = type,
                MBDRecipeType.DUMMY.getRegistryName(),
                true,
                candidates.stream().toList(),
                ResourceLocation::toString));

        recipeModifiers.buildConfigurator(father);
    }
}

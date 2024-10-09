package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.utils.FileUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Accessors(fluent = true)
@Builder
public class ConfigMachineSettings implements IPersistedSerializable, IConfigurable {
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.machine_level", tips = "config.machine_settings.machine_level.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int machineLevel = 0;
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.has_ui", tips = "config.machine_settings.has_ui.tooltip")
    private boolean hasUI = true;
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.has_recipe_logic", tips = {
            "config.machine_settings.has_recipe_logic.tooltip.0",
            "config.machine_settings.has_recipe_logic.tooltip.1"
    })
    private boolean hasRecipeLogic = true;
    @Builder.Default
    @Persisted
    private ResourceLocation recipeType = MBDRecipeType.DUMMY.registryName;
    @Builder.Default
    @Getter
    protected final RecipeModifier.RecipeModifiers recipeModifiers = new RecipeModifier.RecipeModifiers();
    @Getter
    @Builder.Default
    @Configurable(name = "config.machine_settings.recipe_damping_value", tips = "config.machine_settings.recipe_damping_value.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    protected int recipeDampingValue = 2;
    @Singular
    @NonNull
    @Getter
    private List<TraitDefinition> traitDefinitions;

    public MBDRecipeType getRecipeType() {
        return MBDRegistries.RECIPE_TYPES.getOrDefault(recipeType, MBDRecipeType.DUMMY);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        tag.put("recipeModifiers", recipeModifiers.serializeNBT());
        var traits = new ListTag();
        for (var definition : traitDefinitions) {
            traits.add(TraitDefinition.serializeDefinition(definition));
        }
        tag.put("traitDefinitions", traits);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        recipeModifiers.deserializeNBT(tag.getList("recipeModifiers", Tag.TAG_COMPOUND));
        var traits = tag.getList("traitDefinitions", 10);
        traitDefinitions = new ArrayList<>();
        for (var i = 0; i < traits.size(); i++) {
            var trait = traits.getCompound(i);
            var definition = TraitDefinition.deserializeDefinition(trait);
            if (definition != null) {
                traitDefinitions.add(definition);
            }
        }
    }

    public void addTraitDefinition(TraitDefinition definition) {
        traitDefinitions = new ArrayList<>(traitDefinitions);
        traitDefinitions.add(definition);
    }

    public void removeTraitDefinition(TraitDefinition definition) {
        traitDefinitions = this.traitDefinitions.stream().filter(s -> s != definition).toList();
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        // add recipe type configurator
        var candidates = new HashSet<ResourceLocation>();
        candidates.add(MBDRecipeType.DUMMY.registryName);
        // add all loaded recipe types
        candidates.addAll(MBDRegistries.RECIPE_TYPES.keys());
        // add from files
        var path = new File(MBD2.getLocation(), "recipe_type");
        FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> {
            var recipeType = tag.getCompound("recipe_type").getString("registryName");
            if (!recipeType.isEmpty() && ResourceLocation.isValidResourceLocation(recipeType)) {
                candidates.add(new ResourceLocation(recipeType));
            }
        });

        father.addConfigurators(new SelectorConfigurator<>("editor.machine.recipe_type",
                () -> recipeType,
                (type) -> recipeType = type,
                MBDRecipeType.DUMMY.registryName,
                true,
                candidates.stream().toList(),
                ResourceLocation::toString));

        recipeModifiers.buildConfigurator(father);
    }
}

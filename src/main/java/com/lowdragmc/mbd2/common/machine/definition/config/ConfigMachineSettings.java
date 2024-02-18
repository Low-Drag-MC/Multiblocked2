package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
@Builder
public class ConfigMachineSettings implements IPersistedSerializable {
    @Builder.Default
    private MBDRecipeType recipeType = MBDRecipeType.DUMMY;
    @Singular
    @NonNull
    @Getter
    private List<TraitDefinition> traitDefinitions;

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
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
}

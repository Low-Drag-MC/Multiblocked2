package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTags;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.slurry.Slurry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ChemicalFilterSettings<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> implements IPersistedSerializable, IToggleConfigurable, Predicate<CHEMICAL> {
    @Getter
    @Setter
    @Persisted
    private boolean enable;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.whitelist")
    private boolean isWhitelist = true;
    @Getter
    @Setter
//    @Configurable(name = "config.definition.trait.filter.chemicals")
    @NumberRange(range = {1, 1})
    private List<CHEMICAL> filterChemicals = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.chemical_tags", forceUpdate = false)
    private List<ResourceLocation> filterTags = new ArrayList<>();

    private final ChemicalTags<CHEMICAL> chemicalTags;
    private final MekanismChemicalRecipeCapability<CHEMICAL,STACK> recipeCapability;

    public ChemicalFilterSettings(ChemicalTags<CHEMICAL> chemicalTags, MekanismChemicalRecipeCapability<CHEMICAL,STACK> recipeCapability) {
        this.chemicalTags = chemicalTags;
        this.recipeCapability = recipeCapability;
    }


    @Override
    public boolean test(CHEMICAL chemical) {
        if (!enable) {
            return true;
        }
        for (var filterChemical : filterChemicals) {
            if (filterChemical == chemical) {
                return isWhitelist;
            }
        }
        for (var filterTag : filterTags) {
            if (chemical.is(chemicalTags.tag(filterTag))) {
                return isWhitelist;
            }
        }
        return !isWhitelist;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IToggleConfigurable.super.buildConfigurator(father);
        var arrayGroup = new ArrayConfiguratorGroup<>("config.definition.trait.filter.chemicals", true, () -> filterChemicals, (getter, setter) -> {
            var configuratorGroup = new ConfiguratorGroup("");
            recipeCapability.createContentConfigurator(configuratorGroup,
                    () -> recipeCapability.createStack.apply(getter.get(), 1L),
                    stack -> setter.accept(stack.getType()));
            return configuratorGroup;
        }, true);
        arrayGroup.setOnAdd(chemical -> filterChemicals.add(chemical));
        arrayGroup.setOnRemove(chemical -> filterChemicals.remove(chemical));
        arrayGroup.setAddDefault(recipeCapability::createDefaultChemical);
        arrayGroup.setOnUpdate(list -> {
            filterChemicals.clear();
            filterChemicals.addAll(list);
        });
        father.addConfigurators(arrayGroup);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        var chemicals = new ListTag();
        for (var filterChemical : filterChemicals) {
            var nbt = new CompoundTag();
            ChemicalType.getTypeFor(filterChemical).write(nbt);
            filterChemical.write(nbt);
            chemicals.add(nbt);
        }
        tag.put("filterChemicals", chemicals);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        IPersistedSerializable.super.deserializeNBT(nbt);
        filterChemicals.clear();
        var chemicals = nbt.getList("filterChemicals", Tag.TAG_COMPOUND);
        for (var i = 0; i < chemicals.size(); i++) {
            var tag = chemicals.getCompound(i);
            var type = ChemicalType.fromNBT(tag);
            Chemical stack = null;
            if (type == ChemicalType.GAS) {
                stack = Gas.readFromNBT(tag);
            } else if (type == ChemicalType.INFUSION) {
                stack = InfuseType.readFromNBT(tag);
            } else if (type == ChemicalType.PIGMENT) {
                stack = Pigment.readFromNBT(tag);
            } else if (type == ChemicalType.SLURRY) {
                stack = Slurry.readFromNBT(tag);
            }
            if (stack != null) {
                filterChemicals.add((CHEMICAL) stack);
            }
        }
    }
}

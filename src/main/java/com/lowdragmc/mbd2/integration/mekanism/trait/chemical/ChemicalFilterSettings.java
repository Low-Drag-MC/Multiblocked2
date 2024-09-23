package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTags;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ChemicalFilterSettings<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> implements IToggleConfigurable, Predicate<CHEMICAL> {
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
    @Configurable(name = "config.definition.trait.filter.chemicals")
    @NumberRange(range = {1, 1})
    private List<STACK> filterFluids = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.chemical_tags", forceUpdate = false)
    private List<ResourceLocation> filterTags = new ArrayList<>();

    private final ChemicalTags<CHEMICAL> chemicalTags;

    public ChemicalFilterSettings(ChemicalTags<CHEMICAL> chemicalTags) {
        this.chemicalTags = chemicalTags;
    }

    @Override
    public boolean test(CHEMICAL chemical) {
        if (!enable) {
            return true;
        }
        for (var filterFluids : filterFluids) {
            if (filterFluids.isTypeEqual(chemical)) {
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
}

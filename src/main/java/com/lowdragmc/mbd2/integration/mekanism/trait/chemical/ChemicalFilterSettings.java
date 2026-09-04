package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ChemicalFilterSettings implements IToggleConfigurable, Predicate<ChemicalStack> {
    @Getter @Setter
    @Persisted
    private boolean enable;

    @Getter @Setter
    @Configurable(name = "config.definition.trait.filter.whitelist")
    private boolean isWhitelist = true;

    @Getter @Setter
    @Configurable(name = "config.definition.trait.filter.chemicals")
    private List<ResourceLocation> filterChemicals = new ArrayList<>();

    @Getter @Setter
    @Configurable(name = "config.definition.trait.filter.chemical_tags")
    @ConfigList(configuratorMethod = "buildFilterTagsConfigurator", addDefaultMethod = "addDefaultFilterTag")
    private List<ResourceLocation> filterTags = new ArrayList<>();

    @Override
    public boolean test(ChemicalStack stack) {
        return !enable || matches(stack);
    }

    /** @see com.lowdragmc.mbd2.common.trait.item.ItemFilterSettings#matches */
    public boolean matches(ChemicalStack stack) {
        var chemicalKey = MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical());
        if (chemicalKey != null) {
            for (var entry : filterChemicals) {
                if (entry.equals(chemicalKey)) return isWhitelist;
            }
        }
        for (var tagLocation : filterTags) {
            var tagKey = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, tagLocation);
            if (stack.getChemicalHolder().is(tagKey)) return isWhitelist;
        }
        return !isWhitelist;
    }

    private Configurator buildFilterTagsConfigurator(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return new SearchComponentConfigurator<>("",
                getter::get,
                setter::accept,
                new SearchComponentConfigurator.ISearchConfigurator<ResourceLocation>() {
                    @Override
                    public ResourceLocation defaultValue() {
                        return addDefaultFilterTag();
                    }

                    @Override
                    public void search(String word, IResultHandler<ResourceLocation> handler) {
                        var lower = word.toLowerCase();
                        MekanismAPI.CHEMICAL_REGISTRY.getTagNames().forEach(tagKey -> {
                            if (tagKey.location().toString().toLowerCase().contains(lower)) {
                                handler.acceptResult(tagKey.location());
                            }
                        });
                    }

                    @Override
                    public String resultText(ResourceLocation value) {
                        return value.toString();
                    }

                    @Override
                    public UIElementProvider<ResourceLocation> candidateUIProvider() {
                        return UIElementProvider.text(loc -> Component.literal("#" + loc));
                    }
                }, true);
    }

    private ResourceLocation addDefaultFilterTag() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen");
    }
}

package com.lowdragmc.mbd2.integration.mekanism.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.chemical.TagChemicalIngredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TagChemicalIngredientConfigurator extends ValueConfigurator<TagChemicalIngredient> {
    public TagChemicalIngredientConfigurator(String name,
                                             Supplier<TagChemicalIngredient> getter,
                                             Consumer<TagChemicalIngredient> setter,
                                             @NotNull TagChemicalIngredient defaultValue,
                                             boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new SearchComponentConfigurator<>("",
                () -> getter.get().tag(),
                tagKey -> updateValueActively(new TagChemicalIngredient(tagKey)),
                new SearchComponentConfigurator.ISearchConfigurator<TagKey<Chemical>>() {
                    @Override
                    public TagKey<Chemical> defaultValue() {
                        return defaultValue.tag();
                    }

                    @Override
                    public void search(String word, com.lowdragmc.lowdraglib2.utils.search.IResultHandler<TagKey<Chemical>> handler) {
                        var lower = word.toLowerCase();
                        MekanismAPI.CHEMICAL_REGISTRY.getTagNames().forEach(tagKey -> {
                            if (tagKey.location().toString().toLowerCase().contains(lower)) {
                                handler.acceptResult(tagKey);
                            }
                        });
                    }

                    @Override
                    public String resultText(TagKey<Chemical> value) {
                        return value.location().toString();
                    }

                    @Override
                    public UIElementProvider<TagKey<Chemical>> candidateUIProvider() {
                        return UIElementProvider.text(tag -> Component.literal("#" + tag.location()));
                    }
                }, true));
    }
}

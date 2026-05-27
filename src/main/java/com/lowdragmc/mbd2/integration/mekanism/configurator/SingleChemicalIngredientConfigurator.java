package com.lowdragmc.mbd2.integration.mekanism.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.chemical.SingleChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("removal")
public class SingleChemicalIngredientConfigurator extends ValueConfigurator<SingleChemicalIngredient> {
    public SingleChemicalIngredientConfigurator(String name,
                                                Supplier<SingleChemicalIngredient> getter,
                                                Consumer<SingleChemicalIngredient> setter,
                                                @NotNull SingleChemicalIngredient defaultValue,
                                                boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new RegistrySearchComponent<>("",
                () -> value.chemical().value(),
                chemical -> updateValueActively((SingleChemicalIngredient)
                        IngredientCreatorAccess.chemical().of(chemical.getAsHolder())),
                value.chemical().value(),
                true,
                MekanismAPI.CHEMICAL_REGISTRY,
                UIElementProvider.text(chemical -> Component.translatable(chemical.getTranslationKey()))
        ));
    }
}

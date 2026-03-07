package com.lowdragmc.mbd2.common.capability.recipe.configurators.fluid;

import com.lowdragmc.lowdraglib2.configurator.ui.TagKeySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TagFluidIngredientConfigurator extends ValueConfigurator<TagFluidIngredient> {
    public TagFluidIngredientConfigurator(String name,
                                          Supplier<TagFluidIngredient> getter,
                                          Consumer<TagFluidIngredient> setter,
                                          @NotNull TagFluidIngredient defaultValue,
                                          boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new TagKeySearchComponent.Fluid("",
                () -> getter.get().tag(),
                tagKey -> setter.accept(new TagFluidIngredient(tagKey)),
                defaultValue.tag(), true));
    }
}

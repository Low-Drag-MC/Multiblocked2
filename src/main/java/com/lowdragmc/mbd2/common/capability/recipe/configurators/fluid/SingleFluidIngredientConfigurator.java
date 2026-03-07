package com.lowdragmc.mbd2.common.capability.recipe.configurators.fluid;

import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.SingleFluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SingleFluidIngredientConfigurator extends ValueConfigurator<SingleFluidIngredient> {
    public SingleFluidIngredientConfigurator(String name,
                                             Supplier<SingleFluidIngredient> getter,
                                             Consumer<SingleFluidIngredient> setter,
                                             @NotNull SingleFluidIngredient defaultValue,
                                             boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new RegistrySearchComponent.Fluid("",
                () -> value.fluid().value(),
                fluid -> updateValueActively(new SingleFluidIngredient(fluid.builtInRegistryHolder())),
                Fluids.WATER,
                true
        ));
    }
}

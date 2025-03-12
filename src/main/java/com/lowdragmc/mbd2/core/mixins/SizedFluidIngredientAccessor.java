package com.lowdragmc.mbd2.core.mixins;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SizedFluidIngredient.class)
public interface SizedFluidIngredientAccessor {
    @Accessor
    void setCachedStacks(FluidStack[] cachedStacks);
}

package com.lowdragmc.mbd2.core.mixins;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FluidIngredient.class)
public interface FluidIngredientAccessor {
    @Accessor
    @Mutable
    void setStacks(FluidStack[] stacks);
}

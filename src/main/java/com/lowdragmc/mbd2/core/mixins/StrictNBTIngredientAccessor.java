package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StrictNBTIngredient.class)
public interface StrictNBTIngredientAccessor {
    @Accessor
    ItemStack getStack();
    @Accessor
    @Mutable
    void setStack(ItemStack stack);
}

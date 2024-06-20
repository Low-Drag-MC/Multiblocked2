package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IngredientAccessor {
    @Accessor
    Ingredient.Value[] getValues();
    @Accessor
    @Mutable
    void setValues(Ingredient.Value[] values);
    @Accessor
    @Mutable
    void setItemStacks(ItemStack[] itemStacks);
}

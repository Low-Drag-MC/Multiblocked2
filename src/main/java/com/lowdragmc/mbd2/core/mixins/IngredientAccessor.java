package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IngredientAccessor {
    @Accessor
    @Mutable
    void setValues(Ingredient.Value[] values);
    @Accessor
    @Mutable
    void setItemStacks(ItemStack[] itemStacks);
    @Accessor
    void setCustomIngredient(ICustomIngredient ingredient);
}

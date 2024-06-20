package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.ItemValue.class)
public interface ItemValueAccessor {
    @Accessor
    ItemStack getItem();
    @Accessor
    @Mutable
    void setItem(ItemStack item);
}

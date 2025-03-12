package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SizedIngredient.class)
public interface SizedIngredientAccessor {
    @Accessor
    @Mutable
    void setCachedStacks(ItemStack[] cachedStacks);
}

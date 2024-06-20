package com.lowdragmc.mbd2.core.mixins;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.TagValue.class)
public interface TagValueAccessor {
    @Accessor
    TagKey<Item> getTag();
    @Accessor
    @Mutable
    void setTag(TagKey<Item> item);
}

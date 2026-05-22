package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SerializerSizedIngredient implements IContentSerializer<SizedIngredient> {

    public static SerializerSizedIngredient INSTANCE = new SerializerSizedIngredient();

    private SerializerSizedIngredient() {}

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SizedIngredient of(Object o) {
        if (o instanceof SizedIngredient sizedIngredient) {
            return sizedIngredient;
        } else if (o instanceof Ingredient ingredient) {
            return new SizedIngredient(ingredient, 1);
        } else if (o instanceof ItemStack itemStack) {
            return SizedIngredient.of(itemStack.getItem(), itemStack.getCount());
        } else if (o instanceof ItemLike itemLike) {
            return SizedIngredient.of(itemLike, 1);
        } else if (o instanceof TagKey tag) {
            return SizedIngredient.of(tag, 1);
        }
        return new SizedIngredient(Ingredient.EMPTY, 1);
    }

    @Override
    public SizedIngredient copyInner(SizedIngredient content) {
        return new SizedIngredient(content.ingredient(), content.count());
    }

    @Override
    public Codec<SizedIngredient> codec() {
        return SizedIngredient.FLAT_CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SizedIngredient> streamCodec() {
        return SizedIngredient.STREAM_CODEC;
    }

    @Override
    public SizedIngredient copyWithModifier(SizedIngredient content, ContentModifier modifier) {
        var count = modifier.apply(content.count()).intValue();
        return new SizedIngredient(content.ingredient(), count);
    }

}

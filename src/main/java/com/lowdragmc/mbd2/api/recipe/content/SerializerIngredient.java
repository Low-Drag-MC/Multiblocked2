package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SerializerIngredient implements IContentSerializer<SizedIngredient> {

    public static SerializerIngredient INSTANCE = new SerializerIngredient();

    private SerializerIngredient() {}

    @Override
    public SizedIngredient of(Object o) {
        if (o instanceof SizedIngredient sizedIngredient) {
            return sizedIngredient;
        } else if (o instanceof Ingredient ingredient) {
            return new SizedIngredient(ingredient, 1);
        }
        return new SizedIngredient(Ingredient.EMPTY, 1);
    }

    @Override
    public SizedIngredient copyInner(SizedIngredient content) {
        var count = content.count();
        var ingredient = content.ingredient();
        return new SizedIngredient(ingredient, count);
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

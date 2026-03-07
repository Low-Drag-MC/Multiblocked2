package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class SerializerFluidIngredient implements IContentSerializer<SizedFluidIngredient> {

    public static SerializerFluidIngredient INSTANCE = new SerializerFluidIngredient();

    private SerializerFluidIngredient() {}

    @Override
    public SizedFluidIngredient of(Object o) {
        if (o instanceof SizedFluidIngredient sizedIngredient) {
            return sizedIngredient;
        } else if (o instanceof FluidIngredient ingredient) {
            return new SizedFluidIngredient(ingredient, 1);
        }
        return new SizedFluidIngredient(FluidIngredient.empty(), 1);
    }

    @Override
    public SizedFluidIngredient copyInner(SizedFluidIngredient content) {
        var amount = content.amount();
        var ingredient = content.ingredient();
        return new SizedFluidIngredient(ingredient, amount);
    }

    @Override
    public Codec<SizedFluidIngredient> codec() {
        return SizedFluidIngredient.FLAT_CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SizedFluidIngredient> streamCodec() {
        return SizedFluidIngredient.STREAM_CODEC;
    }

    @Override
    public SizedFluidIngredient copyWithModifier(SizedFluidIngredient content, ContentModifier modifier) {
        var amount = modifier.apply(content.amount()).intValue();
        return new SizedFluidIngredient(content.ingredient(), amount);
    }

}
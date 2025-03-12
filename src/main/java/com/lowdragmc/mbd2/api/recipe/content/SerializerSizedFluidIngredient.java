package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.EmptyFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class SerializerSizedFluidIngredient implements IContentSerializer<SizedFluidIngredient> {

    public static SerializerSizedFluidIngredient INSTANCE = new SerializerSizedFluidIngredient();

    private SerializerSizedFluidIngredient() {}

    @Override
    public SizedFluidIngredient of(Object o) {
        if (o instanceof SizedFluidIngredient ingredient) {
            return ingredient;
        }
        if (o instanceof FluidStack stack) {
            return SizedFluidIngredient.of(stack.copy());
        }
        return new SizedFluidIngredient(EmptyFluidIngredient.INSTANCE, 1);
    }

    @Override
    public SizedFluidIngredient copyInner(SizedFluidIngredient content) {
        return new SizedFluidIngredient(content.ingredient(), content.amount());
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
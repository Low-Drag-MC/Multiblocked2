package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SerializerIngredient implements IContentSerializer<SizedIngredient> {

    public static SerializerIngredient INSTANCE = new SerializerIngredient();

    private SerializerIngredient() {}

    /**
     * {@inheritDoc}
     *
     * <p>An {@link ItemStack} carries its own count, so it becomes an ingredient of that size; an
     * {@link ItemLike} has no count and becomes one. Components are not matched on — an ingredient
     * built from a stack accepts any stack of that item, which is what a recipe input means.</p>
     */
    @Override
    public SizedIngredient of(Object o) {
        if (o instanceof SizedIngredient sizedIngredient) {
            return sizedIngredient;
        } else if (o instanceof Ingredient ingredient) {
            return new SizedIngredient(ingredient, 1);
        } else if (o instanceof ItemStack stack) {
            return stack.isEmpty()
                    ? new SizedIngredient(Ingredient.EMPTY, 1)
                    : new SizedIngredient(Ingredient.of(stack), stack.getCount());
        } else if (o instanceof ItemLike itemLike) {
            return new SizedIngredient(Ingredient.of(itemLike), 1);
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

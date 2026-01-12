package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class SerializerEntityIngredient implements IContentSerializer<EntityIngredient> {

    public static SerializerEntityIngredient INSTANCE = new SerializerEntityIngredient();

    private SerializerEntityIngredient() {}

    @Override
    public EntityIngredient of(Object o) {
        if (o instanceof EntityIngredient ingredient) {
            return ingredient;
        }
        if (o instanceof EntityType<?> entityType) {
            return EntityIngredient.of(1, entityType);
        }
        if (o instanceof Entity entity) {
            return EntityIngredient.of(1, entity.getType());
        }
        return EntityIngredient.EMPTY;
    }

    @Override
    public EntityIngredient copyInner(EntityIngredient content) {
        return content.copy();
    }

    @Override
    public Codec<EntityIngredient> codec() {
        return EntityIngredient.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, EntityIngredient> streamCodec() {
        return EntityIngredient.STREAM_CODEC;
    }

    @Override
    public EntityIngredient copyWithModifier(EntityIngredient content, ContentModifier modifier) {
        if (content.isEmpty()) return content.copy();
        EntityIngredient copy = content.copy();
        copy.setCount(modifier.apply(copy.getCount()).intValue());
        return copy;
    }

    @Override
    public EntityIngredient deepCopyInner(EntityIngredient content) {
        return content.copy();
    }
}
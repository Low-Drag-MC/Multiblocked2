package com.lowdragmc.mbd2.api.recipe.content;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class SerializerEntityIngredient implements IContentSerializer<EntityIngredient> {

    public static SerializerEntityIngredient INSTANCE = new SerializerEntityIngredient();

    private SerializerEntityIngredient() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, EntityIngredient content) {
        content.toNetwork(buf);
    }

    @Override
    public EntityIngredient fromNetwork(FriendlyByteBuf buf) {
        return EntityIngredient.fromNetwork(buf);
    }

    @Override
    public EntityIngredient fromJson(JsonElement json) {
        return EntityIngredient.fromJson(json);
    }

    @Override
    public JsonElement toJson(EntityIngredient content) {
        return content.toJson();
    }

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
    public EntityIngredient copyWithModifier(EntityIngredient content, ContentModifier modifier) {
        if (content.isEmpty()) return content.copy();
        EntityIngredient copy = content.copy();
        copy.setCount(modifier.apply(copy.getCount()).intValue());
        return copy;
    }
}
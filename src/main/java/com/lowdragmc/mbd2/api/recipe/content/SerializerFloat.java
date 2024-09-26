package com.lowdragmc.mbd2.api.recipe.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.math.NumberUtils;

public class SerializerFloat implements IContentSerializer<Float> {

    public static SerializerFloat INSTANCE = new SerializerFloat();

    private SerializerFloat() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Float content) {
        buf.writeFloat(content);
    }

    @Override
    public Float fromNetwork(FriendlyByteBuf buf) {
        return buf.readFloat();
    }

    @Override
    public Tag toNBT(Float content) {
        return FloatTag.valueOf(content);
    }

    @Override
    public Float fromNBT(Tag nbt) {
        if (nbt instanceof FloatTag floatTag) {
            return floatTag.getAsFloat();
        }
        return 0f;
    }

    @Override
    public Float fromJson(JsonElement json) {
        return json.getAsFloat();
    }

    @Override
    public JsonElement toJson(Float content) {
        return new JsonPrimitive(content);
    }

    @Override
    public Float of(Object o) {
        if (o instanceof Float) {
            return (Float) o;
        } else if (o instanceof Number) {
            return ((Number) o).floatValue();
        } else if (o instanceof CharSequence) {
            return NumberUtils.toFloat(o.toString(), 1);
        }
        return 0f;
    }

    @Override
    public Float copyWithModifier(Float content, ContentModifier modifier) {
        return modifier.apply(content).floatValue();
    }

    @Override
    public Float copyInner(Float content) {
        return content;
    }

}

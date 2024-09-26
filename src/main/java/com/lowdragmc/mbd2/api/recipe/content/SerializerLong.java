package com.lowdragmc.mbd2.api.recipe.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * @author KilaBash
 * @date 2022/06/22
 * @implNote SerializerLong
 */
public class SerializerLong implements IContentSerializer<Long> {

    public static SerializerLong INSTANCE = new SerializerLong();

    private SerializerLong() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Long content) {
        buf.writeVarLong(content);
    }

    @Override
    public Long fromNetwork(FriendlyByteBuf buf) {
        return buf.readVarLong();
    }

    @Override
    public Tag toNBT(Long content) {
        return LongTag.valueOf(content);
    }

    @Override
    public Long fromNBT(Tag nbt) {
        if (nbt instanceof LongTag longTag) {
            return longTag.getAsLong();
        }
        return 0L;
    }

    @Override
    public Long fromJson(JsonElement json) {
        return json.getAsLong();
    }

    @Override
    public JsonElement toJson(Long content) {
        return new JsonPrimitive(content);
    }

    @Override
    public Long of(Object o) {
        if (o instanceof Long) {
            return (Long) o;
        } else if (o instanceof Number) {
            return ((Number) o).longValue();
        } else if (o instanceof CharSequence) {
            return NumberUtils.toLong(o.toString(), 1);
        }
        return 0L;
    }

    @Override
    public Long copyWithModifier(Long content, ContentModifier modifier) {
        return modifier.apply(content).longValue();
    }

    @Override
    public Long copyInner(Long content) {
        return content;
    }

}

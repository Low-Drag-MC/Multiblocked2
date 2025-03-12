package com.lowdragmc.mbd2.api.recipe.content;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    @Override
    public Codec<Long> codec() {
        return Codec.LONG;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Long> streamCodec() {
        return ByteBufCodecs.VAR_LONG;
    }

}

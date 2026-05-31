package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PressureAir(boolean isAir, float value) {

    public static class SerializerPressureAir implements IContentSerializer<PressureAir> {

        public static final SerializerPressureAir INSTANCE = new SerializerPressureAir();

        public static final Codec<PressureAir> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("isAir").forGetter(PressureAir::isAir),
                Codec.FLOAT.fieldOf("value").forGetter(PressureAir::value)
        ).apply(instance, PressureAir::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PressureAir> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, PressureAir::isAir,
                ByteBufCodecs.FLOAT, PressureAir::value,
                PressureAir::new
        );

        private SerializerPressureAir() {}

        @Override
        public Codec<PressureAir> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, PressureAir> streamCodec() {
            return STREAM_CODEC;
        }

        @Override
        public PressureAir of(Object o) {
            if (o instanceof PressureAir pressureAir) {
                return pressureAir;
            } else if (o instanceof Number number) {
                return new PressureAir(false, number.floatValue());
            } else if (o instanceof CharSequence) {
                var str = o.toString();
                var splits = str.split(":");
                if (splits.length == 2) {
                    try {
                        var isAir = Boolean.parseBoolean(splits[0]);
                        var value = Float.parseFloat(splits[1]);
                        return new PressureAir(isAir, value);
                    } catch (Exception ignored) {
                    }
                }
            }
            return new PressureAir(false, 0);
        }

        @Override
        public PressureAir copyWithModifier(PressureAir content, ContentModifier modifier) {
            return new PressureAir(content.isAir, modifier.apply(content.value).floatValue());
        }

        @Override
        public PressureAir copyInner(PressureAir content) {
            return content;
        }

        @Override
        public PressureAir deepCopyInner(PressureAir content) {
            return content;
        }
    }
}

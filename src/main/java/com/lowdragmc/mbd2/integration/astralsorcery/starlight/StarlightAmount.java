package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @param constellation empty for any constellation
 */
public record StarlightAmount(Optional<ResourceLocation> constellation, float amount) {
    public static final Codec<StarlightAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("constellation").forGetter(StarlightAmount::constellation),
            Codec.FLOAT.fieldOf("amount").forGetter(StarlightAmount::amount)
    ).apply(instance, StarlightAmount::new));
    public static final StreamCodec<ByteBuf, StarlightAmount> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), StarlightAmount::constellation,
            ByteBufCodecs.FLOAT, StarlightAmount::amount,
            StarlightAmount::new);

    public static StarlightAmount any(float amount) {
        return new StarlightAmount(Optional.empty(), amount);
    }

    public static StarlightAmount of(ResourceLocation constellation, float amount) {
        return new StarlightAmount(Optional.of(constellation), amount);
    }

    public StarlightAmount withAmount(float amount) {
        return new StarlightAmount(constellation, amount);
    }
}

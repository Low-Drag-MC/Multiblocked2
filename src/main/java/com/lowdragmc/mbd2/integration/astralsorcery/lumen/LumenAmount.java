package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * An id rather than Astral Sorcery's {@code LumenStack}: recipe types are built during mod construction, before
 * any {@link Lumen} is registered, so the id is only resolved when the recipe runs.
 */
public record LumenAmount(ResourceLocation lumen, int amount) {
    public static final Codec<LumenAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("lumen").forGetter(LumenAmount::lumen),
            Codec.INT.fieldOf("amount").forGetter(LumenAmount::amount)
    ).apply(instance, LumenAmount::new));
    public static final StreamCodec<ByteBuf, LumenAmount> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, LumenAmount::lumen,
            ByteBufCodecs.VAR_INT, LumenAmount::amount,
            LumenAmount::new);

    @Nullable
    public Lumen resolve() {
        return AstralSorceryIntegration.lumen(lumen);
    }

    public LumenAmount withAmount(int amount) {
        return new LumenAmount(lumen, amount);
    }
}

package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface IContentSerializer<T> {
    /**
     * deep copy of this content.
     */
    default T deepCopyInner(T content) {
        var op = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        return codec().parse(op, codec().encodeStart(op, content).getOrThrow()).getOrThrow();
    }

    T of(Object o);

    /**
     * deep copy and modify the size attribute for those Content that have the size attribute.
     */
    T copyWithModifier(T content, ContentModifier modifier);

    /**
     * copy of this content. recipe need it for searching and such things, which may be not deep copy.
     */
    T copyInner(T content);

    /**
     * Codec for this content
     */
    Codec<T> codec();

    /**
     * StreamCodec for this content
     */
    StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();
}

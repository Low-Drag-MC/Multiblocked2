package com.lowdragmc.mbd2.api.recipe.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface IContentSerializer<T> {

    default void toNetwork(RegistryFriendlyByteBuf buf, T content) {
        streamCodec().encode(buf, content);
    }

    default T fromNetwork(RegistryFriendlyByteBuf buf) {
        return streamCodec().decode(buf);
    }

    default Tag toNBT(T content, HolderLookup.Provider provider) {
        return codec().encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), content).getOrThrow();
    }

    default T fromNBT(Tag tag, HolderLookup.Provider provider) {
        return codec().parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    default JsonElement toJson(T content, HolderLookup.Provider provider) {
        return codec().encodeStart(
                provider.createSerializationContext(JsonOps.INSTANCE), content).getOrThrow();
    }

    default T fromJson(JsonElement json, HolderLookup.Provider provider) {
        return codec().parse(provider.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
    }

    default T deepCopy(T content) {
        return codec().parse(JavaOps.INSTANCE, codec().encodeStart(JavaOps.INSTANCE, content).getOrThrow()).getOrThrow();
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

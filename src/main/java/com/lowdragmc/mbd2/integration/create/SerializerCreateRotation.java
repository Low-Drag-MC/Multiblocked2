package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SerializerCreateRotation implements IContentSerializer<CreateRotation> {
    public static final SerializerCreateRotation INSTANCE = new SerializerCreateRotation();

    private SerializerCreateRotation() {}

    @Override
    public CreateRotation of(Object o) {
        if (o instanceof CreateRotation cr) return cr;
        if (o instanceof Number n) return CreateRotation.stress(n.floatValue());
        return new CreateRotation();
    }

    @Override
    public CreateRotation copyWithModifier(CreateRotation content, ContentModifier modifier) {
        return new CreateRotation(
                modifier.apply(content.value).floatValue(),
                content.mode,
                ToggleFloat.of(content.torqueOverride.isEnable(), content.torqueOverride.getValue()));
    }

    @Override
    public CreateRotation copyInner(CreateRotation content) {
        return content.copy();
    }

    @Override
    public CreateRotation deepCopyInner(CreateRotation content) {
        return content.copy();
    }

    @Override
    public Codec<CreateRotation> codec() {
        return CreateRotation.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, CreateRotation> streamCodec() {
        return CreateRotation.STREAM_CODEC;
    }
}

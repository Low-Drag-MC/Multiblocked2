package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.mojang.serialization.Codec;
import hellfirepvp.astralsorcery.common.constellation.BaseConstellation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

public class SerializerStarlightAmount implements IContentSerializer<StarlightAmount> {
    public static final SerializerStarlightAmount INSTANCE = new SerializerStarlightAmount();
    public static final float DEFAULT_AMOUNT = 100f;

    private SerializerStarlightAmount() {}

    /**
     * Accepts a {@link StarlightAmount}, a number (any constellation), a {@link BaseConstellation} or its id
     * ({@value #DEFAULT_AMOUNT} of it), or a string such as {@code "100"}, {@code "100x astralsorcery:armara"} or
     * {@code "armara"}.
     *
     * @throws IllegalStateException for anything else, a non-positive or non-finite amount, or an id that names
     *                               no constellation
     */
    @Override
    public StarlightAmount of(Object o) {
        if (o instanceof StarlightAmount stack) {
            return stack;
        }
        if (o instanceof Number number) {
            return StarlightAmount.any(checked(number.floatValue()));
        }
        if (o instanceof BaseConstellation constellation) {
            return StarlightAmount.of(AstralSorceryIntegration.constellationId(constellation), DEFAULT_AMOUNT);
        }
        if (o instanceof ResourceLocation id) {
            return StarlightAmount.of(checked(id), DEFAULT_AMOUNT);
        }
        if (o instanceof CharSequence) {
            return parseString(o.toString().trim());
        }
        throw new IllegalStateException("Not starlight: " + o);
    }

    private static StarlightAmount parseString(String str) {
        float amount = DEFAULT_AMOUNT;
        int x = str.indexOf('x');
        if (x > 0 && x < str.length() - 2 && str.charAt(x + 1) == ' ') {
            try {
                amount = checked(Float.parseFloat(str.substring(0, x).trim()));
                str = str.substring(x + 2).trim();
            } catch (NumberFormatException ignore) {
            }
        } else {
            try {
                return StarlightAmount.any(checked(Float.parseFloat(str)));
            } catch (NumberFormatException ignore) {
            }
        }
        ResourceLocation id;
        try {
            id = AstralSorceryIntegration.parseId(str);
        } catch (ResourceLocationException e) {
            throw new IllegalStateException("Invalid starlight: " + str, e);
        }
        return StarlightAmount.of(checked(id), amount);
    }

    /** NaN compares false against every shortfall, so it would pay for any recipe. */
    private static float checked(float amount) {
        if (!Float.isFinite(amount) || amount <= 0) {
            throw new IllegalStateException("Invalid starlight amount: " + amount);
        }
        return amount;
    }

    private static ResourceLocation checked(ResourceLocation id) {
        if (!AstralSorceryIntegration.isKnownConstellation(id)) {
            throw new IllegalStateException("Invalid constellation: " + id);
        }
        return id;
    }

    @Override
    public StarlightAmount copyWithModifier(StarlightAmount content, ContentModifier modifier) {
        return content.withAmount(Math.max(0, modifier.apply(content.amount()).floatValue()));
    }

    @Override
    public StarlightAmount copyInner(StarlightAmount content) {
        return content;
    }

    @Override
    public StarlightAmount deepCopyInner(StarlightAmount content) {
        return content;
    }

    @Override
    public Codec<StarlightAmount> codec() {
        return StarlightAmount.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, StarlightAmount> streamCodec() {
        return StarlightAmount.STREAM_CODEC;
    }
}

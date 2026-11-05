package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.mojang.serialization.Codec;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

public class SerializerLumenAmount implements IContentSerializer<LumenAmount> {
    public static final SerializerLumenAmount INSTANCE = new SerializerLumenAmount();
    /** One flask. */
    public static final int DEFAULT_AMOUNT = LumenStack.FLASK_VALUE;

    private SerializerLumenAmount() {}

    /**
     * Accepts a {@link LumenAmount}, {@link LumenStack}, {@link Lumen} or id (one flask), or a string such as
     * {@code "250x astralsorcery:armara"}, {@code "astralsorcery:armara"} or {@code "armara"}.
     *
     * @throws IllegalStateException for anything else, or an id that names no lumen
     */
    @Override
    public LumenAmount of(Object o) {
        if (o instanceof LumenAmount amount) {
            return amount;
        }
        if (o instanceof LumenStack stack) {
            if (stack.isEmpty()) {
                throw new IllegalStateException("Empty lumen stack");
            }
            return new LumenAmount(AstralSorceryIntegration.lumenId(stack.getLumen()), stack.getAmount());
        }
        if (o instanceof Lumen lumen) {
            return new LumenAmount(AstralSorceryIntegration.lumenId(lumen), DEFAULT_AMOUNT);
        }
        if (o instanceof ResourceLocation id) {
            return new LumenAmount(checked(id), DEFAULT_AMOUNT);
        }
        if (o instanceof CharSequence) {
            return parseString(o.toString().trim());
        }
        throw new IllegalStateException("Not a lumen: " + o);
    }

    private static LumenAmount parseString(String str) {
        int amount = DEFAULT_AMOUNT;
        int x = str.indexOf('x');
        if (x > 0 && x < str.length() - 2 && str.charAt(x + 1) == ' ') {
            try {
                amount = Integer.parseInt(str.substring(0, x).trim());
                str = str.substring(x + 2).trim();
            } catch (NumberFormatException ignore) {
            }
        }
        if (amount <= 0) {
            throw new IllegalStateException("Invalid lumen amount: " + amount);
        }
        ResourceLocation id;
        try {
            id = AstralSorceryIntegration.parseId(str);
        } catch (ResourceLocationException e) {
            throw new IllegalStateException("Invalid lumen: " + str, e);
        }
        return new LumenAmount(checked(id), amount);
    }

    private static ResourceLocation checked(ResourceLocation id) {
        if (!AstralSorceryIntegration.isKnownLumen(id)) {
            throw new IllegalStateException("Invalid lumen: " + id);
        }
        return id;
    }

    @Override
    public LumenAmount copyInner(LumenAmount content) {
        return content;
    }

    @Override
    public LumenAmount deepCopyInner(LumenAmount content) {
        return content;
    }

    @Override
    public LumenAmount copyWithModifier(LumenAmount content, ContentModifier modifier) {
        return content.withAmount(Math.max(0, modifier.apply(content.amount()).intValue()));
    }

    @Override
    public Codec<LumenAmount> codec() {
        return LumenAmount.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, LumenAmount> streamCodec() {
        return LumenAmount.STREAM_CODEC;
    }
}

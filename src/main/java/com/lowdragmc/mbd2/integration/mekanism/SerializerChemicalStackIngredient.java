package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.mojang.serialization.Codec;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

@SuppressWarnings("removal")
public class SerializerChemicalStackIngredient implements IContentSerializer<ChemicalStackIngredient> {

    public static final SerializerChemicalStackIngredient INSTANCE = new SerializerChemicalStackIngredient();
    public static final long DEFAULT_AMOUNT = 1000L;

    private SerializerChemicalStackIngredient() {}

    @Override
    @SuppressWarnings({"unchecked"})
    public ChemicalStackIngredient of(Object o) {
        if (o instanceof ChemicalStackIngredient ingredient) {
            return ingredient;
        }
        if (o instanceof ChemicalIngredient ingredient) {
            return new ChemicalStackIngredient(ingredient, DEFAULT_AMOUNT);
        }
        if (o instanceof ChemicalStack stack) {
            if (stack.isEmpty()) {
                return fallback();
            }
            return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of(stack), Math.max(1L, stack.getAmount()));
        }
        if (o instanceof Holder<?> holder && holder.value() instanceof Chemical) {
            return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of((Holder<Chemical>) holder), DEFAULT_AMOUNT);
        }
        if (o instanceof Chemical chemical) {
            return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of(chemical.getAsHolder()), DEFAULT_AMOUNT);
        }
        if (o instanceof TagKey<?> tag) {
            return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().tag((TagKey<Chemical>) tag), DEFAULT_AMOUNT);
        }
        if (o instanceof CharSequence) {
            return parseString(o.toString());
        }
        return fallback();
    }

    private static ChemicalStackIngredient parseString(String str) {
        long amount = DEFAULT_AMOUNT;
        int x = str.indexOf('x');
        if (x > 0 && x < str.length() - 2 && str.charAt(x + 1) == ' ') {
            try {
                amount = Long.parseLong(str.substring(0, x).trim());
                str = str.substring(x + 2).trim();
            } catch (NumberFormatException ignore) {
            }
        }
        if (str.startsWith("#")) {
            var tag = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, ResourceLocation.parse(str.substring(1)));
            return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().tag(tag), Math.max(1L, amount));
        }
        var id = ResourceLocation.parse(str);
        if (!MekanismAPI.CHEMICAL_REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Invalid chemical input: " + str);
        }
        var chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);

        return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of(chemical.getAsHolder()), Math.max(1L, amount));
    }

    private static ChemicalStackIngredient fallback() {
        return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().empty(), 1L);
    }

    @Override
    public ChemicalStackIngredient copyInner(ChemicalStackIngredient content) {
        return new ChemicalStackIngredient(content.ingredient(), content.amount());
    }

    @Override
    public Codec<ChemicalStackIngredient> codec() {
        return ChemicalStackIngredient.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ChemicalStackIngredient> streamCodec() {
        return ChemicalStackIngredient.STREAM_CODEC;
    }

    @Override
    public ChemicalStackIngredient copyWithModifier(ChemicalStackIngredient content, ContentModifier modifier) {
        var amount = Math.max(1L, modifier.apply(content.amount()).longValue());
        return new ChemicalStackIngredient(content.ingredient(), amount);
    }
}

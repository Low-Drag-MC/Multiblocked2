package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote MBDRecipeSerializer
 */
public class MBDRecipeSerializer implements RecipeSerializer<MBDRecipe> {
    public static final Codec<Map<RecipeCapability<?>, List<Content>>> CONTENTS_CODEC = Codec.dispatchedMap(
            RecipeCapability.CODEC,
            cap -> Content.codec(cap).listOf()
    );

    public static final MapCodec<MBDRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    MBDRegistries.RECIPE_TYPES.codec().fieldOf("type").forGetter(val -> val.recipeType),
                    ResourceLocation.CODEC.lenientOptionalFieldOf("id").forGetter(val -> Optional.ofNullable(val.id)),
                    CONTENTS_CODEC.optionalFieldOf("inputs", Map.of()).forGetter(val -> val.inputs),
                    CONTENTS_CODEC.optionalFieldOf("outputs", Map.of()).forGetter(val -> val.outputs),
                    RecipeCondition.CODEC.listOf().optionalFieldOf("recipeConditions", List.of()).forGetter(val -> val.conditions),
                    CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(val -> val.data),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("duration").forGetter(val -> val.duration),
                    Codec.BOOL.optionalFieldOf("isFuel", false).forGetter(val -> val.isFuel),
                    Codec.BOOL.optionalFieldOf("isXEIHidden", false).forGetter(val -> val.isXEIHidden),
                    Codec.INT.fieldOf("priority").forGetter(val -> val.priority))
            .apply(instance, (recipeType, id, inputs, outputs, conditions, data, duration, isFuel, isXEIHidden, priority) ->
                    new MBDRecipe(recipeType, id.orElse(null), inputs, outputs, conditions, data, duration, isFuel, isXEIHidden, priority)));

    public static final StreamCodec<RegistryFriendlyByteBuf, MBDRecipe> STREAM_CODEC = StreamCodec
            .of(MBDRecipeSerializer::toNetwork, MBDRecipeSerializer::fromNetwork);

    public static final MBDRecipeSerializer SERIALIZER = new MBDRecipeSerializer();

    public static Tuple<RecipeCapability<?>, List<Content>> entryReader(FriendlyByteBuf buf) {
        var capability = MBDRegistries.RECIPE_CAPABILITIES.get(buf.readUtf());
        var contents = buf.readCollection(c -> new ArrayList<Content>(), (StreamDecoder) Content.streamCodec(capability));
        return new Tuple<>(capability, contents);
    }

    public static void entryWriter(Map.Entry<RecipeCapability<?>, ? extends List<Content>> entry, FriendlyByteBuf buf) {
        var capability = entry.getKey();
        var contents = entry.getValue();
        buf.writeUtf(MBDRegistries.RECIPE_CAPABILITIES.getKey(capability));
        buf.writeCollection(contents, (StreamEncoder) Content.streamCodec(capability));
    }

    public static Map<RecipeCapability<?>, List<Content>> tuplesToMap(List<Tuple<RecipeCapability<?>, List<Content>>> entries) {
        Map<RecipeCapability<?>, List<Content>> map = new HashMap<>();
        entries.forEach(entry -> map.put(entry.getA(), entry.getB()));
        return map;
    }

    @NotNull
    public static MBDRecipe fromNetwork(@NotNull RegistryFriendlyByteBuf buf) {
        var id = buf.readResourceLocation();
        var recipeType = buf.readUtf();
        var duration = buf.readVarInt();
        Map<RecipeCapability<?>, List<Content>> inputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), MBDRecipeSerializer::entryReader));
        Map<RecipeCapability<?>, List<Content>> outputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), MBDRecipeSerializer::entryReader));
        var conditions = buf.readCollection(c -> new ArrayList<RecipeCondition>(), (StreamDecoder) RecipeCondition.STREAM_CODEC);
        CompoundTag data = buf.readNbt();
        boolean isFuel = buf.readBoolean();
        boolean isXEIHidden = buf.readBoolean();
        int priority = buf.readVarInt();
        return new MBDRecipe((MBDRecipeType) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(recipeType)), id, inputs, outputs, conditions, data, duration, isFuel, isXEIHidden, priority);
    }

    public static void toNetwork(RegistryFriendlyByteBuf buf, MBDRecipe recipe) {
        buf.writeResourceLocation(recipe.id);
        buf.writeUtf(recipe.recipeType == null ? "dummy" : recipe.recipeType.toString());
        buf.writeVarInt(recipe.duration);
        buf.writeObjectCollection(recipe.inputs.entrySet(), MBDRecipeSerializer::entryWriter);
        buf.writeObjectCollection(recipe.outputs.entrySet(), MBDRecipeSerializer::entryWriter);
        buf.writeCollection(recipe.conditions, (StreamEncoder) RecipeCondition.STREAM_CODEC);
        buf.writeNbt(recipe.data);
        buf.writeBoolean(recipe.isFuel);
        buf.writeBoolean(recipe.isXEIHidden);
        buf.writeVarInt(recipe.priority);
    }

    @Override
    @Nonnull
    public MapCodec<MBDRecipe> codec() {
        return CODEC;
    }

    @Override
    @Nonnull
    public StreamCodec<RegistryFriendlyByteBuf, MBDRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}

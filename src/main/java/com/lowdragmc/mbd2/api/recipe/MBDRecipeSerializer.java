package com.lowdragmc.mbd2.api.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote MBDRecipeSerializer
 */
public class MBDRecipeSerializer implements RecipeSerializer<MBDRecipe> {

    public static final MBDRecipeSerializer SERIALIZER = new MBDRecipeSerializer();

    public Map<RecipeCapability<?>, List<Content>> capabilitiesFromJson(JsonObject json) {
        Map<RecipeCapability<?>, List<Content>> capabilities = new HashMap<>();
        for (String key : json.keySet()) {
            JsonArray contentsJson = json.getAsJsonArray(key);
            RecipeCapability<?> capability = MBDRegistries.RECIPE_CAPABILITIES.get(key);
            if (capability != null) {
                List<Content> contents = new ArrayList<>();
                for (JsonElement contentJson : contentsJson) {
                    contents.add(capability.serializer.fromJsonContent(contentJson));
                }
                capabilities.put(capability, contents);
            }
        }
        return capabilities;
    }

    @Override
    public @NotNull MBDRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        String recipeType = GsonHelper.getAsString(json, "type");
        int duration = json.has("duration") ? GsonHelper.getAsInt(json, "duration") : 100;
        CompoundTag data = new CompoundTag();
        if (json.has("data"))
            data = CraftingHelper.getNBT(json.get("data"));
        Map<RecipeCapability<?>, List<Content>> inputs = capabilitiesFromJson(json.has("inputs") ? json.getAsJsonObject("inputs") : new JsonObject());
        Map<RecipeCapability<?>, List<Content>> outputs = capabilitiesFromJson(json.has("outputs") ? json.getAsJsonObject("outputs") : new JsonObject());
        List<RecipeCondition> conditions = new ArrayList<>();
        JsonArray conditionsJson = json.has("recipeConditions") ? json.getAsJsonArray("recipeConditions") : new JsonArray();
        for (JsonElement jsonElement : conditionsJson) {
            if (jsonElement instanceof JsonObject jsonObject) {
                var conditionKey = GsonHelper.getAsString(jsonObject, "type", "");
                var clazz = MBDRegistries.RECIPE_CONDITIONS.get(conditionKey);
                if (clazz != null) {
                    RecipeCondition condition = RecipeCondition.create(clazz);
                    if (condition != null) {
                        conditions.add(condition.deserialize(GsonHelper.getAsJsonObject(jsonObject, "data", new JsonObject())));
                    }
                }
            }
        }
        boolean isFuel = GsonHelper.getAsBoolean(json, "isFuel", false);
        int priority = GsonHelper.getAsInt(json, "priority", 0);
        return new MBDRecipe((MBDRecipeType) BuiltInRegistries.RECIPE_TYPE.get(new ResourceLocation(recipeType)), id, inputs, outputs, conditions, data, duration, isFuel, priority);
    }

    public JsonObject capabilitiesToJson(Map<RecipeCapability<?>, List<Content>> contents) {
        JsonObject jsonObject = new JsonObject();
        contents.forEach((cap, list) -> {
            JsonArray contentsJson = new JsonArray();
            for (Content content : list) {
                contentsJson.add(cap.serializer.toJsonContent(content));
            }
            jsonObject.add(MBDRegistries.RECIPE_CAPABILITIES.getKey(cap), contentsJson);
        });
        return jsonObject;
    }

    public JsonObject toJson(@NotNull MBDRecipe recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("type", recipe.recipeType.registryName.toString());
        json.addProperty("duration", Math.abs(recipe.duration));
        if (recipe.data != null && !recipe.data.isEmpty()) {
            json.add("data", NBTToJsonConverter.getObject(recipe.data));
        }
        json.add("inputs", capabilitiesToJson(recipe.inputs));
        json.add("outputs", capabilitiesToJson(recipe.outputs));
        if (!recipe.conditions.isEmpty()) {
            JsonArray array = new JsonArray();
            for (RecipeCondition condition : recipe.conditions) {
                JsonObject cond = new JsonObject();
                cond.addProperty("type", MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
                cond.add("data", condition.serialize());
                array.add(cond);
            }
            json.add("recipeConditions", array);
        }
        if (recipe.isFuel) {
            json.addProperty("isFuel", true);
        }
        if (recipe.priority != 0) {
            json.addProperty("priority", recipe.priority);
        }
        return json;
    }

    public static Tuple<RecipeCapability<?>, List<Content>> entryReader(FriendlyByteBuf buf) {
        RecipeCapability<?> capability = MBDRegistries.RECIPE_CAPABILITIES.get(buf.readUtf());
        List<Content> contents = buf.readList(capability.serializer::fromNetworkContent);
        return new Tuple<>(capability, contents);
    }

    public static void entryWriter(FriendlyByteBuf buf, Map.Entry<RecipeCapability<?>, ? extends List<Content>> entry) {
        RecipeCapability<?> capability = entry.getKey();
        List<Content> contents = entry.getValue();
        buf.writeUtf(MBDRegistries.RECIPE_CAPABILITIES.getKey(capability));
        buf.writeCollection(contents, capability.serializer::toNetworkContent);
    }

    public static RecipeCondition conditionReader(FriendlyByteBuf buf) {
        RecipeCondition condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(buf.readUtf()));
        return condition.fromNetwork(buf);
    }

    public static void conditionWriter(FriendlyByteBuf buf, RecipeCondition condition) {
        buf.writeUtf(MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
        condition.toNetwork(buf);
    }

    public static Map<RecipeCapability<?>, List<Content>> tuplesToMap(List<Tuple<RecipeCapability<?>, List<Content>>> entries) {
        Map<RecipeCapability<?>, List<Content>> map = new HashMap<>();
        entries.forEach(entry -> map.put(entry.getA(), entry.getB()));
        return map;
    }

    @Override
    @NotNull
    public MBDRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
        String recipeType = buf.readUtf();
        int duration = buf.readVarInt();
        Map<RecipeCapability<?>, List<Content>> inputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), MBDRecipeSerializer::entryReader));
        Map<RecipeCapability<?>, List<Content>> outputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), MBDRecipeSerializer::entryReader));
        List<RecipeCondition> conditions = buf.readCollection(c -> new ArrayList<>(), MBDRecipeSerializer::conditionReader);
        CompoundTag data = buf.readNbt();
        boolean isFuel = buf.readBoolean();
        int priority = buf.readVarInt();
        return new MBDRecipe((MBDRecipeType) BuiltInRegistries.RECIPE_TYPE.get(new ResourceLocation(recipeType)), id, inputs, outputs, conditions, data, duration, isFuel, priority);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, MBDRecipe recipe) {
        buf.writeUtf(recipe.recipeType == null ? "dummy" : recipe.recipeType.toString());
        buf.writeVarInt(recipe.duration);
        buf.writeCollection(recipe.inputs.entrySet(), MBDRecipeSerializer::entryWriter);
        buf.writeCollection(recipe.outputs.entrySet(), MBDRecipeSerializer::entryWriter);
        buf.writeCollection(recipe.conditions, MBDRecipeSerializer::conditionWriter);
        buf.writeNbt(recipe.data);
        buf.writeBoolean(recipe.isFuel);
        buf.writeVarInt(recipe.priority);
    }

    public Map<RecipeCapability<?>, List<Content>> capabilitiesFromNBT(CompoundTag nbt) {
        Map<RecipeCapability<?>, List<Content>> capabilities = new HashMap<>();
        for (String key : nbt.getAllKeys()) {
            List<Content> contents = new ArrayList<>();
            RecipeCapability<?> capability = MBDRegistries.RECIPE_CAPABILITIES.get(key);
            if (capability != null) {
                for (var tag : nbt.getList(key, Tag.TAG_COMPOUND)) {
                    contents.add(capability.serializer.fromNBT((CompoundTag) tag));
                }
                capabilities.put(capability, contents);
            }
        }
        return capabilities;
    }

    public MBDRecipe fromNBT(@NotNull ResourceLocation id, @NotNull CompoundTag nbt) {
        String recipeType = nbt.getString("type");
        int duration = nbt.getInt("duration");
        Map<RecipeCapability<?>, List<Content>> inputs = capabilitiesFromNBT(nbt.getCompound("inputs"));
        Map<RecipeCapability<?>, List<Content>> outputs = capabilitiesFromNBT(nbt.getCompound("outputs"));
        List<RecipeCondition> conditions = new ArrayList<>();
        for (var tag : nbt.getList("recipeConditions", Tag.TAG_COMPOUND)) {
            CompoundTag conditionTag = (CompoundTag) tag;
            RecipeCondition condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(conditionTag.getString("type")));
            if (condition != null) {
                conditions.add(condition.fromNBT(conditionTag.getCompound("data")));
            }
        }
        CompoundTag data = nbt.getCompound("data");
        boolean isFuel = nbt.getBoolean("isFuel");
        int priority = nbt.getInt("priority");
        return new MBDRecipe((MBDRecipeType) BuiltInRegistries.RECIPE_TYPE.get(new ResourceLocation(recipeType)), id, inputs, outputs, conditions, data, duration, isFuel, priority);
    }

    public CompoundTag capabilitiesToNBT(Map<RecipeCapability<?>, List<Content>> contents) {
        CompoundTag tag = new CompoundTag();
        contents.forEach((cap, list) -> {
            ListTag contentsTag = new ListTag();
            for (Content content : list) {
                contentsTag.add(cap.serializer.toNBT(content));
            }
            tag.put(cap.name, contentsTag);
        });
        return tag;
    }

    public CompoundTag toNBT(@NotNull MBDRecipe recipe) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", recipe.recipeType.toString());
        nbt.putInt("duration", recipe.duration);
        nbt.put("inputs", capabilitiesToNBT(recipe.inputs));
        nbt.put("outputs", capabilitiesToNBT(recipe.outputs));
        ListTag conditions = new ListTag();
        for (RecipeCondition condition : recipe.conditions) {
            CompoundTag conditionTag = new CompoundTag();
            conditionTag.putString("type", MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
            conditionTag.put("data", condition.toNBT());
            conditions.add(conditionTag);
        }
        nbt.put("recipeConditions", conditions);
        nbt.put("data", recipe.data);
        nbt.putBoolean("isFuel", recipe.isFuel);
        nbt.putInt("priority", recipe.priority);
        return nbt;
    }
}

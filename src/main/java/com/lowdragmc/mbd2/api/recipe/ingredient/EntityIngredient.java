package com.lowdragmc.mbd2.api.recipe.ingredient;

import com.google.common.collect.Lists;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EntityIngredient implements Predicate<Collection<Entity>> {
    public static final EntityIngredient EMPTY = new EntityIngredient(Stream.empty(), 0, null);
    public Value[] values;
    @Nullable
    public EntityType<?>[] types;
    @Getter
    private int count;
    @Getter
    @Nullable
    private CompoundTag nbt;
    private boolean changed = true;

    public EntityIngredient(Stream<? extends Value> empty, int count, @Nullable CompoundTag nbt) {
        this.values = empty.toArray(Value[]::new);
        this.count = count;
        this.nbt = nbt;
    }

    public static EntityIngredient fromValues(Stream<? extends Value> stream, int count, @Nullable CompoundTag nbt) {
        EntityIngredient ingredient = new EntityIngredient(stream, count, nbt);
        return ingredient.isEmpty() ? EMPTY : ingredient;
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeCollection(Arrays.asList(this.getTypes()), (buf, entityType) ->
                buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)));
        buffer.writeVarInt(count);
        buffer.writeNbt(nbt);
    }

    public JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("count", this.count);
        if (this.nbt != null) {
            jsonObject.addProperty("nbt", this.nbt.getAsString());
        }
        if (this.values.length == 1) {
            jsonObject.add("value", this.values[0].serialize());
        }
        JsonArray jsonArray = new JsonArray();
        for (Value value : this.values) {
            jsonArray.add(value.serialize());
        }
        jsonObject.add("value", jsonArray);
        return jsonObject;
    }

    public EntityIngredient copy() {
        return new EntityIngredient(Arrays.stream(this.values).map(Value::copy), this.count, this.nbt == null ? null : this.nbt.copy());
    }

    public EntityIngredient copy(int count) {
        return new EntityIngredient(Arrays.stream(this.values).map(Value::copy), count, this.nbt == null ? null : this.nbt.copy());
    }
    
    @Override
    public boolean test(@Nullable Collection<Entity> entities) {
        if (entities == null) {
            return false;
        }
        if (this.isEmpty()) {
            return entities.isEmpty();
        }
        int matches = 0;
        var types = Arrays.stream(getTypes()).collect(Collectors.toSet());
        for (var entity : entities) {
            if (types.contains(entity.getType())) {
                if (nbt != null && !nbt.isEmpty()) {
                    var held = entity.serializeNBT();
                    var copied = nbt.copy();
                    copied.merge(held);
                    if (!nbt.equals(copied)) {
                        continue;
                    }
                }
                matches++;
            }
        }
        return matches >= count;
    }
    
    public boolean isEmpty() {
        return this.values.length == 0;
    }

    public EntityType<?>[] getTypes() {
        if (changed || this.types == null) {
            this.types = Arrays.stream(this.values).flatMap(entry -> entry.getTypes().stream()).distinct().toArray(EntityType<?>[]::new);
            this.changed = false;
        }
        return this.types;
    }

    public void setCount(int count) {
        this.count = count;
        this.changed = true;
    }

    public void setNbt(CompoundTag nbt) {
        this.nbt = nbt;
        this.changed = true;
    }

    public static EntityIngredient of() {
        return EMPTY;
    }

    public static EntityIngredient of(int count, EntityType<?>... entityTypes) {
        return EntityIngredient.of(Arrays.stream(entityTypes), count, null);
    }

    public static EntityIngredient of(Stream<EntityType<?>> types, int count, CompoundTag nbt) {
        return EntityIngredient.fromValues(types.map(EntityTypeValue::new), count, nbt);
    }

    /**
     * {@return a new ingredient which accepts items which are in the given tag}
     *
     * @param tag the tag key
     */
    public static EntityIngredient of(TagKey<EntityType<?>> tag, int count) {
        return EntityIngredient.fromValues(Stream.of(new TagValue(tag)), count, null);
    }

    public static EntityIngredient of(TagKey<EntityType<?>> tag, int count, CompoundTag nbt) {
        return EntityIngredient.fromValues(Stream.of(new TagValue(tag)), count, nbt);
    }

    public static EntityIngredient fromNetwork(FriendlyByteBuf buffer) {
        return EntityIngredient.fromValues(buffer.readList(buf -> BuiltInRegistries.ENTITY_TYPE.get(buf.readResourceLocation())).stream()
                .map(EntityTypeValue::new), buffer.readVarInt(), buffer.readNbt());
    }

    public static EntityIngredient fromJson(@Nullable JsonElement json) {
        return EntityIngredient.fromJson(json, true);
    }

    public static EntityIngredient fromJson(@Nullable JsonElement json, boolean allowAir) {
        if (json == null || json.isJsonNull()) {
            throw new JsonSyntaxException("Entity ingredient cannot be null");
        }
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected entity ingredient to be object");
        }
        JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "ingredient");
        var count = GsonHelper.getAsInt(jsonObject, "count", 0);
        var nbt = jsonObject.has("nbt") ? CraftingHelper.getNBT(jsonObject.get("nbt")) : null;
        if (GsonHelper.isObjectNode(jsonObject, "value")) {
            return EntityIngredient.fromValues(Stream.of(EntityIngredient.valueFromJson(GsonHelper.getAsJsonObject(jsonObject, "value"))), count, nbt);
        } else if (GsonHelper.isArrayNode(jsonObject, "value")) {
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "value");
            if (jsonArray.isEmpty() && !allowAir) {
                throw new JsonSyntaxException("Entity array cannot be empty, at least one item must be defined");
            }
            return EntityIngredient.fromValues(StreamSupport.stream(jsonArray.spliterator(), false).map(jsonElement -> EntityIngredient.valueFromJson(GsonHelper.convertToJsonObject(jsonElement, "entityType"))), count, nbt);
        }
        throw new JsonSyntaxException("expected value to be either object or array.");
    }

    private static Value valueFromJson(JsonObject json) {
        if (json.has("entityType") && json.has("tag")) {
            throw new JsonParseException("A entity ingredient entry is either a tag or a entityType, not both");
        }
        if (json.has("entityType")) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(GsonHelper.getAsString(json, "entityType")));
            return new EntityTypeValue(entityType);
        }
        if (json.has("tag")) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, resourceLocation);
            return new TagValue(tagKey);
        }
        throw new JsonParseException("A entity ingredient entry needs either a tag or a entityType");
    }

    public interface Value {
        Collection<EntityType<?>> getTypes();

        JsonObject serialize();
        Value copy();
    }

    public static class TagValue implements Value {
        @Getter @Setter
        private TagKey<EntityType<?>> tag;

        public TagValue(TagKey<EntityType<?>> tag) {
            this.tag = tag;

        }

        @Override
        public Collection<EntityType<?>> getTypes() {
            ArrayList<EntityType<?>> list = Lists.newArrayList();
            for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(this.tag)) {
                list.add(holder.value());
            }
            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("tag", this.tag.location().toString());
            return jsonObject;
        }

        @Override
        public Value copy() {
            return new TagValue(this.tag);
        }
    }

    public static class EntityTypeValue implements Value {
        @Getter @Setter
        private EntityType<?> entityType;

        public EntityTypeValue(EntityType<?> item) {
            this.entityType = item;
        }

        @Override
        public Collection<EntityType<?>> getTypes() {
            return Collections.singleton(this.entityType);
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("entityType", BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString());
            return jsonObject;
        }

        @Override
        public Value copy() {
            return new EntityTypeValue(this.entityType);
        }
    }
}

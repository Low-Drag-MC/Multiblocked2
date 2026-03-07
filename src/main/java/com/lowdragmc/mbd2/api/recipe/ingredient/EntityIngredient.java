package com.lowdragmc.mbd2.api.recipe.ingredient;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityIngredient implements Predicate<Collection<Entity>> {
    public static final Codec<EntityIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("count").forGetter(EntityIngredient::getCount),
            CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(ingredient -> Optional.ofNullable(ingredient.nbt)),
            Value.VALUES_CODEC.fieldOf("values").forGetter(ingredient -> ingredient.values)
    ).apply(instance, (count, nbt, values) -> new EntityIngredient(Arrays.stream(values), count, nbt.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityIngredient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EntityIngredient::getCount,
            ByteBufCodecs.OPTIONAL_COMPOUND_TAG,
            entityIngredient -> Optional.ofNullable(entityIngredient.nbt),
            Value.STREAM_CODEC.apply(ByteBufCodecs.list()),
            entityIngredient -> List.of(entityIngredient.values),
            (count, tag, values) -> new EntityIngredient(values.stream(), count, tag.orElse(null))
    );

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
            if (entity.isAlive() && types.contains(entity.getType())) {
                if (nbt != null && !nbt.isEmpty()) {
                    var held = entity.saveWithoutId(new CompoundTag());
                    if (!held.copy().merge(nbt).equals(held)) {
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

    public interface Value {
        MapCodec<Value> MAP_CODEC = NeoForgeExtraCodecs.xor(EntityTypeValue.MAP_CODEC, TagValue.MAP_CODEC)
                .xmap(either -> either.map(left -> left, right -> right), value -> {
                    if (value instanceof TagValue tagValue) {
                        return Either.right(tagValue);
                    } else if (value instanceof EntityTypeValue entityTypeValue) {
                        return Either.left(entityTypeValue);
                    } else {
                        throw new UnsupportedOperationException("This is neither an entity value nor a tag value.");
                    }
                });
        Codec<Value> CODEC = MAP_CODEC.codec();
        Codec<Value[]> VALUES_CODEC = Codec.list(Value.CODEC)
                .comapFlatMap(list -> DataResult.success(list.toArray(new Value[0])), List::of);
        StreamCodec<RegistryFriendlyByteBuf, Value> STREAM_CODEC = ByteBufCodecs.either(EntityTypeValue.STREAM_CODEC, TagValue.STREAM_CODEC)
                .map(
                        either -> either.map(
                                left -> left,
                                right -> right
                        ),
                        value -> {
                            if (value instanceof TagValue tagValue) {
                                return Either.right(tagValue);
                            } else if (value instanceof EntityTypeValue entityTypeValue) {
                                return Either.left(entityTypeValue);
                            } else {
                                throw new UnsupportedOperationException("This is neither an entity value nor a tag value.");
                            }
                        }
                );

        Collection<EntityType<?>> getTypes();

        Value copy();
    }

    public static class TagValue implements Value {
        public static final MapCodec<TagValue> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(TagValue::getTag)
        ).apply(instance, TagValue::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TagValue> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());

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
        public Value copy() {
            return new TagValue(this.tag);
        }
    }

    public static class EntityTypeValue implements Value {
        public static final MapCodec<EntityTypeValue> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entityType").forGetter(EntityTypeValue::getEntityType)
        ).apply(instance, EntityTypeValue::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityTypeValue> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());

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
        public Value copy() {
            return new EntityTypeValue(this.entityType);
        }
    }
}

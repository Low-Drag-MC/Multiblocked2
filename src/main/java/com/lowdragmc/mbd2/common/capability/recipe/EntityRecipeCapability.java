package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.accessors.CompoundTagAccessor;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerEntityIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.gui.recipe.ingredient.entity.EntityPreviewWidget;
import com.lowdragmc.mbd2.common.gui.recipe.ingredient.entity.EntityTypeConfigurator;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lowdragmc.lowdraglib.gui.widget.TankWidget.FLUID_SLOT_TEXTURE;

public class EntityRecipeCapability extends RecipeCapability<EntityIngredient> {
    public static final String ENTITY_TYPE = "recipe.capability.entity.ingredient.values.entity";
    public static final String TAG_TYPE = "recipe.capability.entity.ingredient.values.tag";

    public final static EntityRecipeCapability CAP = new EntityRecipeCapability();

    protected EntityRecipeCapability() {
        super("entity", SerializerEntityIngredient.INSTANCE);
    }

    @Override
    public EntityIngredient createDefaultContent() {
        return EntityIngredient.of(1, EntityType.PIG);
    }

    @Override
    public Widget createPreviewWidget(EntityIngredient content) {
        return new EntityPreviewWidget(content, 0, 0, 18, 18).setDrawHoverOverlay(false);
    }

    @Override
    public Widget createXEITemplate() {
        var preview = new EntityPreviewWidget();
        preview.initTemplate();
        return preview;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof EntityPreviewWidget entityPreview) {
            var entityIngredient = of(content.content);
            entityPreview.setEntityIngredient(entityIngredient);
            entityPreview.setIngredientIO(ingredientIO);
            entityPreview.setXEIChance(content.chance);
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<EntityIngredient> supplier, Consumer<EntityIngredient> onUpdate) {
        // count
        father.addConfigurators(new NumberConfigurator("recipe.capability.entity.ingredient.count",
                () -> supplier.get().getCount(),
                number -> {
                    var amount = number.intValue();
                    onUpdate.accept(supplier.get().copy(amount));
                }, 1, true).setRange(1, Integer.MAX_VALUE));
        // entity ingredient
        var valuesGroup = new ArrayConfiguratorGroup<>("recipe.capability.entity.ingredient.candidates", false,
                () -> Arrays.stream(supplier.get().values).collect(Collectors.toList()), (getter, setter) -> {
            // check values type
            return new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.values.type", false, getter, setter,
                    new EntityIngredient.EntityTypeValue(EntityType.PIG), true,
                    List.of(
                            // values candidates
                            new EntityIngredient.EntityTypeValue(EntityType.PIG),
                            new EntityIngredient.TagValue(EntityTypeTags.SKELETONS)),
                    value -> {
                        if (value instanceof EntityIngredient.EntityTypeValue) {
                            return ENTITY_TYPE;
                        } else if (value instanceof EntityIngredient.TagValue) {
                            return TAG_TYPE;
                        }
                        return ENTITY_TYPE;
                    }, (value, valueGroup) -> {
                // preview slot
                var preview = new EntityPreviewWidget(EntityIngredient.fromValues(Stream.of(value), 1, supplier.get().getNbt()), 0, 0, 60, 60);
                preview.setBackground(FLUID_SLOT_TEXTURE);
                preview.setShowAmount(false);
                preview.setClientSideWidget();

                if (value instanceof EntityIngredient.EntityTypeValue entityTypeValue) {
                    // entity type value
                    valueGroup.addConfigurators(new EntityTypeConfigurator(ENTITY_TYPE,
                            entityTypeValue::getEntityType,
                            entityType -> {
                                entityTypeValue.setEntityType(entityType);
                                preview.setEntityIngredient(EntityIngredient.of(value.getTypes().stream(), 1, supplier.get().getNbt()));
                                setter.accept(value);
                            },
                            EntityType.PIG, true));
                } else if (value instanceof EntityIngredient.TagValue tagValue) {
                    // tag value
                    valueGroup.addConfigurators(new SearchComponentConfigurator<>(TAG_TYPE,
                            () -> tagValue.getTag().location(), tagKey -> {
                        tagValue.setTag(TagKey.create(Registries.ENTITY_TYPE, tagKey));
                        preview.setEntityIngredient(EntityIngredient.of(value.getTypes().stream(), 1, supplier.get().getNbt()));
                        setter.accept(value);
                    }, EntityTypeTags.SKELETONS.location(), true, (word, find) -> {
                        var tags = ForgeRegistries.ENTITY_TYPES.tags();
                        if (tags == null) return;
                        for (var tag : tags) {
                            if (Thread.currentThread().isInterrupted()) return;
                            var tagKey = tag.getKey().location();
                            if (tagKey.toString().toLowerCase().contains(word.toLowerCase())) {
                                find.accept(tagKey);
                            }
                        }}, ResourceLocation::toString));
                }
                valueGroup.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", preview));
            });
        }, true);
        valuesGroup.setAddDefault(() -> new EntityIngredient.EntityTypeValue(EntityType.PIG));
        valuesGroup.setOnAdd(value -> {
            var entityIngredient = supplier.get();
            var values = entityIngredient.values;
            var newValues = Arrays.copyOf(values, values.length + 1);
            newValues[values.length] = value;
            entityIngredient.values = newValues;
            entityIngredient.types = null;
        });
        valuesGroup.setOnRemove(value -> {
            var entityIngredient = supplier.get();
            var values = entityIngredient.values;
            var newValues = Arrays.stream(values).filter(v -> v != value).toArray(EntityIngredient.Value[]::new);
            newValues[values.length] = value;
            entityIngredient.values = newValues;
            entityIngredient.types = null;
        });
        valuesGroup.setOnUpdate(values -> {
            var entityIngredient = supplier.get();
            entityIngredient.values = values.toArray(EntityIngredient.Value[]::new);
            entityIngredient.types = null;
        });
        father.addConfigurators(valuesGroup);
        // entity nbt
        try {
            father.addConfigurators(new CompoundTagAccessor().create("ldlib.gui.editor.configurator.nbt",
                    () -> Optional.ofNullable(supplier.get().getNbt()).orElseGet(CompoundTag::new),
                    tag -> {
                        var entityIngredient = supplier.get();
                        var newTag = tag.isEmpty() ? null : tag;
                        if (Objects.equals(newTag, entityIngredient.getNbt())) return;
                        entityIngredient.setNbt(newTag);
                        onUpdate.accept(entityIngredient);
                    }, false, RecipeCapability.class.getField("name")));
        } catch (Exception ignored) {}
    }

    @Override
    public Component getLeftErrorInfo(List<EntityIngredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var entityIngredient = left.get(i);
            result.append(entityIngredient.getCount() + "x ");
            var types = entityIngredient.getTypes();
            if (types.length > 0) {
                result.append(types[0].getDescription());
            } else {
                result.append("Unknown");
            }
            if (entityIngredient.getNbt() != null) {
                result.append(" with NBT");
                result.append(entityIngredient.getNbt().toString());
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }
}

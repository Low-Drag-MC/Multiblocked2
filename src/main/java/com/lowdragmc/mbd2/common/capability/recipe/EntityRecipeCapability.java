package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerEntityIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.gui.recipe.ingredient.entity.EntityIngredientSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import org.appliedenergistics.yoga.YogaDisplay;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityRecipeCapability extends RecipeCapability<EntityIngredient> {
    public static final String ENTITY_TYPE = "recipe.capability.entity.ingredient.values.entity";
    public static final String TAG_TYPE = "recipe.capability.entity.ingredient.values.tag";

    @LDLRegister(name = "entity", registry = "mbd2:recipe_capability")
    public final static EntityRecipeCapability CAP = new EntityRecipeCapability();

    protected EntityRecipeCapability() {
        super("entity", SerializerEntityIngredient.INSTANCE);
    }

    @Override
    public EntityIngredient createDefaultContent() {
        return EntityIngredient.of(1, EntityType.PIG);
    }

    @Override
    public UIElement createPreviewWidget(EntityIngredient content) {
        return new EntityIngredientSlot().slotStyle(slotStyle -> slotStyle.hoverOverlay(IGuiTexture.EMPTY));
    }

    @Override
    public UIElement createXEITemplate() {
        return new EntityIngredientSlot();
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof EntityIngredientSlot entityIngredientSlot) {
            var entityIngredient = of(content.content);
            entityIngredientSlot.setEntityIngredient(entityIngredient);

            // xei
            var ingredientIO = switch (io) {
                case IN -> IngredientIO.INPUT;
                case OUT -> IngredientIO.OUTPUT;
                default -> IngredientIO.NONE;
            };

            if (ingredientIO == IngredientIO.NONE) return;

            entityIngredientSlot.xeiRecipeIngredient(ingredientIO);
            entityIngredientSlot.xeiRecipeSlot(ingredientIO, content.chance);
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
            return new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.values.type", getter, setter,
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
                var preview = new EntityIngredientSlot();
                preview.setValue(
                        EntityIngredient.fromValues(Stream.of(value), 1, supplier.get().getNbt())
                );
                preview.countLabel.setDisplay(YogaDisplay.NONE);
                preview.getLayout().width(60).height(60);

                if (value instanceof EntityIngredient.EntityTypeValue entityTypeValue) {
                    // entity type value
                    valueGroup.addConfigurator(new RegistrySearchComponent.EntityType(ENTITY_TYPE,
                            entityTypeValue::getEntityType,
                            entityType -> {
                                entityTypeValue.setEntityType(entityType);
                                preview.setEntityIngredient(EntityIngredient.of(value.getTypes().stream(), 1, supplier.get().getNbt()));
                                setter.accept(value);
                            },
                            EntityType.PIG,
                            true
                    ));
                } else if (value instanceof EntityIngredient.TagValue tagValue) {
                    // tag value
                    valueGroup.addConfigurators(new TagKeySearchComponent.EntityType(TAG_TYPE,
                            tagValue::getTag,
                            tagKey -> {
                                tagValue.setTag(tagKey);
                                preview.setEntityIngredient(EntityIngredient.of(value.getTypes().stream(), 1, supplier.get().getNbt()));
                                setter.accept(value);
                            },
                            EntityTypeTags.SKELETONS,
                            true
                    ));
                }

                valueGroup.addConfigurators(new Configurator().addInlineChild(preview));
            });
        }, true);
        valuesGroup.setAddDefault(() -> new EntityIngredient.EntityTypeValue(EntityType.PIG));
        valuesGroup.setOnUpdate(values -> {
            var entityIngredient = supplier.get();
            entityIngredient.values = values.toArray(EntityIngredient.Value[]::new);
            entityIngredient.types = null;
        });
        father.addConfigurators(valuesGroup);

        // entity nbt
        try {
            father.addConfigurators(new TagConfigurator("ldlib2.gui.editor.configurator.nbt",
                    () -> Optional.ofNullable(supplier.get().getNbt()).orElseGet(CompoundTag::new),
                    tag -> {
                        var entityIngredient = supplier.get();
                        var newTag = tag instanceof CompoundTag compoundTag ? compoundTag : null;
                        if (newTag == null || newTag.isEmpty()) newTag = null;
                        if (Objects.equals(newTag, entityIngredient.getNbt())) return;
                        entityIngredient.setNbt(newTag);
                        onUpdate.accept(entityIngredient);
                    }, new CompoundTag(), true));
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

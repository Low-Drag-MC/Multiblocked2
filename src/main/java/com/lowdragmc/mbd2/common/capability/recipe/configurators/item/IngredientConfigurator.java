package com.lowdragmc.mbd2.common.capability.recipe.configurators.item;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.mbd2.core.mixins.IngredientAccessor;
import com.lowdragmc.mbd2.core.mixins.ItemValueAccessor;
import com.lowdragmc.mbd2.core.mixins.TagValueAccessor;
import com.mojang.serialization.JsonOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class IngredientConfigurator extends ValueConfigurator<Ingredient> {
    public enum IngredientType {
        VANILLA,
        JSON,
    }

    public static final String ITEM_VALUE = "recipe.capability.item.ingredient.values.item";
    public static final String TAG_VALUE = "recipe.capability.item.ingredient.values.tag";
    // runtime
    @Nullable
    private IngredientType currentType;

    public IngredientConfigurator(String name,
                                  Supplier<Ingredient> getter,
                                  Consumer<Ingredient> setter,
                                  @NotNull Ingredient defaultValue,
                                  boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.type",
                () -> getType(getter.get()),
                type -> setter.accept(createDefault(type)),
                IngredientType.VANILLA,
                true,
                Arrays.stream(IngredientType.values()).toList(),
                Enum::name,
                (type, group) -> {
                    currentType = type;
                    if (type == IngredientType.VANILLA) {
                        var ingredient = getter.get();
                        //noinspection ConstantValue
                        if (!(((Object) ingredient) instanceof IngredientAccessor vanillaIngredient)) return;
                        // vanilla ingredient
                        var valuesGroup = new ArrayConfiguratorGroup<>("recipe.capability.item.ingredient.candidates", false,
                                () -> Arrays.stream(ingredient.getValues()).collect(Collectors.toList()),
                                (valueGetter, valueSetter) -> {
                                    // check values type
                                    return new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.values.type",
                                            valueGetter, valueSetter,
                                            new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()), true,
                                            List.of(
                                                    // values candidates
                                                    new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()),
                                                    new Ingredient.TagValue(ItemTags.COALS)),
                                            value -> value instanceof Ingredient.TagValue ? TAG_VALUE : ITEM_VALUE,
                                            (value, valueGroup) -> {
                                        if (value instanceof ItemValueAccessor itemValue) {
                                            // item value
                                            valueGroup.addConfigurator(new RegistrySearchComponent.Item("",
                                                    () -> itemValue.getItem().getItem(),
                                                    item -> {
                                                        itemValue.setItem(item.getDefaultInstance());
                                                        valueSetter.accept(value);
                                                    }, Items.IRON_INGOT, true));
                                        } else if (value instanceof TagValueAccessor tagValue) {
                                            // tag value
                                            valueGroup.addConfigurator(new TagKeySearchComponent.Item("",
                                                    tagValue::getTag, tagKey -> {
                                                tagValue.setTag(tagKey);
                                                valueSetter.accept(value);
                                            }, ItemTags.COALS, true));
                                        }
                                    });
                        }, true);
                        valuesGroup.setAddDefault(() -> new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()));
                        valuesGroup.setOnUpdate(values -> {
                            vanillaIngredient.setValues(values.toArray(Ingredient.Value[]::new));
                            vanillaIngredient.setItemStacks(null);
                        });
                        group.addConfigurators(valuesGroup);
                    } else if (type == IngredientType.JSON) {
                        var op = Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE);
                        group.addConfigurator(new TextAreaConfigurator("",
                                () -> Ingredient.CODEC.encodeStart(op, getter.get())
                                        .map(LDLib2.GSON::toJson)
                                        .map(s -> s.split("\n"))
                                        .result().orElseGet(() -> new String[0]),
                                lines -> {
                            setter.accept(Ingredient.CODEC.parse(op, JsonParser.parseString(String.join("\n", lines)))
                                    .result()
                                    .orElse(Ingredient.EMPTY));
                                }, """
                                {
                                  "type": "neoforge:compound",
                                  "ingredients": [
                                     { "item": "minecraft:coal" },
                                     { "item": "minecraft:charcoal" }
                                  ]
                                }
                                """.split("\n"), true));
                    }
                }));
    }

    public IngredientType getType(Ingredient ingredient) {
        // if JSON, then keep JSON.
        if (currentType != null && currentType == IngredientType.JSON) return currentType;
        if (!ingredient.isCustom()) return IngredientType.VANILLA;
        return IngredientType.JSON;
    }

    public Ingredient createDefault(IngredientType type) {
        if (type == IngredientType.VANILLA) return Ingredient.of(Items.IRON_INGOT);
        return Ingredient.EMPTY;
    }
}

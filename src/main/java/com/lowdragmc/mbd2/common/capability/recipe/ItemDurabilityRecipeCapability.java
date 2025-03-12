package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.accessors.CompoundTagAccessor;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerSizedIngredient;
import com.lowdragmc.mbd2.core.mixins.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ItemDurabilityRecipeCapability extends RecipeCapability<SizedIngredient> {
    public static final String VANILLA_TYPE = "recipe.capability.item.ingredient.type.vanilla";
    public static final String DATA_COMPONENT_TYPE = "recipe.capability.item.ingredient.type.data_component";

    public static final String ITEM_TYPE = "recipe.capability.item.ingredient.values.item";
    public static final String TAG_TYPE = "recipe.capability.item.ingredient.values.tag";

    public final static ItemDurabilityRecipeCapability CAP = new ItemDurabilityRecipeCapability();

    protected ItemDurabilityRecipeCapability() {
        super("item_durability", SerializerSizedIngredient.INSTANCE);
    }

    @Override
    public SizedIngredient createDefaultContent() {
        return new SizedIngredient(Ingredient.of(Items.FLINT_AND_STEEL), 1);
    }

    @Override
    public Widget createPreviewWidget(SizedIngredient content) {
        var transfer = new CycleItemStackHandler(List.of(Arrays.stream(content.getItems()).filter(ItemStack::isDamageableItem).toList()));
        return new SlotWidget(transfer, 0, 0, 0, false, false)
                .setItemHook(stack -> {
                    if (!stack.isDamageableItem()) return stack.copyWithCount(1);
                    var percentage = System.currentTimeMillis() % 2000 / 2000f;
                    if (percentage < 0.5) percentage = percentage / 0.5f;
                    else percentage = 1 - (percentage - 0.5f) / 0.5f;
                    stack = stack.copyWithCount(1);
                    stack.setDamageValue((int) (stack.getMaxDamage() * percentage));
                    return stack;
                })
                .setOnAddedTooltips((widget, tooltips) -> {
                    var value = content.getItems().length == 0 ? 0 : content.getItems()[0].getCount();
                    tooltips.add(Component.translatable("recipe.capability.item.ingredient.durability.content", value));
                })
                .setDrawHoverOverlay(false).setBackgroundTexture(null);
    }

    @Override
    public Widget createXEITemplate() {
        var slotWidget = new SlotWidget();
        slotWidget.initTemplate();
        return slotWidget;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof SlotWidget slotWidget) {
            var ingredient = of(content.content);
            if (slotWidget.getOverlay() == null || slotWidget.getOverlay() == IGuiTexture.EMPTY) {
                slotWidget.setOverlay(content.createOverlay());
            } else {
                var groupTexture = new GuiTextureGroup(slotWidget.getOverlay(), content.createOverlay());
                slotWidget.setOverlay(groupTexture);
            }
            slotWidget.setHandlerSlot(new CycleItemStackHandler(List.of(Arrays.stream(ingredient.getItems()).toList())), 0);
            slotWidget.setIngredientIO(ingredientIO);
            slotWidget.setCanTakeItems(false);
            slotWidget.setCanPutItems(false);
            slotWidget.setXEIChance(content.chance);
            slotWidget.setItemHook(stack -> {
                if (!stack.isDamageableItem()) return stack.copyWithCount(1);
                var percentage = System.currentTimeMillis() % 2000 / 2000f;
                if (percentage < 0.5) percentage = percentage / 0.5f;
                else percentage = 1 - (percentage - 0.5f) / 0.5f;
                stack = stack.copyWithCount(1);
                stack.setDamageValue((int) (stack.getMaxDamage() * percentage));
                return stack;
            });
            slotWidget.setOnAddedTooltips((slot, tooltips) -> tooltips.add(Component.translatable("recipe.capability.item.ingredient.durability.content", ingredient.count())));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<SizedIngredient> supplier, Consumer<SizedIngredient> onUpdate) {
        BiConsumer<Ingredient, Integer> update = (ingredient, amount) -> onUpdate.accept(new SizedIngredient(ingredient, amount));
        Runnable clearCache = () -> {
            var ingredient = supplier.get();
            ((IngredientAccessor)(Object)ingredient.ingredient()).setItemStacks(null);
            ((SizedIngredientAccessor)(Object)ingredient).setCachedStacks(null);
        };
        // sized ingredient durability
        father.addConfigurators(new NumberConfigurator("recipe.capability.item.ingredient.durability",
                () -> supplier.get().count(),
                number -> update.accept(supplier.get().ingredient(), number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
        // inner ingredient type
        father.addConfigurators(new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.type", false, () -> supplier.get().ingredient(), ingredient -> {
            update.accept(ingredient, supplier.get().count());
        }, Ingredient.of(Items.IRON_INGOT), true, List.of(
                // ingredient type candidates
                Ingredient.of(Items.IRON_INGOT),
                DataComponentIngredient.of(false, PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE))
        ), ingredient -> {
            if (!ingredient.isCustom()) {
                return VANILLA_TYPE;
            } else if (ingredient.getCustomIngredient() instanceof DataComponentIngredient) {
                return DATA_COMPONENT_TYPE;
            }
            return VANILLA_TYPE;
        }, (ingredient, group) -> {
            if (!((Object)ingredient instanceof IngredientAccessor vanillaIngredient)) return;
            if (!ingredient.isCustom()) {
                // vanilla ingredient
                var valuesGroup = new ArrayConfiguratorGroup<>("recipe.capability.item.ingredient.candidates", false, () -> {
                    var values = ingredient.getValues();
                    return Arrays.stream(values).collect(Collectors.toList());
                }, (getter, setter) -> {
                    // check values type
                    return new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.values.type", false, getter, setter,
                            new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()), true,
                            List.of(
                                    // values candidates
                                    new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()),
                                    new Ingredient.TagValue(ItemTags.COALS)),
                            value -> {
                                if (value instanceof Ingredient.ItemValue) {
                                    return ITEM_TYPE;
                                } else if (value instanceof Ingredient.TagValue) {
                                    return TAG_TYPE;
                                }
                                return ITEM_TYPE;
                            }, (value, valueGroup) -> {
                        // preview slot
                        var itemHandler = new CycleItemStackHandler(List.of(value.getItems().stream().toList()));
                        var slot = new SlotWidget(itemHandler, 0, 0, 0, false, false);
                        slot.setClientSideWidget();

                        if (value instanceof ItemValueAccessor itemValue) {
                            // item value
                            valueGroup.addConfigurators(new ItemConfigurator(ITEM_TYPE,
                                    () -> itemValue.getItem().getItem(),
                                    item -> {
                                        itemValue.setItem(item.getDefaultInstance());
                                        itemHandler.updateStacks(List.of(value.getItems().stream().toList()));
                                        setter.accept(value);
                                    },
                                    Items.IRON_INGOT, true));
                        } else if (value instanceof TagValueAccessor tagValue) {
                            // tag value
                            valueGroup.addConfigurators(new SearchComponentConfigurator<>(TAG_TYPE,
                                    () -> tagValue.getTag().location(), tagKey -> {
                                tagValue.setTag(ItemTags.create(tagKey));
                                itemHandler.updateStacks(List.of(value.getItems().stream().toList()));
                                setter.accept(value);
                            }, ItemTags.COALS.location(), true, (word, find) -> {
                                for (var tag : BuiltInRegistries.ITEM.getTagNames().toList()) {
                                    if (Thread.currentThread().isInterrupted()) return;
                                    var tagKey = tag.location();
                                    if (tagKey.toString().toLowerCase().contains(word.toLowerCase())) {
                                        find.accept(tagKey);
                                    }
                                }}, ResourceLocation::toString));
                        }
                        valueGroup.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", slot));
                    });
                }, true);
                valuesGroup.setAddDefault(() -> new Ingredient.ItemValue(Items.IRON_INGOT.getDefaultInstance()));
                valuesGroup.setOnAdd(value -> {
                    var values = ingredient.getValues();
                    var newValues = Arrays.copyOf(values, values.length + 1);
                    newValues[values.length] = value;
                    vanillaIngredient.setValues(newValues);
                    clearCache.run();
                });
                valuesGroup.setOnRemove(value -> {
                    var values = ingredient.getValues();
                    var newValues = Arrays.stream(values).filter(v -> v != value).toArray(Ingredient.Value[]::new);
                    vanillaIngredient.setValues(newValues);
                    clearCache.run();
                });
                valuesGroup.setOnUpdate(values -> {
                    vanillaIngredient.setValues(values.toArray(Ingredient.Value[]::new));
                    clearCache.run();
                });
                group.addConfigurators(valuesGroup);
            } else if (ingredient.getCustomIngredient() instanceof DataComponentIngredient) {
                // preview
                Supplier<DataComponentIngredient> getCustomIngredient = () -> (DataComponentIngredient) ingredient.getCustomIngredient();
                var itemHandler = new CycleItemStackHandler(List.of(getCustomIngredient.get().getItems().toList()));
                var slot = new SlotWidget(itemHandler, 0, 0, 0, false, false);
                slot.setClientSideWidget();
                Consumer<DataComponentIngredient> updateCustomIngredient = customIngredient -> {
                    vanillaIngredient.setCustomIngredient(customIngredient);
                    clearCache.run();
                    itemHandler.updateStacks(List.of(customIngredient.getItems().toList()));
                };

                // strict
                var strictConfigurator = new BooleanConfigurator("recipe.capability.item.ingredient.data_component.strict",
                        () -> getCustomIngredient.get().isStrict(),
                        strict -> {
                            var previous = getCustomIngredient.get();
                            updateCustomIngredient.accept(new DataComponentIngredient(previous.items(), previous.components(), strict));
                        }, true, true);
                strictConfigurator.setTips("recipe.capability.item.ingredient.data_component.strict.tips");

                // items
                var itemsConfigurator = new ArrayConfiguratorGroup<>("recipe.capability.item.ingredient.data_component.items", false,
                        () -> getCustomIngredient.get().items().stream().toList(),
                        (getter, setter) -> new ItemConfigurator("recipe.capability.item.ingredient.values.item", () -> getter.get().value(), Item::builtInRegistryHolder, Items.POTION, true), true);
                itemsConfigurator.setAddDefault(() -> Items.POTION.builtInRegistryHolder());
                itemsConfigurator.setOnAdd(value -> {
                    var items = getCustomIngredient.get().items().stream().toArray(Holder[]::new);
                    Holder<Item>[] newItems = Arrays.copyOf(items, items.length + 1);
                    newItems[items.length] = value;
                    updateCustomIngredient.accept(new DataComponentIngredient(HolderSet.direct(newItems), getCustomIngredient.get().components(), getCustomIngredient.get().isStrict()));
                });
                itemsConfigurator.setOnRemove(value -> {
                    var items = getCustomIngredient.get().items().stream().filter(item -> !item.equals(value)).toArray(Holder[]::new);
                    updateCustomIngredient.accept(new DataComponentIngredient(HolderSet.direct(items), getCustomIngredient.get().components(), getCustomIngredient.get().isStrict()));
                });
                itemsConfigurator.setOnUpdate(list -> updateCustomIngredient.accept(new DataComponentIngredient(HolderSet.direct(list), getCustomIngredient.get().components(), getCustomIngredient.get().isStrict())));

                // data component
                Field _x = null;
                try {
                    _x = RecipeCapability.class.getDeclaredField("name");
                } catch (NoSuchFieldException ignored) {}
                var componentConfigurator = new CompoundTagAccessor().create("recipe.capability.item.ingredient.data_component.data_component",
                        () -> DataComponentPredicate.CODEC.encodeStart(NbtOps.INSTANCE, getCustomIngredient.get().components())
                                .result().map(CompoundTag.class::cast).orElseGet(CompoundTag::new),
                        tag -> updateCustomIngredient.accept(new DataComponentIngredient(getCustomIngredient.get().items(),
                                DataComponentPredicate.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(DataComponentPredicate.EMPTY),
                                getCustomIngredient.get().isStrict())), false, _x);


                group.addConfigurators(strictConfigurator, itemsConfigurator, componentConfigurator, new WrapperConfigurator("ldlib.gui.editor.group.preview", slot));
            }
        }));
    }

    @Override
    public Component getLeftErrorInfo(List<SizedIngredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var ingredient = left.get(i);
            result.append(ingredient.count() + "x ");
            var stacks = ingredient.getItems();
            if (stacks.length > 0) {
                result.append(stacks[0].getDisplayName());
            } else {
                result.append("Unknown");
            }
            if (ingredient.ingredient().getCustomIngredient() instanceof DataComponentIngredient) {
                result.append(" with DataComponent");
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }

}

package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.accessors.CompoundTagAccessor;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.TagOrCycleItemStackTransfer;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.core.mixins.IngredientAccessor;
import com.lowdragmc.mbd2.core.mixins.ItemValueAccessor;
import com.lowdragmc.mbd2.core.mixins.StrictNBTIngredientAccessor;
import com.lowdragmc.mbd2.core.mixins.TagValueAccessor;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote ItemRecipeCapability
 */
public class ItemRecipeCapability extends RecipeCapability<Ingredient> {
    public static final String VANILLA_TYPE = "recipe.capability.item.ingredient.type.vanilla";
    public static final String NBT_TYPE = "recipe.capability.item.ingredient.type.nbt";

    public static final String ITEM_TYPE = "recipe.capability.item.ingredient.values.item";
    public static final String TAG_TYPE = "recipe.capability.item.ingredient.values.tag";

    public final static ItemRecipeCapability CAP = new ItemRecipeCapability();

    protected ItemRecipeCapability() {
        super("item", SerializerIngredient.INSTANCE);
    }

    @Override
    public Ingredient createDefaultContent() {
        return SizedIngredient.create(Ingredient.of(Items.IRON_INGOT));
    }

    @Override
    public Widget createPreviewWidget(Ingredient content) {
        var transfer = new CycleItemStackHandler(List.of(List.of(content.getItems())));
        return new SlotWidget(transfer, 0, 0, 0, false, false).setDrawHoverOverlay(false).setBackgroundTexture(null);
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
            final int amount;
            var innerIngredient = ingredient;
            if (ingredient instanceof SizedIngredient sizedIngredient) {
                amount = sizedIngredient.getAmount();
                innerIngredient = sizedIngredient.getInner();
            } else {
                amount = 1;
            }
            Either<List<Pair<TagKey<Item>, Integer>>, List<ItemStack>> either = null;
            if (innerIngredient.isVanilla() && innerIngredient instanceof IngredientAccessor vanillaIngredient) {
                // if all item tags
                if (Arrays.stream(vanillaIngredient.getValues()).allMatch(Ingredient.TagValue.class::isInstance)) {
                    either = Either.left(Arrays.stream(vanillaIngredient.getValues())
                            .map(Ingredient.TagValue.class::cast)
                            .map(TagValueAccessor.class::cast)
                            .map(TagValueAccessor::getTag)
                            .map(tagValue -> new Pair<>(tagValue, amount)).toList());
                }
            }
            if (either == null) {
                either = Either.right(List.of(ingredient.getItems()));
            }
            if (slotWidget.getOverlay() == null || slotWidget.getOverlay() == IGuiTexture.EMPTY) {
                slotWidget.setOverlay(content.createOverlay());
            } else {
                var groupTexture = new GuiTextureGroup(slotWidget.getOverlay(), content.createOverlay());
                slotWidget.setOverlay(groupTexture);
            }
            slotWidget.setHandlerSlot(new TagOrCycleItemStackTransfer(List.of(either)), 0);
            slotWidget.setIngredientIO(ingredientIO);
            slotWidget.setCanTakeItems(false);
            slotWidget.setCanPutItems(false);
            slotWidget.setXEIChance(content.chance);
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Ingredient> supplier, Consumer<Ingredient> onUpdate) {
        // sized ingredient amount
        father.addConfigurators(new NumberConfigurator("recipe.capability.item.ingredient.count",
                () -> supplier.get() instanceof SizedIngredient sizedIngredient ? sizedIngredient.getAmount() : 1,
                number -> {
                    var amount = number.intValue();
                    var ingredient = supplier.get();
                    if (ingredient instanceof SizedIngredient sizedIngredient) {
                        onUpdate.accept(SizedIngredient.create(sizedIngredient.getInner(), amount));
                    } else {
                        onUpdate.accept(SizedIngredient.create(ingredient, amount));
                    }
                }, 1, true).setRange(1, Integer.MAX_VALUE));
        // inner ingredient type
        father.addConfigurators(new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.type", false, () -> {
            var ingredient = supplier.get();
            if (ingredient instanceof SizedIngredient sizedIngredient) {
                return sizedIngredient.getInner();
            }
            return Ingredient.of(Items.IRON_INGOT);
        }, ingredient -> {
            var current = supplier.get();
            if (current instanceof SizedIngredient sizedIngredient) {
                sizedIngredient.updateInnerIngredient(ingredient);
            } else {
                onUpdate.accept(SizedIngredient.create(ingredient, 1));
            }
        }, Ingredient.of(Items.IRON_INGOT), true, List.of(
                // ingredient type candidates
                Ingredient.of(Items.IRON_INGOT),
                StrictNBTIngredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.FIRE_RESISTANCE))), ingredient -> {
            if (ingredient.isVanilla() && ingredient instanceof IngredientAccessor) {
                return VANILLA_TYPE;
            } else if (ingredient instanceof StrictNBTIngredient) {
                return NBT_TYPE;
            }
            return VANILLA_TYPE;
        }, (ingredient, group) -> {
            if (ingredient.isVanilla() && ingredient instanceof IngredientAccessor vanillaIngredient) {
                // vanilla ingredient
                var valuesGroup = new ArrayConfiguratorGroup<>("recipe.capability.item.ingredient.candidates", false, () -> {
                    var values = vanillaIngredient.getValues();
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
                                var tags = ForgeRegistries.ITEMS.tags();
                                if (tags == null) return;
                                for (var tag : tags) {
                                    if (Thread.currentThread().isInterrupted()) return;
                                    var tagKey = tag.getKey().location();
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
                    var values = vanillaIngredient.getValues();
                    var newValues = Arrays.copyOf(values, values.length + 1);
                    newValues[values.length] = value;
                    vanillaIngredient.setValues(newValues);
                    vanillaIngredient.setItemStacks(null);
                    if (supplier.get() instanceof SizedIngredient sizedIngredient) {
                        sizedIngredient.updateInnerIngredient(ingredient);
                    }
                });
                valuesGroup.setOnRemove(value -> {
                    var values = vanillaIngredient.getValues();
                    var newValues = Arrays.stream(values).filter(v -> v != value).toArray(Ingredient.Value[]::new);
                    vanillaIngredient.setValues(newValues);
                    vanillaIngredient.setItemStacks(null);
                    if (supplier.get() instanceof SizedIngredient sizedIngredient) {
                        sizedIngredient.updateInnerIngredient(ingredient);
                    }
                });
                valuesGroup.setOnUpdate(values -> {
                    vanillaIngredient.setValues(values.toArray(Ingredient.Value[]::new));
                    vanillaIngredient.setItemStacks(null);
                    if (supplier.get() instanceof SizedIngredient sizedIngredient) {
                        sizedIngredient.updateInnerIngredient(ingredient);
                    }
                });
                group.addConfigurators(valuesGroup);
            } else if (ingredient instanceof StrictNBTIngredientAccessor accessor) {
                // nbt ingredient
                var itemHandler = new ItemStackTransfer(accessor.getStack());
                var slot = new SlotWidget(itemHandler, 0, 0, 0, false, false);
                slot.setClientSideWidget();
                Consumer<ItemStack> updateStack = stack -> {
                    accessor.setStack(stack);
                    itemHandler.setStackInSlot(0, stack);
                    ((IngredientAccessor)accessor).setItemStacks(null);
                    if (supplier.get() instanceof SizedIngredient sizedIngredient) {
                        sizedIngredient.updateInnerIngredient(ingredient);
                    }
                };
                group.addConfigurators(new ItemConfigurator("id",
                        () -> accessor.getStack().getItem(),
                        item -> {
                            var last = accessor.getStack();
                            var tag = last.getTag();
                            var count = last.getCount();
                            var newStack = new ItemStack(item, Math.max(count, 1));
                            newStack.setTag(tag);
                            updateStack.accept(newStack);
                        }, Items.AIR, true));
                try {
                    group.addConfigurators(new CompoundTagAccessor().create("ldlib.gui.editor.configurator.nbt",
                            () -> accessor.getStack().hasTag() ? accessor.getStack().getTag() : new CompoundTag(),
                            tag -> {
                                var last = accessor.getStack();
                                var item = last.getItem();
                                var count = last.getCount();
                                var newStack = new ItemStack(item, Math.max(count, 1));
                                if (tag.isEmpty()) {
                                    newStack.setTag(null);
                                } else {
                                    newStack.setTag(tag);
                                }
                                updateStack.accept(newStack);
                            }, false, RecipeCapability.class.getField("name")));
                } catch (Exception ignored) {
                }
                group.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", slot));
            }
        }));
    }

    @Override
    public Component getLeftErrorInfo(List<Ingredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var ingredient = left.get(i);
            var amount = 1;
            if (ingredient instanceof SizedIngredient sizedIngredient) {
                amount = sizedIngredient.getAmount();
                ingredient = sizedIngredient.getInner();
            }
            result.append(amount + "x ");
            var stacks = ingredient.getItems();
            if (stacks.length > 0) {
                result.append(stacks[0].getDisplayName());
            } else {
                result.append("Unknown");
            }
            if (ingredient instanceof StrictNBTIngredient) {
                result.append(" with NBT");
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }

}

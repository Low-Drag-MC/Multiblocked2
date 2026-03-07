package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerSizedIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.configurators.item.IngredientConfigurator;
import com.lowdragmc.mbd2.core.mixins.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemDurabilityRecipeCapability extends RecipeCapability<SizedIngredient> {
    public static final String VANILLA_TYPE = "recipe.capability.item.ingredient.type.vanilla";
    public static final String DATA_COMPONENT_TYPE = "recipe.capability.item.ingredient.type.data_component";

    public static final String ITEM_TYPE = "recipe.capability.item.ingredient.values.item";
    public static final String TAG_TYPE = "recipe.capability.item.ingredient.values.tag";

    @LDLRegister(name = "item_durability", registry = "mbd2:recipe_capability")
    public final static ItemDurabilityRecipeCapability CAP = new ItemDurabilityRecipeCapability();

    protected ItemDurabilityRecipeCapability() {
        super("item_durability", SerializerSizedIngredient.INSTANCE);
    }

    @Override
    public SizedIngredient createDefaultContent() {
        return new SizedIngredient(Ingredient.of(Items.FLINT_AND_STEEL), 1);
    }

    @Override
    public UIElement createPreviewWidget(SizedIngredient content) {
        return new ItemSlot().selfCall(element -> ((ItemSlot) element).bindDataSource(SupplierDataSource.of(() -> {
            var mui = element.getModularUI();
            var items = Arrays.stream(content.getItems()).filter(ItemStack::isDamageableItem).toList();
            return parseDamageItems(mui == null ? 0 : (int) mui.getTickCounter(), items);
        })));
    }

    @NotNull
    private ItemStack parseDamageItems(int tickCount, List<ItemStack> items) {
        var stack = items.get((tickCount / 20) % items.size());
        if (!stack.isDamageableItem()) return stack.copy();
        var percentage = System.currentTimeMillis() % 2000 / 2000f;
        if (percentage < 0.5) percentage = percentage / 0.5f;
        else percentage = 1 - (percentage - 0.5f) / 0.5f;
        stack = stack.copy();
        stack.setDamageValue((int) (stack.getMaxDamage() * percentage));
        return stack;
    }

    @Override
    public UIElement createXEITemplate() {
        return new ItemSlot();
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof ItemSlot itemSlot) {
            var ingredient = of(content.content);

            var items = Arrays.stream(ingredient.getItems()).filter(ItemStack::isDamageableItem).toList();
            if (items.isEmpty()) return;

            itemSlot.bindDataSource(SupplierDataSource.of(() -> {
                var mui = widget.getModularUI();
                return parseDamageItems(mui == null ? 0 : (int) mui.getTickCounter(), items);
            }));

            // xei
            var ingredientIO = switch (io) {
                case IN -> IngredientIO.INPUT;
                case OUT -> IngredientIO.OUTPUT;
                default -> IngredientIO.NONE;
            };

            if (ingredientIO != IngredientIO.NONE) {
                itemSlot.xeiRecipeIngredient(ingredientIO, Arrays.stream(ingredient.getItems()));
            }

            itemSlot.xeiRecipeSlot(ingredientIO, content.chance, ingredient.count(), Arrays.stream(ingredient.getItems()));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<SizedIngredient> supplier, Consumer<SizedIngredient> onUpdate) {
        father.addConfigurators(
                new NumberConfigurator("recipe.capability.item.ingredient.durability",
                        () -> supplier.get().count(),
                        number -> onUpdate.accept(new SizedIngredient(supplier.get().ingredient(), number.intValue())), 1, true)
                        .setRange(1, Integer.MAX_VALUE),
                new IngredientConfigurator("",
                        () -> supplier.get().ingredient(),
                        ingredient -> onUpdate.accept(new SizedIngredient(ingredient, supplier.get().count())),
                        Ingredient.of(Items.IRON_INGOT),
                        true)
        );
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

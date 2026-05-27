package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.ScrollDataSource;
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
import com.lowdragmc.mbd2.common.gui.recipe.SupplierScrollDataSource;
import com.lowdragmc.mbd2.core.mixins.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote ItemRecipeCapability
 */
public class ItemRecipeCapability extends RecipeCapability<SizedIngredient> {
    @LDLRegister(name = "item", registry = "mbd2:recipe_capability")
    public final static ItemRecipeCapability CAP = new ItemRecipeCapability();

    protected ItemRecipeCapability() {
        super("item", SerializerSizedIngredient.INSTANCE);
    }

    @Override
    public SizedIngredient createDefaultContent() {
        return new SizedIngredient(Ingredient.of(Items.IRON_INGOT), 1);
    }

    @Override
    public UIElement createPreview(Supplier<SizedIngredient> content) {
        return new ItemSlot().bindDataSource(SupplierScrollDataSource.of(() -> Arrays.stream(content.get().getItems()).toList()));
    }

    @Override
    public UIElement createXEITemplate() {
        return new ItemSlot();
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof ItemSlot itemSlot) {
            var ingredient = of(content.content);

            var items = ingredient.getItems();
            if (items.length == 0) return;
            if (items.length == 1) {
                itemSlot.setItem(items[0]);
            } else {
                itemSlot.bindDataSource(ScrollDataSource.of(Arrays.stream(items).toList()));
            }

            // xei
            var ingredientIO = switch (io) {
                case IN -> IngredientIO.INPUT;
                case OUT -> IngredientIO.OUTPUT;
                default -> IngredientIO.NONE;
            };

            if (ingredientIO != IngredientIO.NONE) {
                itemSlot.xeiRecipeIngredient(ingredientIO, () -> Arrays.stream(ingredient.getItems()));
            }

            itemSlot.xeiRecipeSlot(ingredientIO, content.chance, ingredient.count(), () -> Arrays.stream(ingredient.getItems()));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<SizedIngredient> supplier, Consumer<SizedIngredient> onUpdate) {
        father.addConfigurators(
                new NumberConfigurator("recipe.capability.item.ingredient.count",
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

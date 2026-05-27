package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.ScrollDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.gui.recipe.SupplierScrollDataSource;
import com.lowdragmc.mbd2.integration.mekanism.configurator.ChemicalIngredientConfigurator;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MekanismChemicalRecipeCapability extends RecipeCapability<ChemicalStackIngredient> {
    @LDLRegister(name = "mek_chemical", registry = "mbd2:recipe_capability", modID = "mekanism")
    public static final MekanismChemicalRecipeCapability CAP = new MekanismChemicalRecipeCapability();

    protected MekanismChemicalRecipeCapability() {
        super("mek_chemical", SerializerChemicalStackIngredient.INSTANCE);
    }

    @Override
    public ChemicalStackIngredient createDefaultContent() {
        return new ChemicalStackIngredient(IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN), 1000L);
    }

    @Override
    public UIElement createPreview(Supplier<ChemicalStackIngredient> content) {
        return new ChemicalSlot().bindDataSource(SupplierScrollDataSource.of(() -> stacks(content.get())));
    }

    @Override
    public UIElement createXEITemplate() {
        var widget = new ChemicalSlot();
        widget.getLayout().width(20).height(58);
        return widget;
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof ChemicalSlot tankWidget) {
            var ingredient = of(content.content);
            var possible = stacks(ingredient);
            if (possible.isEmpty()) return;
            if (possible.size() == 1) {
                tankWidget.setChemical(possible.getFirst());
            } else {
                tankWidget.bindDataSource(ScrollDataSource.of(possible));
            }

            var ingredientIO = switch (io) {
                case IN -> IngredientIO.INPUT;
                case OUT -> IngredientIO.OUTPUT;
                default -> IngredientIO.NONE;
            };
            tankWidget.xeiRecipeIngredient(ingredientIO);
            tankWidget.xeiRecipeSlot(ingredientIO, content.chance);
        }
    }

    private static List<ChemicalStack> stacks(ChemicalStackIngredient ingredient) {
        return ingredient.ingredient().getChemicalHolders().stream()
                .map(holder -> new ChemicalStack(holder, ingredient.amount()))
                .collect(Collectors.toList());
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<ChemicalStackIngredient> supplier, Consumer<ChemicalStackIngredient> onUpdate) {
        father.addConfigurators(
                new NumberConfigurator("recipe.capability.mek_chemical.amount",
                        () -> supplier.get().amount(),
                        number -> onUpdate.accept(new ChemicalStackIngredient(supplier.get().ingredient(), Math.max(1L, number.longValue()))),
                        1, true).setRange(1, Long.MAX_VALUE),
                new ChemicalIngredientConfigurator("",
                        () -> supplier.get().ingredient(),
                        ingredient -> onUpdate.accept(new ChemicalStackIngredient(ingredient, supplier.get().amount())),
                        IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN),
                        true)
        );
    }

    @Override
    public Component getLeftErrorInfo(List<ChemicalStackIngredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var ingredient = left.get(i);
            result.append(ingredient.amount() + "x ");
            var holders = ingredient.ingredient().getChemicalHolders();
            if (!holders.isEmpty()) {
                Chemical chemical = holders.getFirst().value();
                result.append(chemical.getTextComponent());
            } else {
                result.append("Unknown");
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }
}

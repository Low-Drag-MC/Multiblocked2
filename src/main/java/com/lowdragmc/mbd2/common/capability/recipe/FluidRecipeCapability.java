package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.ScrollDataSource;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerSizedFluidIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.configurators.fluid.FluidIngredientConfigurator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote FluidRecipeCapability
 */
public class FluidRecipeCapability extends RecipeCapability<SizedFluidIngredient> {
    @LDLRegister(name = "fluid", registry = "mbd2:recipe_capability")
    public final static FluidRecipeCapability CAP = new FluidRecipeCapability();
    public final static SpriteTexture FLUID_SLOT_OVERLAY = SpriteTexture.of("mbd2:textures/gui/fluid_tank_overlay.png");

    public static final String FLUID_TYPE = "recipe.capability.fluid.ingredient.values.fluid";
    public static final String TAG_TYPE = "recipe.capability.fluid.ingredient.values.tag";

    protected FluidRecipeCapability() {
        super("fluid", SerializerSizedFluidIngredient.INSTANCE);
    }

    @Override
    public SizedFluidIngredient createDefaultContent() {
        return SizedFluidIngredient.of(Fluids.WATER, 1000);
    }

    @Override
    public UIElement createPreviewWidget(SizedFluidIngredient content) {
        return new FluidSlot().bindDataSource(ScrollDataSource.of(Arrays.stream(content.getFluids()).toList()));
    }

    @Override
    public UIElement createXEITemplate() {
        var fluidSlot = new FluidSlot();
        fluidSlot.getLayout().width(20).height(58);
        fluidSlot.getStyle().overlay(FLUID_SLOT_OVERLAY);
        fluidSlot.amountLabel.setDisplay(false);
        return fluidSlot;
    }

    @Override
    public void bindXEIWidget(UIElement widget, Content content, IO io) {
        if (widget instanceof FluidSlot fluidSlot) {
            var ingredient = of(content.content);

            var fluids = ingredient.getFluids();
            if (fluids.length == 0) return;
            if (fluids.length == 1) {
                fluidSlot.setFluid(fluids[0]);
            } else {
                fluidSlot.bindDataSource(ScrollDataSource.of(Arrays.stream(fluids).toList()));
            }

            // xei
            var ingredientIO = switch (io) {
                case IN -> IngredientIO.INPUT;
                case OUT -> IngredientIO.OUTPUT;
                default -> IngredientIO.NONE;
            };

            if (ingredientIO != IngredientIO.NONE) {
                fluidSlot.xeiRecipeIngredient(ingredientIO, Arrays.stream(ingredient.getFluids()));
            }

            fluidSlot.xeiRecipeSlot(ingredientIO, content.chance, ingredient.amount(), Arrays.stream(ingredient.getFluids()));
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<SizedFluidIngredient> supplier, Consumer<SizedFluidIngredient> onUpdate) {
        // sized ingredient amount
        father.addConfigurators(
                new NumberConfigurator("recipe.capability.fluid.ingredient.amount",
                        () -> supplier.get().amount(),
                        number -> onUpdate.accept(new SizedFluidIngredient(supplier.get().ingredient(), number.intValue())), 1, true).setRange(1, Integer.MAX_VALUE),
                new FluidIngredientConfigurator("",
                        () -> supplier.get().ingredient(),
                        ingredient -> onUpdate.accept(new SizedFluidIngredient(ingredient, supplier.get().amount())),
                        FluidIngredient.of(Fluids.WATER),
                        true)
        );
    }

    @Override
    public Component getLeftErrorInfo(List<SizedFluidIngredient> left) {
        var result = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            var fluidIngredient = left.get(i);
            result.append(fluidIngredient.amount() + "x ");
            var fluids = fluidIngredient.getFluids();
            if (fluids.length > 0) {
                result.append(fluids[0].getHoverName());
            } else {
                result.append("Unknown");
            }
            if (fluidIngredient.ingredient() instanceof DataComponentFluidIngredient) {
                result.append(" with DataComponent");
            }
            if (i < left.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }
}

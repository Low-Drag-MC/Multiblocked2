package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import me.desht.pneumaticcraft.common.core.ModItems;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PNCPressureAirRecipeCapability extends RecipeCapability<PressureAir> {
    public final static PNCPressureAirRecipeCapability CAP = new PNCPressureAirRecipeCapability();
    public static final ResourceBorderTexture HUD_BACKGROUND = new ResourceBorderTexture(
            "mbd2:textures/gui/progress_bar_boiler_empty_steel.png", 54, 10, 1, 1);
    public static final ResourceTexture HUD_BAR = new ResourceTexture("mbd2:textures/gui/pressure_air.png");
    protected PNCPressureAirRecipeCapability() {
        super("pneumatic_pressure_air", PressureAir.SerializerPressureAir.INSTANCE);
    }

    @Override
    public PressureAir createDefaultContent() {
        return new PressureAir(false, 100);
    }

    @Override
    public Widget createPreviewWidget(PressureAir content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16,
                new ItemStackTexture(ModItems.PRESSURE_GAUGE.get())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue((long) content.value()));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, HUD_BAR.copy()
        ));
        energyBar.setBackground(HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0 pressure"));
        return energyBar;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ProgressWidget energyBar) {
            var pressureAir = of(content.content);
            var unit = LocalizationUtils.format(pressureAir.isAir() ? "recipe.capability.pneumatic_pressure_air.type.air" :
                    "recipe.capability.pneumatic_pressure_air.type.pressure");
            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                if (content.perTick) {
                    textTexture.updateText(pressureAir.value() + " " + unit + "/t");
                } else {
                    textTexture.updateText(pressureAir.value() + " " + unit);
                }
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<PressureAir> supplier, Consumer<PressureAir> onUpdate) {
        var type = new SelectorConfigurator<>("recipe.capability.pneumatic_pressure_air.type", () -> supplier.get().isAir(),
                isAir -> onUpdate.accept(new PressureAir(isAir, supplier.get().value())), false, true,
                List.of(true, false), isAir -> isAir ? "recipe.capability.pneumatic_pressure_air.type.air" : "recipe.capability.pneumatic_pressure_air.type.pressure");
        type.setTips("recipe.capability.pneumatic_pressure_air.type.tooltip");
        father.addConfigurators(type, new NumberConfigurator("recipe.capability.pneumatic_pressure_air.value", () -> supplier.get().value(),
                number -> onUpdate.accept(new PressureAir(supplier.get().isAir(), number.floatValue())), 1, true)
                .setRange(1, Float.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<PressureAir> left) {
        var airValue = 0f;
        var pressureValue = 0f;
        for (PressureAir pressureAir : left) {
            if (pressureAir.isAir()) {
                airValue += pressureAir.value();
            } else {
                pressureValue += pressureAir.value();
            }
        }
        return Component.literal("[")
                .append(Component.translatable("recipe.capability.pneumatic_pressure_air.type.air"))
                .append(Component.literal(": " + airValue + "], ["))
                .append(Component.translatable("recipe.capability.pneumatic_pressure_air.type.pressure"))
                .append(Component.literal(": " + pressureValue + "]"));
    }
}

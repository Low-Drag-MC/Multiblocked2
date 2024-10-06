package com.lowdragmc.mbd2.integration.gtm;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerLong;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GTMEnergyRecipeCapability extends RecipeCapability<Long> {
    public final static GTMEnergyRecipeCapability CAP = new GTMEnergyRecipeCapability();
    public static final ResourceBorderTexture HUD_BACKGROUND = new ResourceBorderTexture(
            "mbd2:textures/gui/progress_bar_boiler_empty_steel.png", 54, 10, 1, 1);
    public static final ResourceBorderTexture HUD_BAR = new ResourceBorderTexture(
            "mbd2:textures/gui/progress_bar_boiler_heat.png", 54, 10, 1, 1);
    protected GTMEnergyRecipeCapability() {
        super("gtm_energy", SerializerLong.INSTANCE);
    }

    @Override
    public Long createDefaultContent() {
        return 128L;
    }

    @Override
    public Widget createPreviewWidget(Long content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(GTItems.BATTERY_HV_SODIUM.asItem())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, HUD_BAR.copy()
        ));
        energyBar.setBackground(HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0 eu"));
        return energyBar;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ProgressWidget energyBar) {
            var energy = of(content.content);
            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                if (content.perTick) {
                    textTexture.updateText(energy + " eu/t");
                } else {
                    textTexture.updateText(energy + " eu");
                }
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Long> supplier, Consumer<Long> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.gtm_energy.energy", supplier::get,
                number -> onUpdate.accept(number.longValue()), 1, true).setRange(1, Long.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Long> left) {
        return Component.literal(left.stream().mapToLong(Long::longValue).sum() + " eu");
    }
}

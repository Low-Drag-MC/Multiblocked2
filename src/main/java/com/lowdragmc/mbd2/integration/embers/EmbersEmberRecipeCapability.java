package com.lowdragmc.mbd2.integration.embers;

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
import com.lowdragmc.mbd2.api.recipe.content.SerializerDouble;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import com.rekindled.embers.RegistryManager;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EmbersEmberRecipeCapability extends RecipeCapability<Double> {
    public final static EmbersEmberRecipeCapability CAP = new EmbersEmberRecipeCapability();
    public static final ResourceBorderTexture HUD_BACKGROUND = new ResourceBorderTexture(
            "mbd2:textures/gui/progress_bar_boiler_empty_steel.png", 54, 10, 1, 1);
    public static final ResourceTexture HUD_BAR = new ResourceTexture("mbd2:textures/gui/ember_hud.png");
    protected EmbersEmberRecipeCapability() {
        super("embers_ember", SerializerDouble.INSTANCE);
    }

    @Override
    public Double createDefaultContent() {
        return 100d;
    }

    @Override
    public Widget createPreviewWidget(Double content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(RegistryManager.EMBER_CRYSTAL.get())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content.longValue()));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 7, new ProgressTexture(
                IGuiTexture.EMPTY, HUD_BAR
        ));
        energyBar.setBackground(HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0 ember"));
        return energyBar;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ProgressWidget energyBar) {
            var energy = of(content.content);
            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                if (content.perTick) {
                    textTexture.updateText(energy + " ember/t");
                } else {
                    textTexture.updateText(energy + " ember");
                }
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Double> supplier, Consumer<Double> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.embers_ember.ember", supplier::get,
                number -> onUpdate.accept(number.doubleValue()), 1, true).setRange(1, Double.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Double> left) {
        return Component.literal(left.stream().mapToDouble(Double::doubleValue).sum() + " ember");
    }
}

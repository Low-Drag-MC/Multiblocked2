package com.lowdragmc.mbd2.common.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.SerializerInteger;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ForgeEnergyRecipeCapability extends RecipeCapability<Integer> {
    public final static ForgeEnergyRecipeCapability CAP = new ForgeEnergyRecipeCapability();

    protected ForgeEnergyRecipeCapability() {
        super("forge_energy", SerializerInteger.INSTANCE);
    }


    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public Widget createPreviewWidget(Integer content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.setBackground(new ResourceTexture("mbd2:textures/gui/forge_energy.png"));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 28, new ProgressTexture(
               IGuiTexture.EMPTY, new ResourceTexture("mbd2:textures/gui/energy_bar_base.png")
        ));
        energyBar.setBackground(new ResourceTexture("mbd2:textures/gui/energy_bar_background.png"));
        energyBar.setOverlay(new TextTexture("0 FE"));
        return energyBar;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ProgressWidget energyBar) {
            var energy = of(content.content);
            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                if (content.perTick) {
                    textTexture.updateText(energy + " FE/t");
                } else {
                    textTexture.updateText(energy + " FE");
                }
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.forge_energy.energy", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " fe");
    }
}

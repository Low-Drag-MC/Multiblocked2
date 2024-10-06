package com.lowdragmc.mbd2.integration.botania;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
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
import com.lowdragmc.mbd2.api.recipe.content.SerializerInteger;
import com.lowdragmc.mbd2.common.gui.recipe.CornerNumberWidget;
import net.minecraft.network.chat.Component;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BotaniaManaRecipeCapability extends RecipeCapability<Integer> {
    public final static BotaniaManaRecipeCapability CAP = new BotaniaManaRecipeCapability();
    public final static ResourceTexture HUD_BACKGROUND= new ResourceTexture("mbd2:textures/gui/mana_hud.png").getSubTexture(0, 0, 1, 0.5);
    public final static ResourceTexture HUD_BAR= new ResourceTexture("mbd2:textures/gui/mana_hud.png").getSubTexture(0, 0.5, 1, 0.5);
    protected BotaniaManaRecipeCapability() {
        super("botania_mana", SerializerInteger.INSTANCE);
    }

    @Override
    public Integer createDefaultContent() {
        return 512;
    }

    @Override
    public Widget createPreviewWidget(Integer content) {
        var previewGroup = new WidgetGroup(0, 0, 18, 18);
        previewGroup.addWidget(new ImageWidget(1, 1, 16, 16, new ItemStackTexture(BotaniaBlocks.manaPool.asItem())));
        previewGroup.addWidget(new CornerNumberWidget(0, 0, 18, 18).setValue(content));
        return previewGroup;
    }

    @Override
    public Widget createXEITemplate() {
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 5, new ProgressTexture(
                IGuiTexture.EMPTY, HUD_BAR.copy().setColor(ColorPattern.LIGHT_BLUE.color)
        ));
        energyBar.setBackground(HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0 mana"));
        return energyBar;
    }

    @Override
    public void bindXEIWidget(Widget widget, Content content, IngredientIO ingredientIO) {
        if (widget instanceof ProgressWidget energyBar) {
            var energy = of(content.content);
            if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                if (content.perTick) {
                    textTexture.updateText(energy + " mana/t");
                } else {
                    textTexture.updateText(energy + " mana");
                }
            }
        }
    }

    @Override
    public void createContentConfigurator(ConfiguratorGroup father, Supplier<Integer> supplier, Consumer<Integer> onUpdate) {
        father.addConfigurators(new NumberConfigurator("recipe.capability.botania_mana.mana", supplier::get,
                number -> onUpdate.accept(number.intValue()), 1, true).setRange(1, Integer.MAX_VALUE));
    }

    @Override
    public Component getLeftErrorInfo(List<Integer> left) {
        return Component.literal(left.stream().mapToInt(Integer::intValue).sum() + " mana");
    }
}

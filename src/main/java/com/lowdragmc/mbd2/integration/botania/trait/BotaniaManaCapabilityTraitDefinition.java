package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.BotaniaBlocks;

@LDLRegister(name = "botania_mana_storage", group = "trait", modID = "botania")
public class BotaniaManaCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<ManaPool, Integer> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.botania_mana_storage.capacity")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int capacity = 5000;
    @Configurable(name = "config.definition.trait.botania_mana_storage.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.botania_mana_storage.fancy_renderer.tooltip")
    private final BotaniaManaFancyRendererSettings fancyRendererSettings = new BotaniaManaFancyRendererSettings(this);

    @Override
    public SimpleCapabilityTrait<ManaPool, Integer> createTrait(MBDMachine machine) {
        return new BotaniaManaCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(BotaniaBlocks.manaPool.asItem());
    }

    @Override
    public RecipeCapability<Integer> getRecipeCapability() {
        return BotaniaManaRecipeCapability.CAP;
    }

    @Override
    public Capability<ManaReceiver> getCapability() {
        return BotaniaForgeCapabilities.MANA_RECEIVER;
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 5, 100, 5, new ProgressTexture(
                IGuiTexture.EMPTY, BotaniaManaRecipeCapability.HUD_BAR.copy().setColor(ColorPattern.LIGHT_BLUE.color)
        ));
        energyBar.setBackground(BotaniaManaRecipeCapability.HUD_BACKGROUND);
        energyBar.setId(prefix);
        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
                .setText("0/0 mana")
                .textureStyle(textTexture -> textTexture.setDropShadow(true));
        energyBarText.setId(prefix + "_text");
        ui.addWidget(energyBar);
        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof BotaniaManaCapabilityTrait manaTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> manaTrait.storage.getCurrentMana() * 1d / manaTrait.storage.getMaxMana());
                energyBar.setDynamicHoverTips(progress -> LocalizationUtils.format(
                        "config.definition.trait.botania_mana_storage.ui_container_hover",
                        Math.round(manaTrait.storage.getMaxMana() * progress), manaTrait.storage.getMaxMana()));
            });
            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
                energyBarText.setText(() -> Component.literal(manaTrait.storage.getCurrentMana() + "/" + manaTrait.storage.getMaxMana() + " mana"));
            });
        }
    }
}

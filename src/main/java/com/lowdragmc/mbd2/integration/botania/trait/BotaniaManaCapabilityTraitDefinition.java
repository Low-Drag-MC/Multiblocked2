package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.capabilities.Capability;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.BotaniaBlocks;

@LDLRegister(name = "botania_mana_storage", group = "trait", priority = -100)
public class BotaniaManaCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<ManaPool, Integer> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.botania_mana_storage.capacity")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int capacity = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.botania_mana_storage.emit_speed", tips = "config.definition.trait.botania_mana_storage.emit_speed.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int emitSpeed = 5000;
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
        return ForgeEnergyRecipeCapability.CAP;
    }

    @Override
    public Capability<ManaReceiver> getCapability() {
        return BotaniaForgeCapabilities.MANA_RECEIVER;
    }

    @Override
    public IRenderer getBESRenderer() {
        return fancyRendererSettings.createRenderer();
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 5, new ProgressTexture(
                IGuiTexture.EMPTY, BotaniaManaRecipeCapability.HUD_BAR.copy().setColor(ColorPattern.LIGHT_BLUE.color)
        ));
        energyBar.setBackground(BotaniaManaRecipeCapability.HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0/0 mana"));
        energyBar.setId(prefix);
        ui.addWidget(energyBar);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof BotaniaManaCapabilityTrait forgeEnergyTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> forgeEnergyTrait.storage.getCurrentMana() * 1d / forgeEnergyTrait.storage.getMaxMana());
                if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                    textTexture.updateText(forgeEnergyTrait.storage.getCurrentMana() + "/" + forgeEnergyTrait.storage.getMaxMana() + " mana");
                }
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.botania_mana_storage.ui_container_hover",
                        forgeEnergyTrait.storage.getCurrentMana(), forgeEnergyTrait.storage.getMaxMana()));
            });
        }
    }
}

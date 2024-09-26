package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

@LDLRegister(name = "forge_energy_storage", group = "trait", priority = -100)
public class ForgeEnergyCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IEnergyStorage, Integer> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.capacity")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int capacity = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.max_receive", tips = "config.definition.trait.forge_energy_storage.max_receive.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int maxReceive = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.max_extract", tips = "config.definition.trait.forge_energy_storage.max_extract.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int maxExtract = 5000;
    @Configurable(name = "config.definition.trait.forge_energy_storage.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.forge_energy_storage.fancy_renderer.tooltip")
    private final ForgeEnergyFancyRendererSettings fancyRendererSettings = new ForgeEnergyFancyRendererSettings(this);

    @Override
    public SimpleCapabilityTrait<IEnergyStorage, Integer> createTrait(MBDMachine machine) {
        return new ForgeEnergyCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ResourceTexture("mbd2:textures/gui/forge_energy.png");
    }

    @Override
    public RecipeCapability<Integer> getRecipeCapability() {
        return ForgeEnergyRecipeCapability.CAP;
    }

    @Override
    public Capability<IEnergyStorage> getCapability() {
        return ForgeCapabilities.ENERGY;
    }

    @Override
    public IRenderer getBESRenderer() {
        return fancyRendererSettings.createRenderer();
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 28, new ProgressTexture(
                IGuiTexture.EMPTY, new ResourceTexture("mbd2:textures/gui/energy_bar_base.png")
        ));
        energyBar.setBackground(new ResourceTexture("mbd2:textures/gui/energy_bar_background.png"));
        energyBar.setId(prefix);
        var energyBarText = new TextTextureWidget(5, 9, 90, 10, "0/0 FE");
        energyBarText.setId(prefix + "_text");
        ui.addWidget(energyBar);
        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof ForgeEnergyCapabilityTrait forgeEnergyTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> forgeEnergyTrait.storage.getEnergyStored() * 1d / forgeEnergyTrait.storage.getMaxEnergyStored());
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.forge_energy_storage.ui_container_hover",
                        forgeEnergyTrait.storage.getMaxEnergyStored() * value, forgeEnergyTrait.storage.getMaxEnergyStored()));
            });
            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
                energyBarText.setText(() -> Component.literal(forgeEnergyTrait.storage.getEnergyStored() + "/" + forgeEnergyTrait.storage.getMaxEnergyStored() + " FE"));
            });
        }
    }
}

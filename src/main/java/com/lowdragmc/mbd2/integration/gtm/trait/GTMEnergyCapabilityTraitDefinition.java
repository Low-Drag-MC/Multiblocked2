package com.lowdragmc.mbd2.integration.gtm.trait;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
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
import com.lowdragmc.mbd2.integration.gtm.GTMEnergyRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;

@LDLRegister(name = "gtm_energy_container", group = "trait", modID = "gtceu")
public class GTMEnergyCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IEnergyContainer, Long> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.capacity")
    @NumberRange(range = {1, Long.MAX_VALUE})
    private long capacity = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.explosion_machine", tips = "config.definition.trait.gtm_energy_container.explosion_machine.tooltip")
    private boolean explosionMachine = false;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.input_voltage", tips = "config.definition.trait.gtm_energy_container.input_voltage.tooltip")
    @NumberRange(range = {0, Long.MAX_VALUE})
    private long inputVoltage = 128;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.input_amperage", tips = "config.definition.trait.gtm_energy_container.input_amperage.tooltip")
    @NumberRange(range = {0, Long.MAX_VALUE})
    private long inputAmperage = 1;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.output_voltage", tips = "config.definition.trait.gtm_energy_container.output_voltage.tooltip")
    @NumberRange(range = {0, Long.MAX_VALUE})
    private long outputVoltage = 128;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.gtm_energy_container.output_amperage", tips = "config.definition.trait.gtm_energy_container.output_amperage.tooltip")
    @NumberRange(range = {0, Long.MAX_VALUE})
    private long outputAmperage = 1;

    @Configurable(name = "config.definition.trait.gtm_energy_container.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.gtm_energy_container.fancy_renderer.tooltip")
    private final GTMEnergyFancyRendererSettings fancyRendererSettings = new GTMEnergyFancyRendererSettings(this);

    @Override
    public SimpleCapabilityTrait<IEnergyContainer, Long> createTrait(MBDMachine machine) {
        return new GTMEnergyCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(GTItems.BATTERY_HV_SODIUM.asItem());
    }

    @Override
    public RecipeCapability<Long> getRecipeCapability() {
        return GTMEnergyRecipeCapability.CAP;
    }

    @Override
    public Capability<IEnergyContainer> getCapability() {
        return GTCapability.CAPABILITY_ENERGY_CONTAINER;
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, GTMEnergyRecipeCapability.HUD_BAR
        ));
        energyBar.setBackground(GTMEnergyRecipeCapability.HUD_BACKGROUND);
        energyBar.setId(prefix);
        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
                .setText("0/0 eu")
                .textureStyle(textTexture -> textTexture.setDropShadow(true));
        energyBarText.setId(prefix + "_text");
        ui.addWidget(energyBar);
        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof GTMEnergyCapabilityTrait energyTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> energyTrait.container.getEnergyStored() * 1d / energyTrait.container.getEnergyCapacity());
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.gtm_energy_container.ui_container_hover",
                        Math.round(energyTrait.container.getEnergyCapacity() * value), energyTrait.container.getEnergyCapacity()));
            });
            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
                energyBarText.setText(() -> Component.literal(energyTrait.container.getEnergyStored() + "/" + energyTrait.container.getEnergyCapacity() + " eu"));
            });
        }
    }
}

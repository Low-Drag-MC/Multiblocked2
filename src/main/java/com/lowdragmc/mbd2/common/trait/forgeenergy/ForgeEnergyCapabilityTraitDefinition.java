package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.fluid.FluidHandlerList;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.utils.EnergyFormattingUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import static com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability.ENERGY_BAR;
import static com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability.ENERGY_BASE;

@LDLRegister(name = "forge_energy_storage", registry = "mbd2:trait_definition_type", group = "trait", priority = -100)
public class ForgeEnergyCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IEnergyStorage, @Nullable Direction> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int capacity = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.max_receive", tips = "config.definition.trait.forge_energy_storage.max_receive.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxReceive = 5000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.forge_energy_storage.max_extract", tips = "config.definition.trait.forge_energy_storage.max_extract.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxExtract = 5000;
    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.forge_energy_storage.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Configurable(name = "config.definition.trait.forge_energy_storage.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.forge_energy_storage.fancy_renderer.tooltip")
    private final ForgeEnergyFancyRendererSettings fancyRendererSettings = new ForgeEnergyFancyRendererSettings(this);

    @Override
    public ForgeEnergyCapabilityTrait createTrait(MBDMachine machine) {
        return new ForgeEnergyCapabilityTrait(machine, this);
    }

    @Override
    protected @Nullable IEnergyStorage getCapContent(MBDMachine machine, @Nullable Direction context) {
        return new EnergyStorageList(machine.getAdditionalTraits().stream().filter(trait -> trait instanceof ForgeEnergyCapabilityTrait)
                .map(ForgeEnergyCapabilityTrait.class::cast)
                .map(trait -> trait.getCapContent(trait.getCapabilityIO(context)))
                .toArray(IEnergyStorage[]::new));
    }

    @Override
    public BlockCapability<IEnergyStorage, @Nullable Direction> getCapability() {
        return Capabilities.EnergyStorage.BLOCK;
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of("mbd2:textures/gui/forge_energy.png");
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(UI ui) {
        // todo ui

//        var prefix = uiPrefixName();
//        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 14, new ProgressTexture(
//                IGuiTexture.EMPTY, ENERGY_BAR
//        ));
//        energyBar.setBackground(ENERGY_BASE);
//        energyBar.setId(prefix);
//        var energyBarText = new TextTextureWidget(5, 2, 90, 10)
//                .setText("0/0 FE")
//                .textureStyle(textTexture -> textTexture.setDropShadow(true));
//        energyBarText.setId(prefix + "_text");
//        ui.addWidget(energyBar);
//        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
//        if (trait instanceof ForgeEnergyCapabilityTrait forgeEnergyTrait) {
//            var prefix = uiPrefixName();
//            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
//                energyBar.setProgressSupplier(() -> forgeEnergyTrait.storage.getEnergyStored() * 1d / forgeEnergyTrait.storage.getMaxEnergyStored());
//                energyBar.setDynamicHoverTips(value -> {
//                    var stored = EnergyFormattingUtil.formatExtended(Math.round(forgeEnergyTrait.storage.getMaxEnergyStored() * value));
//                    var maxStored = EnergyFormattingUtil.formatExtended(forgeEnergyTrait.storage.getMaxEnergyStored());
//                    return LocalizationUtils.format("config.definition.trait.forge_energy_storage.ui_container_hover", stored, maxStored);
//                });
//            });
//            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
//                energyBarText.setText(() -> {
//                    var stored = EnergyFormattingUtil.formatCompact(forgeEnergyTrait.storage.getEnergyStored()) + "FE";
//                    var maxStored = EnergyFormattingUtil.formatCompact(forgeEnergyTrait.storage.getMaxEnergyStored()) + "FE";
//                    return Component.literal(stored + "/" + maxStored);
//                });
//            });
//        }
    }
}

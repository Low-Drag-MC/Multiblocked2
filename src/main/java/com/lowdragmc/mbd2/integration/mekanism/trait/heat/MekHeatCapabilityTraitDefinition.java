//package com.lowdragmc.mbd2.integration.mekanism.trait.heat;
//
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
//import com.lowdragmc.lowdraglib2.gui.editor.annotation.NumberRange;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ProgressTexture;
//import com.lowdragmc.lowdraglib2.gui.widget.ProgressWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.TextTextureWidget;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.ITrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
//import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
//import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
//import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
//import com.lowdragmc.mbd2.utils.WidgetUtils;
//import lombok.Getter;
//import lombok.Setter;
//import mekanism.common.registries.MekanismBlocks;
//import net.minecraft.network.chat.Component;
//import net.minecraft.util.Mth;
//
//@Setter
//@Getter
//@LDLRegister(name = "mek_heat_container", group = "trait", modID = "mekanism")
//public class MekHeatCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition {
//
//    @Configurable(name = "config.definition.trait.mek_heat_container.capacity")
//    @ConfigNumber(range = {1, Double.MAX_VALUE})
//    private double capacity = 1;
//    @Configurable(name = "config.definition.trait.mek_heat_container.min_temp_display",
//            tips = {
//                    "config.definition.trait.mek_heat_container.min_temp_display.tooltip.0",
//                    "config.definition.trait.mek_heat_container.min_temp_display.tooltip.1"
//            })
//    @ConfigNumber(range = {-Double.MAX_VALUE, Double.MAX_VALUE})
//    private double minTempDisplay = 0;
//    @Configurable(name = "config.definition.trait.mek_heat_container.max_temp_display",
//            tips = {
//                    "config.definition.trait.mek_heat_container.max_temp_display.tooltip.0",
//                    "config.definition.trait.mek_heat_container.max_temp_display.tooltip.1"
//            })
//    @ConfigNumber(range = {-Double.MAX_VALUE, Double.MAX_VALUE})
//    private double maxTempDisplay = 600;
//    @Configurable(name = "config.definition.trait.mek_heat_container.inverse_conduction",
//            tips = "config.definition.trait.mek_heat_container.inverse_conduction.tooltip")
//    @ConfigNumber(range = {1d, Double.MAX_VALUE})
//    private double inverseConduction = 1d;
//    @Getter
//    @Configurable(name = "config.definition.trait.mek_heat_container.simulate_environment", tips = "config.definition.trait.mek_heat_container.simulate_environment.tooltip")
//    private boolean simulateEnvironment = false;
//    @Getter
//    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.mek_heat_container.auto_io.tooltip")
//    private final ToggleAutoIO autoIO = new ToggleAutoIO();
//
//    @Override
//    public SimpleCapabilityTrait createTrait(MBDMachine machine) {
//        return new MekHeatCapabilityTrait(machine, this);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(MekanismBlocks.RESISTIVE_HEATER.getItemStack());
//    }
//
//    @Override
//    public void createTraitUITemplate(WidgetGroup ui) {
//        var prefix = uiPrefixName();
//        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
//                IGuiTexture.EMPTY, MekanismHeatRecipeCapability.HUD_BAR
//        ));
//        energyBar.setBackground(MekanismHeatRecipeCapability.HUD_BACKGROUND);
//        energyBar.setId(prefix);
//        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
//                .setText("0K")
//                .textureStyle(texture -> texture.setDropShadow(true));
//        energyBarText.setId(prefix + "_text");
//        ui.addWidget(energyBar);
//        ui.addWidget(energyBarText);
//    }
//
//    @Override
//    public void initTraitUI(ITrait trait, WidgetGroup group) {
//        if (trait instanceof MekHeatCapabilityTrait heatTrait) {
//            var prefix = uiPrefixName();
//            var range = maxTempDisplay - minTempDisplay;
//            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
//                energyBar.setProgressSupplier(() -> Mth.clamp((heatTrait.container.getTemperature(0) - minTempDisplay) / range, 0, 1));
//                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
//                        "config.definition.trait.mek_heat_container.ui_container_hover",
//                        Math.round(range * value), range));
//            });
//            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
//                energyBarText.setText(() -> Component.literal("%.1fK".formatted(heatTrait.container.getTotalTemperature())));
//            });
//        }
//    }
//}

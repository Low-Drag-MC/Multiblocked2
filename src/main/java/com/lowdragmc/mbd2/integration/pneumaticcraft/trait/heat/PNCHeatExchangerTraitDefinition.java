//package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;
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
//import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
//import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
//import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
//import com.lowdragmc.mbd2.utils.WidgetUtils;
//import lombok.Getter;
//import lombok.Setter;
//import me.desht.pneumaticcraft.common.core.ModItems;
//import net.minecraft.network.chat.Component;
//
//@LDLRegister(name = "pneumatic_heat_exchanger", group = "trait", modID = "pneumaticcraft")
//public class PNCHeatExchangerTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_heat_exchanger.thermal_capacity",
//            tips = {"config.definition.trait.pneumatic_heat_exchanger.thermal_capacity.tips.0",
//                    "config.definition.trait.pneumatic_heat_exchanger.thermal_capacity.tips.1"})
//    @ConfigNumber(range = {0, Double.MAX_VALUE})
//    private float thermalCapacity = 1;
//
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_heat_exchanger.thermal_resistance",
//            tips = "config.definition.trait.pneumatic_heat_exchanger.thermal_resistance.tips")
//    @ConfigNumber(range = {0, Double.MAX_VALUE})
//    private float thermalResistance= 1;
//
//    @Override
//    public PNCHeatExchangerTrait createTrait(MBDMachine machine) {
//        return new PNCHeatExchangerTrait(machine, this);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(ModItems.HEAT_FRAME.get());
//    }
//
//    @Override
//    public boolean allowMultiple() {
//        return false;
//    }
//
//    @Override
//    public void createTraitUITemplate(WidgetGroup ui) {
//        var prefix = uiPrefixName();
//        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 10, new ProgressTexture(
//                IGuiTexture.EMPTY, PNCHeatRecipeCapability.HUD_BAR
//        ));
//        energyBar.setBackground(PNCHeatRecipeCapability.HUD_BACKGROUND);
//        energyBar.setId(prefix);
//        var energyBarText = new TextTextureWidget(5, 0, 90, 10)
//                .setText("0 temperature")
//                .textureStyle(texture -> texture.setDropShadow(true));
//        energyBarText.setId(prefix + "_text");
//        ui.addWidget(energyBar);
//        ui.addWidget(energyBarText);
//    }
//
//    @Override
//    public void initTraitUI(ITrait trait, WidgetGroup group) {
//        if (trait instanceof PNCHeatExchangerTrait heatTrait) {
//            var prefix = uiPrefixName();
//            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
//                energyBar.setProgressSupplier(() -> heatTrait.getHandler().getTemperature() / 2273);
//                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
//                        "config.definition.trait.pneumatic_heat_exchanger.ui_container_hover",
//                        Math.round(2273 * value) - 273));
//            });
//            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
//                energyBarText.setText(() -> Component.literal(Math.round(heatTrait.getHandler().getTemperature()) - 273 + "°C"));
//            });
//        }
//    }
//}

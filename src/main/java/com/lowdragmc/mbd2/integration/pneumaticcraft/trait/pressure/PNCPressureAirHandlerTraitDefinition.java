//package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;
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
//import com.lowdragmc.mbd2.common.trait.*;
//import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
//import com.lowdragmc.mbd2.utils.WidgetUtils;
//import lombok.Getter;
//import lombok.Setter;
//import me.desht.pneumaticcraft.api.pressure.PressureTier;
//import me.desht.pneumaticcraft.common.core.ModItems;
//import me.desht.pneumaticcraft.common.inventory.CreativeCompressedIronBlockMenu;
//import net.minecraft.network.chat.Component;
//
//@LDLRegister(name = "pneumatic_pressure_air_handler", group = "trait", modID = "pneumaticcraft")
//public class PNCPressureAirHandlerTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.volume")
//    @ConfigNumber(range = {1, Double.MAX_VALUE})
//    private int volume = 1;
//
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.max_pressure",
//            tips = "config.definition.trait.pneumatic_pressure_air_handler.max_pressure.tips")
//    @ConfigNumber(range = {1, Double.MAX_VALUE})
//    private float maxPressure = 20f;
//
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.danger_pressure",
//            tips = {"config.definition.trait.pneumatic_pressure_air_handler.danger_pressure.tips.0",
//                    "config.definition.trait.pneumatic_pressure_air_handler.danger_pressure.tips.1"})
//    @ConfigNumber(range = {0, Double.MAX_VALUE})
//    private float dangerPressure = 0;
//
//    @Getter
//    @Setter
//    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.critical_pressure",
//            tips = {"config.definition.trait.pneumatic_pressure_air_handler.critical_pressure.tips.0",
//                    "config.definition.trait.pneumatic_pressure_air_handler.critical_pressure.tips.1"})
//    @ConfigNumber(range = {0, Double.MAX_VALUE})
//    private float criticalPressure = 0;
//    @Getter
//    @Configurable(name = "config.definition.trait.connected_io", subConfigurable = true, tips = "config.definition.trait.pneumatic_pressure_air_handler.connected_io.tooltip")
//    private final ConnectedIO connectionIO = new ConnectedIO();
//
//    @Override
//    public PNCPressureAirHandlerTrait createTrait(MBDMachine machine) {
//        return new PNCPressureAirHandlerTrait(machine, this);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(ModItems.PRESSURE_GAUGE.get());
//    }
//
//    @Override
//    public boolean allowMultiple() {
//        return false;
//    }
//
//    public float getRealDangerPressure() {
//        return dangerPressure == 0 ? maxPressure : dangerPressure;
//    }
//
//    public float getRealCriticalPressure() {
//        return criticalPressure == 0 ? getRealDangerPressure() : criticalPressure;
//    }
//
//    public PressureTier getPressureTier() {
//        return new PressureTier() {
//            @Override
//            public float getDangerPressure() {
//                return getRealDangerPressure();
//            }
//
//            @Override
//            public float getCriticalPressure() {
//                return getRealCriticalPressure();
//            }
//        };
//    }
//
//    @Override
//    public void createTraitUITemplate(WidgetGroup ui) {
//        var prefix = uiPrefixName();
//        var pressureBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
//                IGuiTexture.EMPTY, PNCPressureAirRecipeCapability.HUD_BAR
//        ));
//        pressureBar.setBackground(PNCPressureAirRecipeCapability.HUD_BACKGROUND);
//        pressureBar.setId(prefix);
//        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
//                .setText("0 pressure")
//                .textureStyle(texture -> texture.setDropShadow(true));
//        energyBarText.setId(prefix + "_text");
//        ui.addWidget(pressureBar);
//        ui.addWidget(energyBarText);
//    }
//
//    @Override
//    public void initTraitUI(ITrait trait, WidgetGroup group) {
//        if (trait instanceof PNCPressureAirHandlerTrait pressureAirHandlerTrait) {
//            var prefix = uiPrefixName();
//            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
//                energyBar.setProgressSupplier(() -> Math.max(pressureAirHandlerTrait.handler.getPressure(), 0) / pressureAirHandlerTrait.handler.maxPressure());
//                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
//                        "config.definition.trait.pneumatic_pressure_air_handler.ui_container_hover",
//                        Math.round(pressureAirHandlerTrait.handler.maxPressure() * value), pressureAirHandlerTrait.handler.maxPressure()));
//            });
//            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
//                energyBarText.setText(() -> Component.literal(Math.round(pressureAirHandlerTrait.handler.getPressure()) + " pressure"));
//            });
//        }
//    }
//}

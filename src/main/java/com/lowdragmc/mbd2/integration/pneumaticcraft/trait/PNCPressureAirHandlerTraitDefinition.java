package com.lowdragmc.mbd2.integration.pneumaticcraft.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.common.core.ModItems;
import net.minecraft.network.chat.Component;

@LDLRegister(name = "pneumatic_pressure_air_handler", group = "trait", modID = "pneumaticcraft")
public class PNCPressureAirHandlerTraitDefinition extends RecipeCapabilityTraitDefinition<PressureAir> implements IUIProviderTrait {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.volume")
    @NumberRange(range = {1, Double.MAX_VALUE})
    private int volume = 1;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.max_pressure")
    @NumberRange(range = {1, Double.MAX_VALUE})
    private float maxPressure = 20f;

    @Override
    public PNCPressureAirHandlerTrait createTrait(MBDMachine machine) {
        return new PNCPressureAirHandlerTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModItems.PRESSURE_GAUGE.get());
    }

    @Override
    public RecipeCapability<PressureAir> getRecipeCapability() {
        return PNCPressureAirRecipeCapability.CAP;
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var pressureBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, PNCPressureAirRecipeCapability.HUD_BAR
        ));
        pressureBar.setBackground(PNCPressureAirRecipeCapability.HUD_BACKGROUND);
        pressureBar.setId(prefix);
        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
                .setText("0 pressure")
                .textureStyle(texture -> texture.setDropShadow(true));
        energyBarText.setId(prefix + "_text");
        ui.addWidget(pressureBar);
        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof PNCPressureAirHandlerTrait pressureAirHandlerTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> Math.max(pressureAirHandlerTrait.handler.getPressure(), 0) / pressureAirHandlerTrait.handler.maxPressure());
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.gtm_energy_container.ui_container_hover",
                        Math.round(pressureAirHandlerTrait.handler.maxPressure() * value), pressureAirHandlerTrait.handler.maxPressure()));
            });
            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
                energyBarText.setText(() -> Component.literal(pressureAirHandlerTrait.handler.getPressure() + " pressure"));
            });
        }
    }
}

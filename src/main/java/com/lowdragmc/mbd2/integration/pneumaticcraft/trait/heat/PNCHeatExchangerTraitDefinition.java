package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PNCHeatExchangerTraitDefinition extends SimpleCapabilityTraitDefinition<IHeatExchangerLogic, @Nullable Direction> {
    @LDLRegister(name = "pneumatic_heat_exchanger", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "pneumaticcraft")
    public static final SimpleCapabilityTraitDefinition.Type<IHeatExchangerLogic, @Nullable Direction, PNCHeatExchangerTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("pneumatic_heat_exchanger", "trait") {
                @Override
                public PNCHeatExchangerTraitDefinition createDefinition() {
                    return new PNCHeatExchangerTraitDefinition();
                }

                @Override
                protected BlockCapability<IHeatExchangerLogic, @Nullable Direction> getCapability() {
                    return PNCCapabilities.HEAT_EXCHANGER_BLOCK;
                }

                @Override
                protected IHeatExchangerLogic merge(List<IHeatExchangerLogic> contents) {
                    return contents.isEmpty() ? null : contents.getFirst();
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_heat_exchanger.thermal_capacity",
            tips = {"config.definition.trait.pneumatic_heat_exchanger.thermal_capacity.tips.0",
                    "config.definition.trait.pneumatic_heat_exchanger.thermal_capacity.tips.1"})
    @ConfigNumber(range = {0, Double.MAX_VALUE})
    private double thermalCapacity = 1;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_heat_exchanger.thermal_resistance",
            tips = "config.definition.trait.pneumatic_heat_exchanger.thermal_resistance.tips")
    @ConfigNumber(range = {0, Double.MAX_VALUE})
    private double thermalResistance = 1;

    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true,
            tips = "config.definition.trait.pneumatic_heat_exchanger.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();

    @Override
    public PNCHeatExchangerTrait createTrait(MBDMachine machine) {
        return new PNCHeatExchangerTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(ModItems.HEAT_FRAME.get()));
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var progress = new ProgressBar();

        progress.getLayout().height(10).width(100);
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(MBDSprites.PNC_HEAT_BG);
        progress.getProgressBarStyle().interpolate(false);
        progress.bar.getStyle().background(IGuiTexture.EMPTY).overflowVisible(false);
        progress.bar.addChild(new UIElement().layout(l -> l.height(10).width(100))
                .style(s -> s.background(MBDSprites.PNC_HEAT_BAR)));
        progress.setProgress(0f);
        progress.label.setText("0°C");
        progress.setId(uiId());
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof PNCHeatExchangerTrait heatTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(bar -> {
                bar.bind(DataBindingBuilder.floatValS2C(() ->
                        (float) Math.min(1.0, Math.max(0.0, heatTrait.getHandler().getTemperature() / 2273.0))
                ).build());

                var temperature = new AtomicReference<>((float) heatTrait.getHandler().getTemperature());
                var temperatureSync = DataBindingBuilder.floatValS2C(() -> (float) heatTrait.getHandler().getTemperature())
                        .remoteSetter(temperature::set)
                        .build();
                bar.addSyncValue(temperatureSync.getSyncValue());

                bar.label.bindDataSource(SupplierDataSource.of(() ->
                        Component.literal(String.format("%.1f°C", temperature.get() - 273f))));

                bar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    var t = temperature.get();
                    event.hoverTooltips = new HoverTooltips(List.of(
                            Component.literal(String.format("Temperature: %.2f°C (%.2f K)", t - 273f, t))),
                            null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

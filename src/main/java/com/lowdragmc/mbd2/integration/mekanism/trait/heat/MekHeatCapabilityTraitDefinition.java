package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

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
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MekHeatCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IHeatHandler, @Nullable Direction> {
    @LDLRegister(name = "mek_heat_container", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "mekanism")
    public static final SimpleCapabilityTraitDefinition.Type<IHeatHandler, @Nullable Direction, MekHeatCapabilityTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("mek_heat_container", "trait") {
                @Override
                public MekHeatCapabilityTraitDefinition createDefinition() {
                    return new MekHeatCapabilityTraitDefinition();
                }

                @Override
                protected BlockCapability<IHeatHandler, @Nullable Direction> getCapability() {
                    return mekanism.common.capabilities.Capabilities.HEAT;
                }

                @Override
                protected IHeatHandler merge(List<IHeatHandler> contents) {
                    return new HeatContainerList(contents.toArray(IHeatHandler[]::new));
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.mek_heat_container.heat_capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private double heatCapacity = 1000;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.mek_heat_container.inverse_conduction")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private double inverseConductionCoefficient = HeatAPI.DEFAULT_INVERSE_CONDUCTION;

    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.mek_heat_container.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();

    @Override
    public MekHeatCapabilityTrait createTrait(MBDMachine machine) {
        return new MekHeatCapabilityTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(MekanismBlocks.RESISTIVE_HEATER));
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var progress = new ProgressBar();
        progress.setProgress(0f);
        progress.label.setText("0K");
        progress.setId(uiId());
        progress.layout(layout -> layout.flex(1).height(14));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof MekHeatCapabilityTrait heatTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(bar -> {
                bar.bind(DataBindingBuilder.floatValS2C(() -> {
                    var temperature = heatTrait.getStorage().getTemperature(0);
                    var max = Math.max(temperature, HeatAPI.AMBIENT_TEMP * 4);
                    return (float) Math.min(1.0, temperature / max);
                }).build());

                var temperatureValue = DataBindingBuilder.floatValS2C(() ->
                        (float) heatTrait.getStorage().getTemperature(0)).build();
                bar.addSyncValue(temperatureValue.getSyncValue());

                bar.label.bindDataSource(SupplierDataSource.of(() ->
                        Component.literal(String.format("%.1fK", (double) temperatureValue.getSyncValue().getValue()))));

                bar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    var t = heatTrait.getStorage().getTemperature(0);
                    event.hoverTooltips = new HoverTooltips(List.of(
                            Component.literal(String.format("Temperature: %.2f K", t))), null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

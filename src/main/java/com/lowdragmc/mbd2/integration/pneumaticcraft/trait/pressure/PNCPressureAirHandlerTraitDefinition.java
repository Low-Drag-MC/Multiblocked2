package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

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
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ConnectedIO;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PNCPressureAirHandlerTraitDefinition extends SimpleCapabilityTraitDefinition<IAirHandlerMachine, @Nullable Direction> {
    @LDLRegister(name = "pneumatic_pressure_air_handler", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "pneumaticcraft")
    public static final SimpleCapabilityTraitDefinition.Type<IAirHandlerMachine, @Nullable Direction, PNCPressureAirHandlerTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("pneumatic_pressure_air_handler", "trait") {
                @Override
                public PNCPressureAirHandlerTraitDefinition createDefinition() {
                    return new PNCPressureAirHandlerTraitDefinition();
                }

                @Override
                protected BlockCapability<IAirHandlerMachine, @Nullable Direction> getCapability() {
                    return PNCCapabilities.AIR_HANDLER_MACHINE;
                }

                @Override
                protected IAirHandlerMachine merge(List<IAirHandlerMachine> contents) {
                    return contents.isEmpty() ? null : contents.getFirst();
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.volume")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int volume = 1;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.max_pressure",
            tips = "config.definition.trait.pneumatic_pressure_air_handler.max_pressure.tips")
    @ConfigNumber(range = {1, Float.MAX_VALUE})
    private float maxPressure = 20f;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.danger_pressure",
            tips = {"config.definition.trait.pneumatic_pressure_air_handler.danger_pressure.tips.0",
                    "config.definition.trait.pneumatic_pressure_air_handler.danger_pressure.tips.1"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float dangerPressure = 0;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.critical_pressure",
            tips = {"config.definition.trait.pneumatic_pressure_air_handler.critical_pressure.tips.0",
                    "config.definition.trait.pneumatic_pressure_air_handler.critical_pressure.tips.1"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float criticalPressure = 0;

    @Getter
    @Configurable(name = "config.definition.trait.pneumatic_pressure_air_handler.connection_io", subConfigurable = true,
            tips = "config.definition.trait.pneumatic_pressure_air_handler.connection_io.tooltip")
    private final ConnectedIO connectionIO = new ConnectedIO();

    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true,
            tips = "config.definition.trait.pneumatic_pressure_air_handler.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();

    @Override
    public PNCPressureAirHandlerTrait createTrait(MBDMachine machine) {
        return new PNCPressureAirHandlerTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(ModItems.PRESSURE_GAUGE.get()));
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    public float getRealDangerPressure() {
        return dangerPressure == 0 ? maxPressure : dangerPressure;
    }

    public float getRealCriticalPressure() {
        return criticalPressure == 0 ? getRealDangerPressure() : criticalPressure;
    }

    public PressureTier getPressureTier() {
        return new PressureTier() {
            @Override
            public float getDangerPressure() {
                return getRealDangerPressure();
            }

            @Override
            public float getCriticalPressure() {
                return getRealCriticalPressure();
            }
        };
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var progress = new ProgressBar();
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(MCSprites.RECT_1);
        progress.bar.getStyle().background(MBDSprites.PRESSURE_AIR_BAR);
        progress.setProgress(0f);
        progress.label.setText("0 bar");
        progress.setId(uiId());
        progress.layout(layout -> layout.height(14));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof PNCPressureAirHandlerTrait pressureTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(bar -> {
                bar.bind(DataBindingBuilder.floatValS2C(() ->
                        (float) Math.clamp(
                                pressureTrait.getHandler().getPressure() / pressureTrait.getHandler().maxPressure(), 0.0, 1.0)
                ).build());

                var pressure = new AtomicReference<>(pressureTrait.getHandler().getPressure());
                var air = new AtomicInteger(pressureTrait.getHandler().getAir());
                var maxPressure = pressureTrait.getHandler().maxPressure();

                var pressureSync = DataBindingBuilder.floatValS2C(() -> pressureTrait.getHandler().getPressure())
                        .remoteSetter(pressure::set)
                        .build();
                var airSync = DataBindingBuilder.intValS2C(() -> pressureTrait.getHandler().getAir())
                        .remoteSetter(air::set)
                        .build();
                bar.addSyncValue(pressureSync.getSyncValue());
                bar.addSyncValue(airSync.getSyncValue());

                bar.label.bindDataSource(SupplierDataSource.of(() ->
                        Component.literal(String.format("%.2f bar", pressure.get()))));

                bar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(
                            Component.literal(String.format("Pressure: %.2f / %.2f bar", pressure.get(), maxPressure)),
                            Component.literal(String.format("Air: %d mL", air.get()))),
                            null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

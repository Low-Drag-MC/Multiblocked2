package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.utils.EnergyFormattingUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability.ENERGY_BAR;
import static com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability.ENERGY_BG;

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
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var progress = new ProgressBar();
        progress.barContainer.getLayout().paddingAll(0);
        progress.barContainer.getStyle().background(ENERGY_BG);
        progress.bar.getStyle().background(ENERGY_BAR);
        progress.setProgress(1f);
        progress.label.setText("0/0 FE");
        progress.setId(uiId());
        progress.layout(layout -> layout.width(100).height(14));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof ForgeEnergyCapabilityTrait forgeEnergyTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(energyBar -> {
                energyBar.bind(DataBindingBuilder.floatValS2C(() -> {
                    var max = forgeEnergyTrait.getStorage().getMaxEnergyStored();
                    return max > 0 ? (float) forgeEnergyTrait.getStorage().getEnergyStored() / max : 0f;
                }).build());
                var stored = new AtomicInteger(forgeEnergyTrait.getStorage().getEnergyStored());
                var maxStored = new AtomicInteger(forgeEnergyTrait.getStorage().getMaxEnergyStored());

                var storageStoredValue = DataBindingBuilder.intValS2C(() -> forgeEnergyTrait.getStorage().getEnergyStored())
                        .remoteSetter(stored::set)
                        .build();
                var storageMaxStoredValue = DataBindingBuilder.intValS2C(() -> forgeEnergyTrait.getStorage().getMaxEnergyStored())
                        .remoteSetter(maxStored::set)
                        .build();

                energyBar.addSyncValue(storageStoredValue.getSyncValue());
                energyBar.addSyncValue(storageMaxStoredValue.getSyncValue());

                energyBar.label.bindDataSource(SupplierDataSource.of(() -> {
                    var storedVal = EnergyFormattingUtil.formatCompact(stored.get());
                    var maxStoredVal = EnergyFormattingUtil.formatCompact(maxStored.get());
                    return Component.literal(storedVal + "/" + maxStoredVal + " FE");
                }));
                energyBar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.literal(
                            EnergyFormattingUtil.formatCompact(stored.get()) + "/" + EnergyFormattingUtil.formatCompact(maxStored.get()) + " FE")),
                            null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The editor-facing half of {@link SourceStorageCapabilityTrait}: a Source buffer on the machine.
 */
public class SourceStorageCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<ISourceCap, @Nullable Direction> {
    @LDLRegister(name = "ars_source_storage", registry = "mbd2:trait_definition_type", group = "trait",
            priority = -100, modID = "ars_nouveau")
    public static final SimpleCapabilityTraitDefinition.Type<ISourceCap, @Nullable Direction, SourceStorageCapabilityTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("ars_source_storage", "trait") {
                @Override
                public SourceStorageCapabilityTraitDefinition createDefinition() {
                    return new SourceStorageCapabilityTraitDefinition();
                }

                @Override
                protected BlockCapability<ISourceCap, @Nullable Direction> getCapability() {
                    return CapabilityRegistry.SOURCE_CAPABILITY;
                }

                @Override
                protected ISourceCap merge(List<ISourceCap> contents) {
                    return new SourceCapList(contents.toArray(ISourceCap[]::new));
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_source_storage.capacity")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int capacity = 10000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_source_storage.max_receive", tips = "config.definition.trait.ars_source_storage.max_receive.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxReceive = 10000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_source_storage.max_extract", tips = "config.definition.trait.ars_source_storage.max_extract.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxExtract = 10000;
    /**
     * On by default: without it the machine is only reachable through an Arcane Relay, which is a
     * surprising amount of setup for something that looks like a Source Jar to the player.
     *
     * @see MachineSourceProvider
     */
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_source_storage.expose_to_devices",
            tips = {"config.definition.trait.ars_source_storage.expose_to_devices.tooltip.0",
                    "config.definition.trait.ars_source_storage.expose_to_devices.tooltip.1"})
    private boolean exposeToDevices = true;
    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.ars_source_storage.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Getter
    @Configurable(name = "config.definition.trait.ars_source_storage.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.ars_source_storage.fancy_renderer.tooltip")
    private final SourceFancyRendererSettings fancyRendererSettings = new SourceFancyRendererSettings(this);

    @Override
    public SourceStorageCapabilityTrait createTrait(MBDMachine machine) {
        return new SourceStorageCapabilityTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ARS_SOURCE;
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
        progress.barContainer.getStyle().background(MBDSprites.ENERGY_BG);
        progress.bar.getStyle().background(MBDSprites.ARS_SOURCE_BAR);
        progress.setProgress(1f);
        progress.label.setText("0/0");
        progress.setId(uiId());
        progress.layout(layout -> layout.height(14));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof SourceStorageCapabilityTrait sourceTrait) {
            ui.selectId(uiId(), ProgressBar.class).forEach(sourceBar -> {
                sourceBar.bind(DataBindingBuilder.floatValS2C(() -> {
                    var max = sourceTrait.getStorage().getSourceCapacity();
                    return max > 0 ? (float) sourceTrait.getStorage().getSource() / max : 0f;
                }).build());
                var stored = new AtomicInteger(sourceTrait.getStorage().getSource());
                var maxStored = new AtomicInteger(sourceTrait.getStorage().getSourceCapacity());

                var storedValue = DataBindingBuilder.intValS2C(() -> sourceTrait.getStorage().getSource())
                        .remoteSetter(stored::set)
                        .build();
                var maxStoredValue = DataBindingBuilder.intValS2C(() -> sourceTrait.getStorage().getSourceCapacity())
                        .remoteSetter(maxStored::set)
                        .build();

                sourceBar.addSyncValue(storedValue.getSyncValue());
                sourceBar.addSyncValue(maxStoredValue.getSyncValue());

                sourceBar.label.bindDataSource(SupplierDataSource.of(() ->
                        Component.translatable("recipe.capability.ars_source.stored", stored.get(), maxStored.get())));
                sourceBar.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(
                            Component.translatable("recipe.capability.ars_source.stored", stored.get(), maxStored.get())),
                            null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }
}

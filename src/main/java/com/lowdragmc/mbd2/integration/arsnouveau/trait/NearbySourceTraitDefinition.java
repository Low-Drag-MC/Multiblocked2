package com.lowdragmc.mbd2.integration.arsnouveau.trait;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The editor-facing half of {@link NearbySourceTrait}: recipes spend the Source in the jars around the
 * machine instead of a buffer it carries itself.
 */
public class NearbySourceTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    @LDLRegister(name = "ars_nearby_source", registry = "mbd2:trait_definition_type", group = "trait", modID = "ars_nouveau")
    public static final TraitDefinitionType<NearbySourceTraitDefinition> TYPE =
            new TraitDefinitionType<>("ars_nearby_source", "trait") {
                @Override
                public NearbySourceTraitDefinition createDefinition() {
                    return new NearbySourceTraitDefinition();
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_nearby_source.radius", tips = "config.definition.trait.ars_nearby_source.radius.tooltip")
    @ConfigNumber(range = {1, 64})
    private int radius = 10;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_nearby_source.scan_interval", tips = "config.definition.trait.ars_nearby_source.scan_interval.tooltip")
    @ConfigNumber(range = {1, 200})
    private int scanInterval = 20;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ars_nearby_source.particles", tips = "config.definition.trait.ars_nearby_source.particles.tooltip")
    private boolean particles = true;

    @Override
    public NearbySourceTrait createTrait(MBDMachine machine) {
        return new NearbySourceTrait(machine, this);
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
    public boolean allowMultiple() {
        return false;
    }

    /**
     * Not alongside a Source buffer on the same machine.
     * <p>
     * Both traits answer the {@code ars_source} recipe capability, and the recipe engine pools handlers
     * of one capability and walks them in turn — so a recipe's cost would be split across the two. That
     * is fine on its own, but with {@code expose_to_devices} on, the buffer is <em>also</em> one of the
     * providers this trait's scan counts, so a machine holding 60 source and standing next to no jars at
     * all would advertise 120 and start a recipe it cannot pay for, having already spent the 60.
     * <p>
     * A machine that wants both a buffer and a supply from the neighbourhood already has one: the
     * storage trait's auto-IO pulls from an adjacent Source Jar.
     */
    @Override
    public boolean isCompatibleWith(TraitDefinition other) {
        return !(other instanceof SourceStorageCapabilityTraitDefinition);
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var ui = new UIElement().setId(uiId());
        ui.layout(layout -> layout.gapAll(2).alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW));
        var icon = new UIElement()
                .layout(layout -> layout.heightPercent(100).aspectRatio(1))
                .style(style -> style.background(MBDSprites.ARS_SOURCE))
                .addClass("source-icon");
        var label = new Label();
        label.setText("0 source").addClass("source-label");
        label.layout(layout -> layout.height(10));
        ui.addChildren(icon, label);
        container.addChild(ui);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof NearbySourceTrait sourceTrait) {
            ui.select("#%s > label".formatted(uiId()), Label.class).forEach(label -> {
                var available = new AtomicInteger(0);
                // the trait's cached count rather than a fresh scan: this supplier runs on the server
                // every sync tick, and a scan is a walk over every block entity in range
                var sync = DataBindingBuilder.intValS2C(sourceTrait::getAvailableSource)
                        .remoteSetter(available::set).build();
                label.addSyncValue(sync.getSyncValue());
                label.bindDataSource(SupplierDataSource.of(() ->
                        Component.translatable("recipe.capability.ars_source.nearby", available.get())));
            });
        }
    }
}

package com.lowdragmc.mbd2.integration.astralsorcery;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/** The trait-UI row every Astral Sorcery trait uses: an icon and a label rendered from a server-synced snapshot. */
public final class AstralTraitUI {
    private AstralTraitUI() {}

    public static void createTemplate(UIElement container, String uiId, IGuiTexture icon, String labelClass) {
        var row = new UIElement().setId(uiId);
        row.layout(layout -> layout.gapAll(2).alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW));
        var iconElement = new UIElement()
                .layout(layout -> layout.heightPercent(100).aspectRatio(1))
                .style(style -> style.background(icon));
        var label = new Label();
        label.setText("---").addClass(labelClass);
        label.layout(layout -> layout.height(10));
        row.addChildren(iconElement, label);
        container.addChild(row);
    }

    /**
     * Not selected as {@code "#" + uiId + " > label"}: {@code uiId} is {@code "ui:<name>"} and the colon parses as
     * a pseudo-class, so that selector silently matches nothing.
     */
    public static void bindLabel(UI ui, String uiId, String labelClass,
                                 Supplier<CompoundTag> snapshot,
                                 Function<CompoundTag, Component> summary,
                                 @Nullable Function<CompoundTag, List<Component>> tooltip) {
        ui.selectId(uiId).flatMap(row -> row.select("." + labelClass, Label.class)).forEach(label -> {
            var synced = new AtomicReference<>(snapshot.get());
            var binding = DataBindingBuilder.tagS2C(snapshot::get)
                    .remoteSetter(tag -> synced.set(tag instanceof CompoundTag compound ? compound : new CompoundTag()))
                    .build();
            label.addSyncValue(binding.getSyncValue());
            label.bindDataSource(SupplierDataSource.of(() -> summary.apply(synced.get())));
            if (tooltip != null) {
                label.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(tooltip.apply(synced.get()), null, null, null);
                    event.stopPropagation();
                });
            }
        });
    }
}

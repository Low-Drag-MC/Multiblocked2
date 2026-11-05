package com.lowdragmc.mbd2.integration.astralsorcery;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/** Trait and recipe widgets drawn with Astral Sorcery's own textures: tinted 8px icons and its lumen bar. */
public final class AstralTraitUI {
    private static final ResourceLocation BAR = ResourceLocation.fromNamespaceAndPath(AstralSorceryIntegration.MOD_ID,
            "textures/screen/lumen_bar_large.png");
    /** The texture's top half is the empty trough, the bottom half the greyscale fill; both have 4px end caps. */
    private static final SpriteTexture BAR_TROUGH = SpriteTexture.of(BAR).setSprite(0, 0, 64, 5).setBorder(4, 0, 4, 0)
            .setColor(0xFF666666);
    private static final SpriteTexture BAR_FILL = SpriteTexture.of(BAR).setSprite(0, 5, 64, 5).setBorder(4, 0, 4, 0);
    private static final int BAR_WIDTH = 64;
    private static final int EMPTY_COLOR = 0xFF888888;

    private AstralTraitUI() {}

    public static IGuiTexture lumenIcon(Lumen lumen) {
        return lumenIcon(lumen, AstralSorceryIntegration.lumenColor(lumen));
    }

    public static IGuiTexture lumenIcon(Lumen lumen, int color) {
        var id = AstralSorceryIntegration.lumenId(lumen);
        return SpriteTexture.of(id.withPath(path -> "textures/lumen/" + path + ".png")).setColor(color);
    }

    public static IGuiTexture starIcon(int color) {
        return SpriteTexture.of(AstralSorceryIntegration.MOD_ID + ":textures/environment/star_1.png").setColor(color);
    }

    /** Recipe viewers draw on a light panel, where pale colours such as Armara's grey all but vanish: darken those. */
    public static int onLightBackground(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (0.299 * r + 0.587 * g + 0.114 * b < 150) return argb;
        return 0xFF000000 | ((int) (r * 0.5) << 16) | ((int) (g * 0.5) << 8) | (int) (b * 0.5);
    }

    // ----- bars -----

    /** One row; {@code key} identifies it across updates, {@code fill} is 0 to 1. */
    public record Bar(String key, IGuiTexture icon, int color, float fill, Component text, Component tooltip) {}

    public static Bar emptyBar(IGuiTexture icon, String text, Component tooltip) {
        return new Bar("", icon, EMPTY_COLOR, 0, Component.literal(text).withColor(EMPTY_COLOR), tooltip);
    }

    public static void createBarsTemplate(UIElement container, String uiId) {
        container.addChild(new UIElement().setId(uiId)
                .layout(layout -> layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2)));
    }

    /**
     * Rows are built on the client from the synced snapshot, never in the template: the template is saved with the
     * machine UI, and which rows exist changes at runtime.
     */
    public static void bindBars(UI ui, String uiId, Supplier<CompoundTag> snapshot, Function<CompoundTag, List<Bar>> bars) {
        ui.selectId(uiId).forEach(list -> {
            var rows = new LinkedHashMap<String, BarRow>();
            var binding = DataBindingBuilder.tagS2C(snapshot::get)
                    .remoteSetter(tag -> update(list, rows, bars.apply(tag instanceof CompoundTag compound ? compound : new CompoundTag())))
                    .build();
            list.addSyncValue(binding.getSyncValue());
        });
    }

    private record BarRow(UIElement row, UIElement fill, Label label) {}

    private static void update(UIElement list, Map<String, BarRow> rows, List<Bar> bars) {
        var keys = bars.stream().map(Bar::key).toList();
        if (!keys.equals(new ArrayList<>(rows.keySet()))) {
            list.clearAllChildren();
            rows.clear();
            for (var bar : bars) {
                var row = createRow(bar);
                rows.put(bar.key(), row);
                list.addChild(row.row());
            }
        }
        for (var bar : bars) {
            var row = rows.get(bar.key());
            var fill = Math.max(0, Math.min(1, bar.fill()));
            row.fill().layout(layout -> layout.widthPercent(fill * 100));
            row.label().setText(bar.text());
            row.row().style(style -> style.tooltips(bar.tooltip()));
        }
    }

    private static BarRow createRow(Bar bar) {
        var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(2).height(9));
        var icon = new UIElement().layout(layout -> layout.width(8).height(8)).style(style -> style.background(bar.icon()));
        var trough = new UIElement().layout(layout -> layout.width(BAR_WIDTH).height(5))
                .style(style -> style.background(BAR_TROUGH));
        var fill = new UIElement().layout(layout -> layout.heightPercent(100).widthPercent(0))
                .style(style -> style.background(BAR_FILL.copy().setColor(bar.color())));
        trough.addChild(fill);
        var label = new Label();
        label.addClass("astral-bar-label");
        label.textStyle(style -> style.adaptiveWidth(true));
        label.layout(layout -> layout.height(9).marginLeft(2));
        row.addChildren(icon, trough, label);
        return new BarRow(row, fill, label);
    }

    // ----- a single label -----

    public static void createLabelTemplate(UIElement container, String uiId, IGuiTexture icon, String labelClass) {
        var row = new UIElement().setId(uiId);
        row.layout(layout -> layout.gapAll(2).alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW));
        var iconElement = new UIElement()
                .layout(layout -> layout.width(8).height(8))
                .style(style -> style.background(icon));
        var label = new Label();
        label.setText("---").addClass(labelClass);
        label.layout(layout -> layout.height(9));
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

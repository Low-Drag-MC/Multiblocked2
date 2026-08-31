package com.lowdragmc.mbd2.common.gui.builtin;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The UIs MBD2 ships, built in Java rather than authored as files.
 *
 * <h2>Why Java and not xml</h2>
 * A machine UI is assembled on both sides, and a ui xml resolves through whichever resource manager
 * is active — assets on the client, datapacks on the server. One file therefore has to be shipped
 * twice and kept in step, and a server that cannot find its copy builds a different element tree from
 * the client's: ids that do not line up, sync values with nowhere to land, clicks reaching a listener
 * that was never registered, and nothing thrown anywhere. Code has no sides.
 *
 * <h2>Where the look lives</h2>
 * In {@link #AUTO_IO_STYLESHEET}, not here. Every element carries a class, and the stylesheet says
 * what that class looks like; a pack can restyle the whole panel by shipping its own
 * {@code assets/<any-namespace>/lss/mbd2_auto_io.lss}, because stylesheets merge by path.
 *
 * <p>What is written here is authored inline and then {@linkplain #demote demoted} to
 * {@link StyleOrigin#DEFAULT}, the bottom of the cascade — so it is the fallback that keeps the panel
 * usable with no stylesheet at all, and <em>any</em> rule in a sheet beats it. Writing it straight at
 * {@code DEFAULT} does not work: only inline values are serialised into a {@link UITemplate}.</p>
 *
 * <h2>What the editor sees</h2>
 * The same templates are registered as read-only entries under a {@code mbd2} provider in
 * {@code UIResource}, so they can be opened and read in the editor and copied out to a file provider
 * when someone wants their own editable version. See {@link #register}.
 */
public final class BuiltinMachineUIs {

    /** The strip that side tabs hang off. One per machine UI, shared by every tab. */
    public static final String SIDE_TAB_STRIP = "side_tab_strip";
    /** One auto-IO tab: a handle, and a panel that folds out beside it. */
    public static final String AUTO_IO_TAB = "auto_io_tab";

    /** The id the strip document and every blueprint that appends to it agree on. */
    public static final String STRIP_ID = "mbd2_side_tabs";

    /**
     * The stylesheet the auto-IO panel is drawn with, as a merged path — every {@code lss/mbd2_auto_io.lss}
     * in every loaded pack contributes, so a pack overrides MBD2's look by shipping its own.
     */
    public static final ResourceLocation AUTO_IO_STYLESHEET = MBD2.id("lss/mbd2_auto_io");

    // ---- the class names the stylesheet and the blueprint both use ------------------------------

    /** On the strip itself. */
    public static final String STRIP_CLASS = "mbd2-side-tabs";
    /** On one tab's root, wrapping its handle and panel. */
    public static final String TAB_CLASS = "mbd2-auto-io-tab";
    /** On the always-visible handle that folds the panel out. */
    public static final String HANDLE_CLASS = "mbd2-auto-io-handle";
    /** On the panel that folds out. */
    public static final String PANEL_CLASS = "mbd2-auto-io-panel";
    /** On one of the six face buttons. */
    public static final String FACE_CLASS = "mbd2-auto-io-face";
    /** On the layer inside a face that the neighbouring block is drawn on. */
    public static final String FACE_ITEM_CLASS = "mbd2-auto-io-face-item";

    /**
     * The class a face carries for what its auto IO is set to — {@code mbd2-io-in} and friends.
     *
     * <p>The blueprint sets this rather than a colour, which is the whole point of doing it with
     * classes: what "in" looks like is then a line in a stylesheet a pack can replace, not a constant
     * compiled into a graph.</p>
     */
    public static String ioClass(IO io) {
        return "mbd2-io-" + io.name().toLowerCase(Locale.ROOT);
    }

    private static final Map<String, Supplier<UIElement>> BUILDERS = new LinkedHashMap<>();

    static {
        BUILDERS.put(SIDE_TAB_STRIP, BuiltinMachineUIs::sideTabStrip);
        BUILDERS.put(AUTO_IO_TAB, BuiltinMachineUIs::autoIOTab);
    }

    private BuiltinMachineUIs() {}

    /** Every built-in UI's name, in the order they are declared. */
    public static Iterable<String> names() {
        return BUILDERS.keySet();
    }

    /**
     * A fresh element tree for one built-in UI, or null when there is no such name.
     *
     * <p>Fresh every call, never a shared instance: an element has identity and belongs to one tree,
     * so two machines showing the same built-in must not be handed the same objects.</p>
     *
     * <p>Its styles are {@link #demote}d, so the stylesheet is in charge of what it looks like and
     * what is written here is only the fallback.</p>
     */
    @Nullable
    public static UIElement create(String name) {
        var builder = BUILDERS.get(name);
        if (builder == null) return null;
        var root = builder.get();
        demote(root);
        return root;
    }

    /**
     * The same trees, as templates, for the editor's resource browser.
     *
     * <p>Built separately from {@link #create} and <em>not</em> demoted, because a template is NBT
     * and only inline values are written to it — a demoted tree serialises as a shape with no
     * styling, which is not what someone opening the built-in wants to see.</p>
     */
    public static void register(BuiltinResourceProvider<UITemplate> provider) {
        for (var entry : BUILDERS.entrySet()) {
            try {
                provider.addResource(entry.getKey(), UITemplate.of(entry.getValue().get()));
            } catch (Exception e) {
                MBD2.LOGGER.error("Failed to build built-in ui '{}'", entry.getKey(), e);
            }
        }
    }

    // ---- the documents ---------------------------------------------------------------------------

    /**
     * A column hanging off the machine panel's edge, for tabs to be appended to.
     *
     * <p>Absolutely positioned so that adding tabs never moves the machine's own contents, and so the
     * strip is outside the panel rather than covering it.</p>
     */
    private static UIElement sideTabStrip() {
        var root = new UIElement();
        root.setId(STRIP_ID);
        root.addClasses(STRIP_CLASS);
        style(root, "position", "absolute");
        style(root, "left", "100%");
        style(root, "top", "4");
        style(root, "flex-direction", "column");
        style(root, "gap", "2");
        return root;
    }

    /**
     * One tab: a handle that stays visible, and a panel that folds out beside it.
     *
     * <p>The panel sits <em>in the row</em> next to its handle rather than floating over it, and is
     * folded away with {@code display} rather than {@code visible}. That is what stops two open tabs
     * from landing on top of each other: a hidden panel is out of the layout entirely, and an open one
     * makes its tab as tall as itself, so the strip's column pushes the tabs below it down.</p>
     *
     * <p>The six faces are a three-by-three grid laid out the way a machine sees itself —
     * {@code UP} above {@code FRONT}, {@code LEFT} and {@code RIGHT} beside it, {@code DOWN} and
     * {@code BACK} below. The empty cells are what make it read as a machine rather than as a list.
     * Each carries a tooltip naming its face, because a coloured square on its own says which way
     * things move but not which side moves them.</p>
     */
    private static UIElement autoIOTab() {
        var tab = new UIElement();
        tab.setId("tab");
        tab.addClasses(TAB_CLASS);
        style(tab, "flex-direction", "row");
        style(tab, "align-items", "flex-start");
        style(tab, "gap", "2");

        var handle = new Button();
        handle.noText();
        handle.setId("handle");
        handle.addClasses(HANDLE_CLASS);
        style(handle, "width", "20");
        style(handle, "height", "20");
        // Scaled rather than drawn at the button's size: an overlay fills its element, so a full-size
        // gear covers the button's own frame and the tab stops reading as a button at all.
        style(handle, "overlay", "icon(settings) scale(0.55)");
        handle.style(style -> style.tooltips("mbd2.gui.auto_io.tab"));
        tab.addChild(handle);

        var panel = new UIElement();
        panel.setId("panel");
        panel.addClasses(PANEL_CLASS);
        panel.setDisplay(false);
        style(panel, "flex-direction", "column");
        style(panel, "gap", "3");
        style(panel, "padding-all", "5");
        background(panel, Sprites.BORDER);
        tab.addChild(panel);

        var title = new Label();
        title.setId("title");
        title.addClasses("mbd2-auto-io-title");
        panel.addChild(title);

        var body = new UIElement();
        body.addClasses("mbd2-auto-io-body");
        style(body, "flex-direction", "row");
        style(body, "align-items", "flex-start");
        style(body, "gap", "5");
        panel.addChild(body);

        body.addChild(legend());
        body.addChild(faceGrid());
        return tab;
    }

    /** What the colours mean, in the order a face cycles through them. */
    private static UIElement legend() {
        var legend = new UIElement();
        legend.addClasses("mbd2-auto-io-legend");
        style(legend, "flex-direction", "column");
        style(legend, "gap", "3");
        style(legend, "padding-all", "3");
        background(legend, Sprites.RECT_RD_DARK);
        for (var io : new IO[]{IO.IN, IO.OUT, IO.BOTH}) {
            var swatch = new UIElement();
            // Both the shared swatch class and the state class the faces use, so a pack recolouring
            // "in" moves the key and the faces together rather than letting them drift apart.
            swatch.addClasses("mbd2-auto-io-swatch", ioClass(io));
            style(swatch, "width", "14");
            style(swatch, "height", "14");
            style(swatch, "background", legendColour(io));
            swatch.style(style -> style.tooltips("mbd2.gui.auto_io." + io.name().toLowerCase(Locale.ROOT)));
            legend.addChild(swatch);
        }
        return legend;
    }

    /** The face cross. Grid rather than rows: the gaps are what make it a machine. */
    private static UIElement faceGrid() {
        var grid = new UIElement();
        grid.addClasses("mbd2-auto-io-grid");
        style(grid, "display", "grid");
        style(grid, "gap", "2");
        style(grid, "padding-all", "3");
        style(grid, "grid-template-columns", FACE_SIZE + " " + FACE_SIZE + " " + FACE_SIZE);
        style(grid, "grid-template-rows", FACE_SIZE + " " + FACE_SIZE + " " + FACE_SIZE);
        background(grid, Sprites.RECT_RD_DARK);

        grid.addChild(face(RelativeDirection.UP, 2, 1));
        grid.addChild(face(RelativeDirection.LEFT, 1, 2));
        grid.addChild(face(RelativeDirection.FRONT, 2, 2));
        grid.addChild(face(RelativeDirection.RIGHT, 3, 2));
        grid.addChild(face(RelativeDirection.DOWN, 2, 3));
        grid.addChild(face(RelativeDirection.BACK, 3, 3));
        return grid;
    }

    /** How wide and tall one face is. Square, and the same number the grid template uses. */
    private static final String FACE_SIZE = "20";

    private static Button face(RelativeDirection relative, int column, int row) {
        var button = new Button();
        button.noText();
        button.setId("face_" + relative.name());
        // The state class is added by the blueprint; this one is the face's identity, and starts out
        // as "nothing set" so the panel reads correctly before the first tick has painted it.
        button.addClasses(FACE_CLASS, ioClass(IO.NONE));
        style(button, "grid-column", String.valueOf(column));
        style(button, "grid-row", String.valueOf(row));
        // Sized rather than left to stretch: a grid item takes the row's height, and a row sized only
        // by the template collapses to its content — which for an empty button is a few pixels, so the
        // faces come out as letterboxes rather than as the squares a block face should be.
        style(button, "width", FACE_SIZE);
        style(button, "height", FACE_SIZE);
        button.style(style -> style.tooltips(
                Component.translatable("mbd2.gui.auto_io.face",
                        Component.translatable("mbd2.gui.auto_io.side." + relative.name().toLowerCase()))));
        button.addChild(faceItem(relative));
        return button;
    }

    /**
     * Where the block on the other side of a face is drawn.
     *
     * <p>A child rather than the button's own texture because of the order an element paints in: its
     * background goes down <em>before</em> a Button's frame, so an item there is covered, and its
     * overlay goes down after everything, which is where the state colour has to be. Children are
     * drawn between the two — under the tint, over the frame — which is exactly the stack this wants.
     *
     * <p>{@code allowHitTest(false)} is what keeps the button clickable. A child normally becomes the
     * target of a click that lands on it, and the listener on the parent then never runs; opting out
     * of hit testing leaves the parent as the only thing under the cursor.</p>
     */
    private static UIElement faceItem(RelativeDirection relative) {
        var item = new UIElement();
        item.setId("item_" + relative.name());
        item.addClasses(FACE_ITEM_CLASS);
        item.setAllowHitTest(false);
        style(item, "width", "100%");
        style(item, "height", "100%");
        return item;
    }

    /** The fallback colour for one state's key swatch; the stylesheet is what a pack edits. */
    private static String legendColour(IO io) {
        return switch (io) {
            case IN -> "#FF4C9BE8";
            case OUT -> "#FFE8944C";
            case BOTH -> "#FF5FCB84";
            default -> "#FF404040";
        };
    }

    /**
     * Written inline, and demoted to {@link StyleOrigin#DEFAULT} by {@link #demote} when a tree is
     * handed out.
     *
     * <p>It has to be written inline first because {@code Style.serializeNBT} only writes a
     * property's <em>inline</em> value — a style set straight at {@code DEFAULT} is dropped on the
     * floor when the document is turned into a {@link UITemplate}, so the editor would open these
     * built-ins with no styling at all. Demoting afterwards is LDLib2's own idiom for this
     * ({@code Configurator}, {@code ConfiguratorGroup} and the asset browser all do it): author
     * normally, then step out of the way of the cascade.</p>
     */
    private static void style(UIElement element, String property, String value) {
        element.lss(property, value, StyleOrigin.INLINE);
    }

    /** The same, for a texture, which has no lss spelling that survives a round trip. */
    private static void background(UIElement element, com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture texture) {
        element.style(style -> style.backgroundTexture(texture));
    }

    /**
     * Moves every inline value in a tree down to {@link StyleOrigin#DEFAULT}.
     *
     * <p>Inline is the top of the cascade below animation: a stylesheet cannot beat it, so a panel
     * authored inline is a panel nobody can restyle. At {@code DEFAULT} the same values are a
     * fallback — what the built-in looks like with no sheet loaded — and any rule wins over them.</p>
     *
     * <p>Recursive because {@code UIElement.moveInlineAsDefault} is not: it moves one element's own
     * bag, and a document is a tree.</p>
     */
    private static void demote(UIElement element) {
        element.moveInlineAsDefault();
        element.getChildren().forEach(BuiltinMachineUIs::demote);
    }
}

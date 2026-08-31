package com.lowdragmc.mbd2.common.gui.builtin;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
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
 * <h2>What the editor sees</h2>
 * The same templates are registered as read-only entries under a {@code mbd2} provider in
 * {@code UIResource}, so they can be opened and read in the editor and copied out to a file provider
 * when someone wants their own editable version. See {@link #register}.
 *
 * <h2>Style</h2>
 * Built out of LDLib2's own nine-slice sprites — the same panels, buttons and tabs the editor uses —
 * so a machine carrying one of these looks like the rest of the game rather than like a web page.
 */
public final class BuiltinMachineUIs {

    /** The strip that side tabs hang off. One per machine UI, shared by every tab. */
    public static final String SIDE_TAB_STRIP = "side_tab_strip";
    /** One auto-IO tab: a handle, and a panel that folds out beside it. */
    public static final String AUTO_IO_TAB = "auto_io_tab";

    /** The id the strip document and every blueprint that appends to it agree on. */
    public static final String STRIP_ID = "mbd2_side_tabs";

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
     */
    @Nullable
    public static UIElement create(String name) {
        var builder = BUILDERS.get(name);
        return builder == null ? null : builder.get();
    }

    /** The same trees, as templates, for the editor's resource browser. */
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
     * <p>The panel is absolutely positioned against the handle rather than sitting next to it in the
     * row, so an open panel does not push the tabs below it down the strip.</p>
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
        style(tab, "flex-direction", "row");
        style(tab, "align-items", "flex-start");

        var handle = new Button();
        handle.noText();
        handle.setId("handle");
        style(handle, "width", "20");
        style(handle, "height", "20");
        // Scaled rather than drawn at the button's size: an overlay fills its element, so a full-size
        // gear covers the button's own frame and the tab stops reading as a button at all.
        style(handle, "overlay", "icon(settings) scale(0.55)");
        handle.style(style -> style.tooltips("mbd2.gui.auto_io.tab"));
        tab.addChild(handle);

        var panel = new UIElement();
        panel.setId("panel");
        panel.setVisible(false);
        style(panel, "position", "absolute");
        style(panel, "left", "100%");
        style(panel, "top", "0");
        style(panel, "flex-direction", "column");
        style(panel, "gap", "3");
        style(panel, "padding-all", "5");
        panel.style(style -> style.backgroundTexture(Sprites.BORDER));
        tab.addChild(panel);

        var title = new Label();
        title.setId("title");
        panel.addChild(title);

        var body = new UIElement();
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
        style(legend, "flex-direction", "column");
        style(legend, "gap", "3");
        style(legend, "padding-all", "3");
        legend.style(style -> style.backgroundTexture(Sprites.RECT_RD_DARK));
        for (var entry : new String[][]{{"in", "#FF4C9BE8"}, {"out", "#FFE8944C"}, {"both", "#FF5FCB84"}}) {
            var swatch = new UIElement();
            style(swatch, "width", "14");
            style(swatch, "height", "14");
            style(swatch, "background", entry[1]);
            swatch.style(style -> style.tooltips("mbd2.gui.auto_io." + entry[0]));
            legend.addChild(swatch);
        }
        return legend;
    }

    /** The face cross. Grid rather than rows: the gaps are what make it a machine. */
    private static UIElement faceGrid() {
        var grid = new UIElement();
        style(grid, "display", "grid");
        style(grid, "gap", "2");
        style(grid, "padding-all", "3");
        style(grid, "grid-template-columns", FACE_SIZE + " " + FACE_SIZE + " " + FACE_SIZE);
        style(grid, "grid-template-rows", FACE_SIZE + " " + FACE_SIZE + " " + FACE_SIZE);
        grid.style(style -> style.backgroundTexture(Sprites.RECT_RD_DARK));

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
        item.setAllowHitTest(false);
        style(item, "width", "100%");
        style(item, "height", "100%");
        return item;
    }

    /**
     * Inline rather than a stylesheet: a built-in is grafted into a machine's own UI by root element,
     * and a document-level stylesheet would be left behind with the document.
     */
    private static void style(UIElement element, String property, String value) {
        element.lss(property, value, StyleOrigin.INLINE);
    }
}

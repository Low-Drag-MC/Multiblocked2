package com.lowdragmc.mbd2.common.gui.builtin;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The UIs MBD2 ships, as read-only entries in the editor's UI resource library.
 *
 * <h2>Structure here, look in the stylesheet</h2>
 * These documents are ids, classes and nesting — and nothing else. Every size, colour, texture and
 * position lives in {@link #AUTO_IO_STYLESHEET}, which the template carries a reference to, so the
 * editor's preview of a built-in looks the same as the real thing and a pack restyles the panel by
 * shipping its own {@code assets/<any-namespace>/lss/mbd2_auto_io.lss} — stylesheets merge by path.
 *
 * <p>This is not only tidiness. A {@link UITemplate} is a {@code CompoundTag}: only a property's
 * <em>inline</em> value is serialised, and inline is the top of the cascade below animation. Styling
 * these documents in code would therefore produce values a stylesheet could not override — or, if
 * written lower in the cascade to avoid that, values that are silently dropped on the way into the
 * template. Owning no styles at all sidesteps both.</p>
 *
 * <h2>Why they are code rather than files</h2>
 * A ui xml resolves through the active resource manager — assets on the client, datapacks on the
 * server — so one file would have to ship twice and stay in step, and a machine UI is assembled on
 * both sides. Built here, registered once, loaded by path: both sides read the same entry.
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

    /** The provider these are registered under; also the namespace in {@link #path}. */
    public static final String PROVIDER_NAME = MBD2.MOD_ID;

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

    /** Which of the six sides a face is, so the stylesheet can place it on the cross. */
    public static String sideClass(RelativeDirection relative) {
        return "mbd2-side-" + relative.name().toLowerCase(Locale.ROOT);
    }

    /** The resource path one of these is loaded by, e.g. {@code built-in(mbd2:auto_io_tab)}. */
    public static String path(String name) {
        return "built-in(%s:%s)".formatted(PROVIDER_NAME, name);
    }

    private static final Map<String, Supplier<UIElement>> BUILDERS = new LinkedHashMap<>();

    static {
        BUILDERS.put(SIDE_TAB_STRIP, BuiltinMachineUIs::sideTabStrip);
        BUILDERS.put(AUTO_IO_TAB, BuiltinMachineUIs::autoIOTab);
    }

    private BuiltinMachineUIs() {}

    /**
     * Put these in the editor's UI resource library, where {@code ldlib2_ui_template_load} finds them.
     *
     * <p>Registered on both sides, because a machine UI is built on both sides and the graph loads
     * the same path on each. Nothing here is client-only — a {@code Resource} is a plain library.</p>
     */
    public static void register() {
        var instance = UIResource.INSTANCE.getResourceInstance();
        var provider = new BuiltinResourceProvider<>(PROVIDER_NAME, instance);
        for (var entry : BUILDERS.entrySet()) {
            try {
                provider.addResource(entry.getKey(), UITemplate.of(entry.getValue().get(), AUTO_IO_STYLESHEET));
            } catch (Exception e) {
                MBD2.LOGGER.error("Failed to build built-in ui '{}'", entry.getKey(), e);
            }
        }
        instance.addBuiltinProvider(provider);
    }

    // ---- the documents ---------------------------------------------------------------------------

    /** A column for tabs to be appended to; the stylesheet hangs it off the machine panel's edge. */
    private static UIElement sideTabStrip() {
        var root = new UIElement();
        root.setId(STRIP_ID);
        root.addClasses(STRIP_CLASS);
        return root;
    }

    /**
     * One tab: a handle that stays visible, and a panel that folds out beside it.
     *
     * <p>The panel sits <em>in the row</em> next to its handle rather than floating over it, and is
     * folded away with {@code display} rather than {@code visible} (the stylesheet starts it at
     * {@code display: none}). That is what stops two open tabs from landing on top of each other: a
     * hidden panel is out of the layout entirely, and an open one makes its tab as tall as itself, so
     * the strip's column pushes the tabs below it down.</p>
     *
     * <p>The six faces are a three-by-three grid laid out the way a machine sees itself — {@code UP}
     * above {@code FRONT}, {@code LEFT} and {@code RIGHT} beside it, {@code DOWN} and {@code BACK}
     * below. Which cell each lands in is a stylesheet rule keyed by its side class, so the cross is
     * something a pack can rearrange. Each carries a tooltip naming its face, because a coloured
     * square on its own says which way things move but not which side moves them.</p>
     */
    private static UIElement autoIOTab() {
        var tab = new UIElement();
        tab.setId("tab");
        tab.addClasses(TAB_CLASS);

        var handle = new Button();
        handle.noText();
        handle.setId("handle");
        handle.addClasses(HANDLE_CLASS);
        handle.style(style -> style.tooltips("mbd2.gui.auto_io.tab"));
        tab.addChild(handle);

        var panel = new UIElement();
        panel.setId("panel");
        panel.addClasses(PANEL_CLASS, "panel_bg");
        tab.addChild(panel);

        var title = new Label();
        title.setId("title");
        title.addClasses("mbd2-auto-io-title");
        panel.addChild(title);

        var body = new UIElement();
        body.addClasses("mbd2-auto-io-body");
        panel.addChild(body);

        body.addChild(legend());
        body.addChild(faceGrid());
        return tab;
    }

    /** What the colours mean, in the order a face cycles through them. */
    private static UIElement legend() {
        var legend = new UIElement();
        legend.addClasses("mbd2-auto-io-legend", "preview_bg");
        legend.layout(layout -> layout.justifyContent(AlignContent.SPACE_BETWEEN));
        for (var io : new IO[]{IO.IN, IO.OUT, IO.BOTH}) {
            var swatch = new UIElement();
            // Both the shared swatch class and the state class the faces use, so a pack recolouring
            // "in" moves the key and the faces together rather than letting them drift apart.
            swatch.addClasses("mbd2-auto-io-swatch", ioClass(io));
            swatch.style(style -> style.tooltips("mbd2.gui.auto_io." + io.name().toLowerCase(Locale.ROOT)));
            legend.addChild(swatch);
        }
        legend.moveInlineAsDefault();
        return legend;
    }

    /** The face cross. Grid rather than rows: the gaps are what make it a machine. */
    private static UIElement faceGrid() {
        var grid = new UIElement();
        grid.addClasses("mbd2-auto-io-grid", "preview_bg");
        for (var relative : new RelativeDirection[]{RelativeDirection.UP, RelativeDirection.LEFT,
                RelativeDirection.FRONT, RelativeDirection.RIGHT, RelativeDirection.DOWN,
                RelativeDirection.BACK}) {
            grid.addChild(face(relative));
        }
        return grid;
    }

    private static Button face(RelativeDirection relative) {
        var button = new Button();
        button.noText();
        button.setId("face_" + relative.name());
        // The state class is added by the blueprint; these two are the face's identity, and it starts
        // as "nothing set" so the panel reads correctly before the first tick has painted it.
        button.addClasses(FACE_CLASS, sideClass(relative), ioClass(IO.NONE));
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
     * <p>It fills the face, so it — not the face — is what a click lands on. That is fine and needs
     * nothing done to it: the face listens in the <em>capture</em> phase, which reaches an ancestor
     * on the way down to the target, before any descendant sees the event.</p>
     */
    private static UIElement faceItem(RelativeDirection relative) {
        var item = new UIElement();
        item.setId("item_" + relative.name());
        item.addClasses(FACE_ITEM_CLASS);
        return item;
    }
}

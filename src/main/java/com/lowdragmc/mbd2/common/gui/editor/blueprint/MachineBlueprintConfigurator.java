package com.lowdragmc.mbd2.common.gui.editor.blueprint;

import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResourceProviderContainer;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The blueprint picker row: drop a blueprint from the Resources panel onto it, or click it to choose
 * one from a dialog.
 *
 * <p>The same interaction as {@code IRendererConfigurator} and {@code IGuiTextureConfigurator}, and for
 * the same reason: the value being picked lives in the Resources panel a few pixels away, so dragging
 * it in is the direct gesture. A dropdown of paths asks the author to recognise their blueprint by a
 * string when they are looking straight at its icon.</p>
 *
 * <h2>Why the value is a path string</h2>
 * {@code ValueConfigurator<T>} normally carries the resource value itself — a renderer, a texture — and
 * the binding stores that. A blueprint cannot work that way: the graph is deserialized per machine at
 * runtime, and storing the value would freeze a copy, breaking the "edit the blueprint once, every
 * machine using it follows" property the whole resource design exists for. So the configurator carries
 * the path, exactly as the binding persists it.
 *
 * <p>That is also why the drop payload is convenient rather than awkward: a graph resource drags a
 * {@link GraphResourceProviderContainer.DraggingGraph}, which carries the {@link IResourcePath} instead
 * of the NBT, so the drop needs no value-to-path guesswork.</p>
 */
public class MachineBlueprintConfigurator extends ValueConfigurator<String> {

    public MachineBlueprintConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate) {
        super(name, supplier, onUpdate, MachineBlueprintBinding.NO_BLUEPRINT, true);
        setTips("editor.drag_drop_resource", "config.machine_blueprint.blueprint.tooltip");
        if (value == null) {
            value = MachineBlueprintBinding.NO_BLUEPRINT;
        }

        var preview = new UIElement();
        preview.layout(layout -> {
                    layout.setPipelineState(StyleOrigin.DEFAULT);
                    layout.height(14);
                    layout.paddingAll(2);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(2);
                    layout.setPipelineState(StyleOrigin.INLINE);
                }).style(style -> {
                    style.setPipelineState(StyleOrigin.DEFAULT);
                    style.backgroundTexture(Sprites.RECT_RD_SOLID);
                    style.setPipelineState(StyleOrigin.IMPORTANT);
                    style.overlayTexture(DynamicTexture.of(() -> preview.isSelfOrChildHover()
                            ? Sprites.RECT_RD_T_SOLID : IGuiTexture.EMPTY));
                    style.setPipelineState(StyleOrigin.INLINE);
                }).addClass("configurator_preview_bg")
                .addChild(new Label().bindDataSource(SupplierDataSource.of(this::describe))
                        .textStyle(textStyle -> textStyle
                                .textAlignVertical(Vertical.CENTER)
                                .textWrap(TextWrap.HOVER_ROLL))
                        .layout(layout -> {
                            layout.heightPercent(100);
                            layout.flex(1);
                        }).setOverflowVisible(false))
                .addChild(new UIElement().layout(layout -> {
                    layout.heightPercent(100);
                    layout.setAspectRatio(1);
                }).style(style -> style.backgroundTexture(MachineBlueprintResource.INSTANCE.getIcon())));
        preview.addEventListener(UIEvents.MOUSE_DOWN, this::showBlueprintDialog);
        inlineContainer.addChildren(preview);

        // Only a blueprint from this resource type. Checking the resource identity is what rejects a
        // graph dragged out of some other graph library, which would otherwise arrive as an equally
        // valid-looking DraggingGraph and be stored as a path that never resolves.
        setCanDropPredicate(object -> object instanceof GraphResourceProviderContainer.DraggingGraph dragging
                && dragging.graphResource() == MachineBlueprintResource.INSTANCE);
    }

    /** What the row reads: the blueprint's name, or why there isn't one. */
    private Component describe() {
        var path = currentPath();
        if (path == null) {
            return Component.translatable("config.machine_blueprint.blueprint.none");
        }
        var entry = MachineBlueprintResource.INSTANCE.getResourceInstance().listAllResourceEntries().stream()
                .filter(candidate -> candidate.path().equals(path))
                .findFirst()
                .orElse(null);
        return entry == null
                ? Component.translatable("config.machine_blueprint.blueprint.missing", path.getPath())
                : Component.literal(entry.getResourceName());
    }

    @Nullable
    private IResourcePath currentPath() {
        var stored = getValue();
        if (stored == null || stored.equals(MachineBlueprintBinding.NO_BLUEPRINT)) return null;
        try {
            return IResourcePath.parse(stored);
        } catch (Exception ignored) {
            // A path saved by an older build, or one whose provider type is gone. Reads as missing
            // rather than throwing out of a UI refresh.
            return null;
        }
    }

    protected void showBlueprintDialog(UIEvent event) {
        var previous = getValue();
        var instance = MachineBlueprintResource.INSTANCE.getResourceInstance();
        instance.createSelectorDialog(event.x, event.y, tag -> {
            // The dialog hands back the resource's value; the path is what we store. Identity-matched
            // by the resource instance's own cache, which is where the dialog got the value from.
            var path = instance.findResourcePath(tag);
            applyPath(path == null ? MachineBlueprintBinding.NO_BLUEPRINT : path.getPathWithType());
        }, () -> applyPath(previous), currentPath()).show(getModularUI());
    }

    private void applyPath(@Nullable String path) {
        onValueUpdatePassively(path == null ? MachineBlueprintBinding.NO_BLUEPRINT : path);
        updateValue();
    }

    @Override
    protected void onDropObject(@Nullable Object object) {
        if (canDropObject(object)
                && object instanceof GraphResourceProviderContainer.DraggingGraph dragging) {
            applyPath(dragging.path().getPathWithType());
        }
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        if (currentPath() != null) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                updateValueActively(MachineBlueprintBinding.NO_BLUEPRINT);
                updateValue();
            });
        }
        return menu;
    }
}

package com.lowdragmc.mbd2.common.gui.editor.multiblopck;

import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import dev.vfyjxf.taffy.style.AlignItems;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PredicatePathConfigurator extends ValueConfigurator<IResourcePath> {
    private final UIElement preview = new UIElement();

    public PredicatePathConfigurator(Supplier<@Nullable IResourcePath> supplier,
                                     Consumer<@Nullable IResourcePath> onUpdate) {
        this("", supplier, onUpdate);
    }

    public PredicatePathConfigurator(String name,
                                     Supplier<@Nullable IResourcePath> supplier,
                                     Consumer<@Nullable IResourcePath> onUpdate) {
        super(name, supplier, onUpdate, MultiblockMachineProject.builtinPath("any"), true);
        setTips("editor.drag_drop_resource");
        if (value == null) {
            value = MultiblockMachineProject.builtinPath("any");
        }
        inlineContainer.layout(layout -> {
            layout.maxHeight(100);
        });
        preview.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.setAspectRatio(1.0f);
            layout.widthPercent(100);
            layout.maxWidth(100);
            layout.maxHeight(100);
            layout.alignSelf(AlignItems.CENTER);
            layout.paddingAll(3);
            layout.setPipelineState(StyleOrigin.INLINE);
        });
        preview.style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)))
                .addClass("preview_bg");
        preview.addEventListener(UIEvents.MOUSE_DOWN, this::showPredicateDialog);
        inlineContainer.addChild(preview);

        setPastable(IResourcePath.class, pasted -> {
            if (pasted != null && isValidPath(pasted)) {
                onPaste(pasted);
            }
        });
        setCanDropPredicate(obj -> obj instanceof IResourcePath path &&
                isValidPath(path) || findResourcePath(obj) != null);
        setCopiableDirect(value);
        refresh();
    }

    protected void showPredicateDialog(UIEvent event) {
        var previous = getValue();
        PredicateResource.getINSTANCE().getResourceInstance().createSelectorDialog(event.x, event.y, predicate -> {
            var path = findResourcePath(predicate);
            if (path != null) {
                onValueUpdatePassively(path);
                updateValue();
            }
        }, () -> {
            if (previous == null) return;
            onValueUpdatePassively(previous);
            updateValue();
        }).show(getModularUI());
    }

    @Override
    protected void onValueUpdatePassively(@Nullable IResourcePath newValue) {
        if (newValue != null && newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        refresh();
    }

    @Override
    protected void onDropObject(@Nullable Object object) {
        if (object instanceof IResourcePath path && isValidPath(path)) {
            updateValueActively(path);
            return;
        }
        var path = findResourcePath(object);
        if (path != null) {
            updateValueActively(path);
        }
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        var path = getValue();
        if (path != null && !path.equals(MultiblockMachineProject.builtinPath("any"))) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                updateValueActively(MultiblockMachineProject.builtinPath("any"));
                updateValue();
            });
        }
        return menu;
    }

    private void refresh() {
        var path = value == null ? MultiblockMachineProject.builtinPath("any") : value;
        var predicate = PredicateResource.getINSTANCE().getResourceInstance().getResource(path);
        preview.style(style -> style.backgroundTexture(predicate == null ? IGuiTexture.EMPTY : predicate.getPreviewTexture()));
        setCopiableDirect(path);
    }

    private static boolean isValidPath(IResourcePath path) {
        return PredicateResource.getINSTANCE().getResourceInstance().getResource(path) != null;
    }

    @Nullable
    private static IResourcePath findResourcePath(@Nullable Object resource) {
        if (resource == null) return null;
        var resources = PredicateResource.getINSTANCE().getResourceInstance().listAllResources();
        for (var entry : resources) {
            if (entry.getValue() == resource) {
                return entry.getKey();
            }
        }
        for (var entry : resources) {
            if (entry.getValue().equals(resource)) {
                return entry.getKey();
            }
        }
        return null;
    }
}

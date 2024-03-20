package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A trait that have UI representation.
 */
public interface ITraitUIProvider {

    /**
     * Get the trait definition.
     */
    default TraitDefinition getDefinition() {
        return (TraitDefinition) this;
    }

    /**
     * Get the UI prefix name.
     */
    default String uiPrefixName() {
        return "ui:" + getDefinition().getName();
    }

    /**
     * Create a template widget for this trait.
     */
    Widget createTraitUITemplate();

    /**
     * Widget initialization.
     */
    void initTraitUI(ITrait trait, WidgetGroup group);

    static List<Widget> getWidgetsById(WidgetGroup group, String regex) {
        return group.getWidgetsById(Pattern.compile(regex));
    }

    @Nullable
    static Widget getFirstWidgetById(WidgetGroup group, String regex) {
        return group.getFirstWidgetById(Pattern.compile(regex));
    }

    static void widgetByIdForEach(WidgetGroup group, String regex, Consumer<Widget> consumer) {
        getWidgetsById(group, regex).forEach(consumer);
    }

    static <T extends Widget> void widgetByIdForEach(WidgetGroup group, String regex, Class<T> clazz, Consumer<T> consumer) {
        for (Widget widget : getWidgetsById(group, regex)) {
            if (clazz.isInstance(widget)) {
                consumer.accept(clazz.cast(widget));
            }
        }
    }

    static int widgetIdIndex(Widget widget) {
        var id = widget.getId();
        if (id == null) return -1;
        var split = id.split("_");
        if (split.length == 0) return -1;
        var end = split[split.length - 1];
        try {
            return Integer.parseInt(end);
        } catch (Exception e) {
            return -1;
        }
    }
}

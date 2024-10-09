package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

/**
 * A trait that have UI representation.
 */
public interface IUIProviderTrait {

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
    void createTraitUITemplate(WidgetGroup ui);

    /**
     * Widget initialization.
     */
    void initTraitUI(ITrait trait, WidgetGroup group);

}

package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

/**
 * A trait that have UI representation.
 */
public interface IUIProviderTrait {
    /**
     * Layout hint for auto-generated UI placement.
     * SLOT: grid of slots/tanks, placed in IO columns (left for input, right for output).
     * BAR: full-width bar, placed below the IO row (energy, heat, pressure, etc.).
     */
    enum TraitUILayoutType {
        SLOT,
        BAR
    }

    /**
     * Get the trait definition.
     */
    default TraitDefinition getDefinition() {
        return (TraitDefinition) this;
    }

    /**
     * Get the UI prefix name.
     */
    default String uiId() {
        return "ui:" + getDefinition().getName();
    }

    /**
     * Get the layout type hint for auto-generated UI placement.
     */
    default TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.SLOT;
    }

    /**
     * Create trait UI elements and add them to the given container.
     */
    void createTraitUITemplate(UIElement container);

    /**
     * Widget initialization at runtime.
     */
    void initTraitUI(ITrait trait, UI ui);

}

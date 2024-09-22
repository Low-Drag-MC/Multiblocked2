package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public abstract class SimpleCapabilityTraitDefinition<T, CONTENT> extends TraitDefinition implements ITraitUIProvider {
    public static class CapabilityIO {
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.internal", tips = "config.definition.trait.capability_io.internal.tooltip")
        private IO internal = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.front")
        private IO frontIO = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.back")
        private IO backIO = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.left")
        private IO leftIO = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.right")
        private IO rightIO = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.top")
        private IO topIO = IO.BOTH;
        @Getter @Setter
        @Configurable(name = "config.definition.trait.capability_io.bottom")
        private IO bottomIO = IO.BOTH;
    }

    @Getter
    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Getter @Setter
    @Configurable(name = "config.definition.trait.recipe_handler", tips = "config.definition.trait.recipe_handler.tooltip")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO recipeHandlerIO = IO.IN;

    @Getter @Setter
    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Getter @Setter
    @Configurable(name = "config.definition.trait.distinct", tips = {"config.definition.trait.distinct.tooltip.0", "config.definition.trait.distinct.tooltip.1"})
    private boolean isDistinct;

    @Getter @Setter
    @Configurable(name = "config.definition.trait.slot_names", tips = "config.definition.trait.slot_names.tooltip")
    private String[] slotNames = new String[0];

    @Override
    public abstract SimpleCapabilityTrait<T, CONTENT> createTrait(MBDMachine machine);

    /**
     * Refer to the recipe capability.
     */
    public abstract RecipeCapability<CONTENT> getRecipeCapability();

    /**
     * Get the capability for {@link ICapabilityProvider}.
     */
    public abstract Capability<?> getCapability();

}

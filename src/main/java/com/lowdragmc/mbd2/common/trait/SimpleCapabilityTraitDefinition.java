package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public abstract class SimpleCapabilityTraitDefinition<T, CONTENT> extends RecipeCapabilityTraitDefinition<CONTENT> implements IUIProviderTrait {
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

        public IO getIO(Direction front, @Nullable Direction side) {
            if (side == null || front.getAxis() == Direction.Axis.Y) {
                return internal;
            }
            if (side == Direction.UP) {
                return topIO;
            } else if (side == Direction.DOWN) {
                return bottomIO;
            } else if (side == front) {
                return frontIO;
            } else if (side == front.getOpposite()) {
                return backIO;
            } else if (side == front.getClockWise()) {
                return rightIO;
            } else if (side == front.getCounterClockWise()) {
                return leftIO;
            }
            return IO.NONE;
        }
    }

    @Getter
    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Getter @Setter
    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Override
    public abstract SimpleCapabilityTrait<T, CONTENT> createTrait(MBDMachine machine);

    /**
     * Get the capability for {@link ICapabilityProvider}.
     */
    public abstract Capability<? super T> getCapability();

}

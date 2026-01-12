package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Setter;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleCapabilityTrait extends RecipeCapabilityTrait {
    /**
     * Override the capability IO for dynamic using.
     */
    @Setter
    private CapabilityIO capabilityIOOverride = null;

    public SimpleCapabilityTrait(MBDMachine machine, SimpleCapabilityTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public SimpleCapabilityTraitDefinition getDefinition() {
        return (SimpleCapabilityTraitDefinition)super.getDefinition();
    }

    public IO getCapabilityIO(@Nullable Direction side) {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        var IO = capabilityIOOverride == null ? getDefinition().getCapabilityIO() : capabilityIOOverride;
        return IO.getIO(front, side);
    }

}

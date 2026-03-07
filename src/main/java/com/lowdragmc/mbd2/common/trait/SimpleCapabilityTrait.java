package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Setter;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleCapabilityTrait<T, C extends @Nullable Object> extends RecipeCapabilityTrait {
    /**
     * Override the capability IO for dynamic using.
     */
    @Setter
    private CapabilityIO capabilityIOOverride = null;

    public SimpleCapabilityTrait(MBDMachine machine, SimpleCapabilityTraitDefinition<T, C>  definition) {
        super(machine, definition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SimpleCapabilityTraitDefinition<T, C> getDefinition() {
        return (SimpleCapabilityTraitDefinition<T, C> ) super.getDefinition();
    }

    /**
     * Get capability IO direction of the specific side.
     * <br/>
     * For example, whether you can insert or extract items from the specific side.
     */
    public IO getCapabilityIO(@Nullable C ctx) {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        var IO = capabilityIOOverride == null ? getDefinition().getCapabilityIO() : capabilityIOOverride;
        return IO.getIO(front, ctx instanceof Direction direction ? direction : null);
    }

    /**
     * Get the capability content with the given IO direction.
     */
    public abstract T getCapContent(IO capabilityIO);
}

package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeCapabilityIO;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleCapabilityTrait<T, C extends @Nullable Object> extends RecipeCapabilityTrait {
    /**
     * Per-machine override of which sides expose this trait's capability. Unset sides read the
     * {@link SimpleCapabilityTraitDefinition#getCapabilityIO() definition}.
     */
    public final RuntimeCapabilityIO capabilityIO =
            new RuntimeCapabilityIO(runtimeValues, "capability_io", () -> getDefinition().getCapabilityIO());

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
        return capabilityIO.getIO(front, ctx instanceof Direction direction ? direction : null);
    }

    /**
     * Set the capability IO of one side at runtime. {@code side} is a world direction, resolved against
     * the machine's current facing, so the override lands on the machine-relative side and rotates with
     * the machine afterwards. A {@code null} side targets the internal IO — the one used when a
     * capability is queried without a direction.
     */
    public void setCapabilityIOSide(@Nullable Direction side, IO io) {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        capabilityIO.slot(front, side).set(io);
    }

    /** Drop every capability IO override, going back to the definition. */
    public void clearCapabilityIO() {
        capabilityIO.clearAll();
    }

    /**
     * @deprecated use {@link #capabilityIO} — the per-side runtime values, which persist per machine.
     *             This shim overrides all seven sides at once from the given object.
     */
    @Deprecated
    public void setCapabilityIOOverride(@Nullable CapabilityIO override) {
        if (override == null) {
            capabilityIO.clearAll();
            return;
        }
        capabilityIO.internal.set(override.getInternal());
        capabilityIO.front.set(override.getFrontIO());
        capabilityIO.back.set(override.getBackIO());
        capabilityIO.left.set(override.getLeftIO());
        capabilityIO.right.set(override.getRightIO());
        capabilityIO.top.set(override.getTopIO());
        capabilityIO.bottom.set(override.getBottomIO());
    }

    /**
     * Get the capability content with the given IO direction.
     */
    public abstract T getCapContent(IO capabilityIO);
}

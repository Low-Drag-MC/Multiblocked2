package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * This interface is used to mark a trait as an auto IO trait.
 * Auto IO traits are traits that automatically handle the IO of the machine based on the configuration.
 * e.g. Auto extract items from the machine's inventory, auto insert items to the machine's inventory, etc.
 */
public interface IAutoIOTrait extends IProxyAutoIOTrait {
    /**
     * @return the per-machine auto IO values of this trait, falling back to its definition where nothing
     *         has been overridden. If the trait does not support auto IO, return null instead.
     */
    @Nullable
    RuntimeAutoIO getRuntimeAutoIO();

    @Override
    default void serverTick() {
        var autoIO = getRuntimeAutoIO();
        if (autoIO == null || !autoIO.enable.get()) return;
        if (getMachine().getOffsetTimer() % autoIO.intervalTicks() != 0) return;
        var pos = getMachine().getPos();
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        for (var side : Direction.values()) {
            var io = autoIO.getIO(front, side);
            if (io != IO.NONE) {
                handleAutoIO(pos, side, io);
            }
        }
    }

    /** Turn this trait's auto IO on or off for this machine only. */
    default void setAutoIOEnabled(boolean enabled) {
        var autoIO = getRuntimeAutoIO();
        if (autoIO != null) {
            autoIO.enable.set(enabled);
        }
    }

    /**
     * Set the auto IO direction of one side for this machine only.
     * <p>
     * {@code side} is a world direction; it is resolved against the machine's current facing, so the
     * override lands on the machine-relative side and rotates with the machine afterwards.
     */
    default void setAutoIOSide(Direction side, IO io) {
        var autoIO = getRuntimeAutoIO();
        if (autoIO != null) {
            autoIO.slot(getMachine().getFrontFacing().orElse(Direction.NORTH), side).set(io);
        }
    }

    /** Set how often auto IO runs, in ticks, for this machine only. */
    default void setAutoIOInterval(int interval) {
        var autoIO = getRuntimeAutoIO();
        if (autoIO != null) {
            autoIO.interval.set(interval);
        }
    }

    /** Drop every auto IO override, going back to the definition. */
    default void clearAutoIO() {
        var autoIO = getRuntimeAutoIO();
        if (autoIO != null) {
            autoIO.clearAll();
        }
    }
}

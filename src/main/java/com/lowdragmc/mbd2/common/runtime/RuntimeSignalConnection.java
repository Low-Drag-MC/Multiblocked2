package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings.SignalConnection;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The runtime view of a {@link SignalConnection} config object: one {@link RuntimeValue} per side.
 * <p>
 * {@link #getConnection} mirrors {@link SignalConnection#getConnection} exactly.
 */
public final class RuntimeSignalConnection {
    public final RuntimeValue<Boolean> front;
    public final RuntimeValue<Boolean> back;
    public final RuntimeValue<Boolean> left;
    public final RuntimeValue<Boolean> right;
    public final RuntimeValue<Boolean> top;
    public final RuntimeValue<Boolean> bottom;

    /**
     * @param prefix   the slot key prefix, e.g. {@code "signal_connection"}
     * @param authored supplies the definition's config object; must be a lambda, it is read lazily
     */
    public RuntimeSignalConnection(RuntimeValueStorage storage, String prefix, Supplier<SignalConnection> authored) {
        // Neighbours cache redstone connectivity, so a change has to push a block update out.
        Runnable update = () -> {
            var machine = storage.getHolder().runtimeValueMachine();
            if (machine != null) {
                machine.updateSignal();
            }
        };
        front = storage.ofBool(prefix + ".front", () -> authored.get().frontConnection()).onChanged(update);
        back = storage.ofBool(prefix + ".back", () -> authored.get().backConnection()).onChanged(update);
        left = storage.ofBool(prefix + ".left", () -> authored.get().leftConnection()).onChanged(update);
        right = storage.ofBool(prefix + ".right", () -> authored.get().rightConnection()).onChanged(update);
        top = storage.ofBool(prefix + ".top", () -> authored.get().topConnection()).onChanged(update);
        bottom = storage.ofBool(prefix + ".bottom", () -> authored.get().bottomConnection()).onChanged(update);
    }

    /** @see SignalConnection#getConnection(Direction, Direction) */
    public boolean getConnection(Direction front, @Nullable Direction side) {
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) {
                return this.front.get();
            } else if (side == front.getOpposite()) {
                return back.get();
            } else {
                return top.get();
            }
        }
        if (side == Direction.UP) {
            return top.get();
        } else if (side == Direction.DOWN) {
            return bottom.get();
        } else if (side == front) {
            return this.front.get();
        } else if (side == front.getOpposite()) {
            return back.get();
        } else if (side == front.getClockWise()) {
            return right.get();
        } else if (side == front.getCounterClockWise()) {
            return left.get();
        }
        return false;
    }

    /** The slot for a machine-relative side. Not nullable — there is no directionless slot here. */
    public RuntimeValue<Boolean> slot(Direction front, Direction side) {
        java.util.Objects.requireNonNull(side, "signal connection has no slot for a null side");
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) return this.front;
            if (side == front.getOpposite()) return back;
            return top;
        }
        if (side == Direction.UP) return top;
        if (side == Direction.DOWN) return bottom;
        if (side == front) return this.front;
        if (side == front.getOpposite()) return back;
        if (side == front.getClockWise()) return right;
        return left;
    }

    /** Drop every override in this view, going back to the definition. */
    public void clearAll() {
        front.clear();
        back.clear();
        left.clear();
        right.clear();
        top.clear();
        bottom.clear();
    }
}

package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The runtime view of a {@link ToggleAutoIO} config object: {@code enable}, one {@link RuntimeValue} per
 * side, and {@code interval}. Each is independently overridable, so turning auto-IO off for one machine
 * leaves its six side directions and its interval still reading the definition.
 * <p>
 * {@link #getIO} mirrors {@link AutoIO#getIO} exactly.
 */
public final class RuntimeAutoIO {
    public final RuntimeValue<Boolean> enable;
    public final RuntimeValue<IO> front;
    public final RuntimeValue<IO> back;
    public final RuntimeValue<IO> left;
    public final RuntimeValue<IO> right;
    public final RuntimeValue<IO> top;
    public final RuntimeValue<IO> bottom;
    public final RuntimeValue<Integer> interval;

    /**
     * @param prefix   the slot key prefix, e.g. {@code "auto_io"}
     * @param authored supplies the definition's config object; must be a lambda, it is read lazily
     */
    public RuntimeAutoIO(RuntimeValueStorage storage, String prefix, Supplier<ToggleAutoIO> authored) {
        enable = storage.ofBool(prefix + ".enable", () -> authored.get().isEnable());
        front = storage.ofEnum(prefix + ".front", IO.class, () -> authored.get().getFrontIO());
        back = storage.ofEnum(prefix + ".back", IO.class, () -> authored.get().getBackIO());
        left = storage.ofEnum(prefix + ".left", IO.class, () -> authored.get().getLeftIO());
        right = storage.ofEnum(prefix + ".right", IO.class, () -> authored.get().getRightIO());
        top = storage.ofEnum(prefix + ".top", IO.class, () -> authored.get().getTopIO());
        bottom = storage.ofEnum(prefix + ".bottom", IO.class, () -> authored.get().getBottomIO());
        // Math.max(1, ...) at the read site rather than here: an interval of 0 would divide by zero in
        // the tick check, and a definition authored before the range constraint can still hold one.
        interval = storage.ofInt(prefix + ".interval", () -> authored.get().getInterval());
    }

    /**
     * The tick period, never below 1.
     * <p>
     * {@code Math.abs} rather than a plain floor: the old code did {@code timer % interval}, which for a
     * negative interval gives a period of {@code |interval|}, and definitions built through KubeJS or
     * saved before the editor's {@code [1, MAX]} clamp can still hold one. Only {@code 0} changes
     * behaviour, and that used to be an {@code ArithmeticException}.
     */
    public int intervalTicks() {
        return Math.max(1, Math.abs(interval.get()));
    }

    /** @see AutoIO#getIO(Direction, Direction) */
    public IO getIO(Direction front, @Nullable Direction side) {
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
        return IO.NONE;
    }

    /**
     * The slot for a machine-relative side.
     * <p>
     * {@code side} is not nullable here, unlike in {@link #getIO}: auto IO has no "internal" slot, so
     * there is nothing sensible to return for a directionless query — {@code getIO} answers
     * {@link IO#NONE} for one, which is not a slot.
     */
    public RuntimeValue<IO> slot(Direction front, Direction side) {
        Objects.requireNonNull(side, "auto IO has no slot for a null side");
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

    /** Override every side at once, the runtime equivalent of {@link AutoIO#setAllIO}. */
    public void setAll(IO io) {
        front.set(io);
        back.set(io);
        left.set(io);
        right.set(io);
        top.set(io);
        bottom.set(io);
        enable.set(io != IO.NONE);
    }

    /** Drop every override in this view, going back to the definition. */
    public void clearAll() {
        enable.clear();
        front.clear();
        back.clear();
        left.clear();
        right.clear();
        top.clear();
        bottom.clear();
        interval.clear();
    }
}

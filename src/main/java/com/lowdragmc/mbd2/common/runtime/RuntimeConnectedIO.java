package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.common.trait.ConnectedIO;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The runtime view of a {@link ConnectedIO} config object: one {@link RuntimeValue} per side.
 * <p>
 * {@link #getConnection} mirrors {@link ConnectedIO#getConnection} exactly.
 */
public final class RuntimeConnectedIO {
    public final RuntimeValue<Boolean> front;
    public final RuntimeValue<Boolean> back;
    public final RuntimeValue<Boolean> left;
    public final RuntimeValue<Boolean> right;
    public final RuntimeValue<Boolean> top;
    public final RuntimeValue<Boolean> bottom;

    /**
     * @param prefix   the slot key prefix, e.g. {@code "connection_io"}
     * @param authored supplies the definition's config object; must be a lambda, it is read lazily
     * @param onChanged run whenever any side changes. Required rather than optional because every known
     *                  consumer of a connection IO caches the resulting face list, so an override that
     *                  does not invalidate that cache is invisible — pass {@code () -> {}} only if the
     *                  reader genuinely re-reads every tick.
     */
    public RuntimeConnectedIO(RuntimeValueStorage storage, String prefix, Supplier<ConnectedIO> authored,
                              Runnable onChanged) {
        front = storage.ofBool(prefix + ".front", () -> authored.get().isFrontIO()).onChanged(onChanged);
        back = storage.ofBool(prefix + ".back", () -> authored.get().isBackIO()).onChanged(onChanged);
        left = storage.ofBool(prefix + ".left", () -> authored.get().isLeftIO()).onChanged(onChanged);
        right = storage.ofBool(prefix + ".right", () -> authored.get().isRightIO()).onChanged(onChanged);
        top = storage.ofBool(prefix + ".top", () -> authored.get().isTopIO()).onChanged(onChanged);
        bottom = storage.ofBool(prefix + ".bottom", () -> authored.get().isBottomIO()).onChanged(onChanged);
    }

    /** @see ConnectedIO#getConnection(Direction, Direction) */
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
        java.util.Objects.requireNonNull(side, "connection IO has no slot for a null side");
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

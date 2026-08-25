package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.trait.CapabilityIO;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The runtime view of a {@link CapabilityIO} config object: one {@link RuntimeValue} per side, plus the
 * internal one. Overriding a single side leaves the other six still reading the definition.
 * <p>
 * {@link #getIO} mirrors {@link CapabilityIO#getIO} exactly, so swapping a call site over is a
 * one-for-one substitution.
 */
public final class RuntimeCapabilityIO {
    public final RuntimeValue<IO> internal;
    public final RuntimeValue<IO> front;
    public final RuntimeValue<IO> back;
    public final RuntimeValue<IO> left;
    public final RuntimeValue<IO> right;
    public final RuntimeValue<IO> top;
    public final RuntimeValue<IO> bottom;

    /**
     * @param prefix   the slot key prefix, e.g. {@code "capability_io"}
     * @param authored supplies the definition's config object; must be a lambda, it is read lazily
     */
    public RuntimeCapabilityIO(RuntimeValueStorage storage, String prefix, Supplier<CapabilityIO> authored) {
        // Changing which sides expose a capability has to invalidate NeoForge's BlockCapabilityCache,
        // otherwise neighbouring pipes keep the resolution they made before the change. Same reason
        // MBDPartMachine invalidates around form/unform.
        Runnable invalidate = () -> {
            var machine = storage.getHolder().runtimeValueMachine();
            if (machine != null) {
                machine.invalidateCapabilities();
                machine.notifyBlockUpdate();
            }
        };
        internal = storage.ofEnum(prefix + ".internal", IO.class, () -> authored.get().getInternal()).onChanged(invalidate);
        front = storage.ofEnum(prefix + ".front", IO.class, () -> authored.get().getFrontIO()).onChanged(invalidate);
        back = storage.ofEnum(prefix + ".back", IO.class, () -> authored.get().getBackIO()).onChanged(invalidate);
        left = storage.ofEnum(prefix + ".left", IO.class, () -> authored.get().getLeftIO()).onChanged(invalidate);
        right = storage.ofEnum(prefix + ".right", IO.class, () -> authored.get().getRightIO()).onChanged(invalidate);
        top = storage.ofEnum(prefix + ".top", IO.class, () -> authored.get().getTopIO()).onChanged(invalidate);
        bottom = storage.ofEnum(prefix + ".bottom", IO.class, () -> authored.get().getBottomIO()).onChanged(invalidate);
    }

    /** @see CapabilityIO#getIO(Direction, Direction) */
    public IO getIO(Direction front, @Nullable Direction side) {
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) {
                return this.front.get();
            } else if (side == front.getOpposite()) {
                return back.get();
            } else {
                return internal.get();
            }
        }
        if (side == null) {
            return internal.get();
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

    /** The slot for a machine-relative side, or {@link #internal} for a null side. */
    public RuntimeValue<IO> slot(Direction front, @Nullable Direction side) {
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) return this.front;
            if (side == front.getOpposite()) return back;
            return internal;
        }
        if (side == null) return internal;
        if (side == Direction.UP) return top;
        if (side == Direction.DOWN) return bottom;
        if (side == front) return this.front;
        if (side == front.getOpposite()) return back;
        if (side == front.getClockWise()) return right;
        return left;
    }

    /** Override every side at once, the runtime equivalent of {@link CapabilityIO#setAllIO}. */
    public void setAll(IO io) {
        internal.set(io);
        front.set(io);
        back.set(io);
        left.set(io);
        right.set(io);
        top.set(io);
        bottom.set(io);
    }

    /** Drop every side override, going back to the definition. */
    public void clearAll() {
        internal.clear();
        front.clear();
        back.clear();
        left.clear();
        right.clear();
        top.clear();
        bottom.clear();
    }
}

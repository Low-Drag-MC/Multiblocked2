package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.mbd2.common.trait.AutoWorldIO;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The runtime view of an {@link AutoWorldIO} config object: {@code enable}, {@code range},
 * {@code interval} and {@code speed}, each independently overridable.
 * <p>
 * Also fixes a latent bug in the config object it replaces: {@link AutoWorldIO} memoises its rotated
 * ranges in a map that lives on the <b>shared definition</b> and is keyed only by {@link Direction}, so it
 * goes stale the moment the range changes. The cache here is per-trait and keyed on the effective range,
 * so a per-machine range override and an editor edit both take effect immediately.
 */
public final class RuntimeAutoWorldIO {
    public final RuntimeValue<Boolean> enable;
    public final RuntimeValue<AABB> range;
    public final RuntimeValue<Integer> interval;
    public final RuntimeValue<Integer> speed;

    private final RotatedRangeCache rotatedRange;

    /**
     * @param prefix   the slot key prefix, e.g. {@code "auto_world_input"}
     * @param authored supplies the definition's config object; must be a lambda, it is read lazily
     */
    public RuntimeAutoWorldIO(RuntimeValueStorage storage, String prefix, Supplier<AutoWorldIO> authored) {
        enable = storage.ofBool(prefix + ".enable", () -> authored.get().isEnable());
        range = storage.ofAABB(prefix + ".range", () -> authored.get().getRange());
        interval = storage.ofInt(prefix + ".interval", () -> authored.get().getInterval());
        speed = storage.ofInt(prefix + ".speed", () -> authored.get().getSpeed());
        rotatedRange = new RotatedRangeCache(range);
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

    /** The effective range rotated to {@code direction}; null and NORTH return it unrotated. */
    public AABB getRotatedRange(@Nullable Direction direction) {
        return rotatedRange.get(direction);
    }

    /** Drop every override in this view, going back to the definition. */
    public void clearAll() {
        enable.clear();
        range.clear();
        interval.clear();
        speed.clear();
    }
}

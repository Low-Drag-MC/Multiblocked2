package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Memoises the facing-rotated forms of an AABB {@link RuntimeValue}.
 * <p>
 * This replaces the caches that used to live on {@code AutoWorldIO} and
 * {@code EntityHandlerTraitDefinition} — both on the <b>shared definition</b> and keyed only by
 * {@link Direction}, so every machine of a type shared one and it went stale whenever the range changed.
 * {@code EntityHandlerTraitDefinition} worked around that by clearing its cache from a
 * {@code @ConfigSetter} and from {@code deserializeNBT}; {@code AutoWorldIO} did not, and was simply
 * stale. Both are now deleted.
 * <p>
 * This cache lives on the trait instance and keys on the effective range, so a per-machine override and
 * an editor edit both take effect immediately with no invalidation hook to remember.
 * <p>
 * Not thread safe — call it from the server tick only.
 */
public final class RotatedRangeCache {
    private final RuntimeValue<AABB> source;
    private final Map<Direction, AABB> rotated = new EnumMap<>(Direction.class);
    @Nullable
    private AABB cachedFor;

    public RotatedRangeCache(RuntimeValue<AABB> source) {
        this.source = source;
    }

    /** The effective range rotated to {@code direction}; {@code null} and NORTH return it unrotated. */
    public AABB get(@Nullable Direction direction) {
        var current = source.get();
        if (!current.equals(cachedFor)) {
            rotated.clear();
            cachedFor = current;
        }
        if (direction == null || direction == Direction.NORTH) {
            return current;
        }
        return rotated.computeIfAbsent(direction, dir -> ShapeUtils.rotate(current, dir));
    }
}

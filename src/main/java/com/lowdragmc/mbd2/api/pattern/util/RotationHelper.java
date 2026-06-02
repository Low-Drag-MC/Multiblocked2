package com.lowdragmc.mbd2.api.pattern.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * Pattern authoring assumes a controller facing {@link Direction#NORTH}. This helper maps
 * a target horizontal facing to the {@link Rotation} that transforms NORTH-authored
 * coordinates and block states into that facing's frame. It is the single source of truth
 * for pattern rotation, used by {@code BlockPattern.autoBuild}, the in-world preview
 * renderer, and {@code rotateFollowController} predicates.
 */
public final class RotationHelper {

    private RotationHelper() {}

    /**
     * @return the rotation that maps NORTH-authored coordinates/states onto {@code facing}.
     * Non-horizontal facings map to {@link Rotation#NONE}.
     */
    public static Rotation rotationFromFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * @return the rotation that undoes {@code rotation} (applies its inverse).
     */
    public static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    public static BlockPos rotateOffset(BlockPos offset, Direction facing) {
        return offset.rotate(rotationFromFacing(facing));
    }
}

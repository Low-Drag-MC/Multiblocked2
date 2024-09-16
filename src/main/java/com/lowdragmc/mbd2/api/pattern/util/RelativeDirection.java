package com.lowdragmc.mbd2.api.pattern.util;

import net.minecraft.core.Direction;

import java.util.function.Function;

/**
 * Relative direction when facing horizontally
 */
public enum RelativeDirection {
    UP(f -> Direction.UP, Direction.Axis.Y),
    DOWN(f -> Direction.DOWN, Direction.Axis.Y),
    LEFT(Direction::getCounterClockWise, Direction.Axis.X),
    RIGHT(Direction::getClockWise, Direction.Axis.X),
    FRONT(Function.identity(), Direction.Axis.Z),
    BACK(Direction::getOpposite, Direction.Axis.Z);

    final Function<Direction, Direction> actualFacing;
    public final Direction.Axis axis;

    RelativeDirection(Function<Direction, Direction> actualFacing, Direction.Axis axis) {
        this.actualFacing = actualFacing;
        this.axis = axis;
    }

    public static RelativeDirection getAisleDirection(Direction.Axis layerAxis, Direction controllerFace) {
        return switch(controllerFace) {
            case NORTH -> switch (layerAxis) {
                case X -> RIGHT;
                case Y -> UP;
                case Z -> BACK;
            };
            case SOUTH -> switch (layerAxis) {
                case X -> LEFT;
                case Y -> UP;
                case Z -> FRONT;
            };
            case WEST -> switch (layerAxis) {
                case X -> BACK;
                case Y -> UP;
                case Z -> LEFT;
            };
            case EAST -> switch (layerAxis) {
                case X -> FRONT;
                case Y -> UP;
                case Z -> RIGHT;
            };
            default -> throw new IllegalArgumentException("Invalid controller face: " + controllerFace);
        };
    }

    public static RelativeDirection getSliceXDirection(Direction.Axis layerAxis, Direction controllerFace) {
        return switch(controllerFace) {
            case NORTH -> switch (layerAxis) {
                case X -> UP;
                case Y -> RIGHT;
                case Z -> RIGHT;
            };
            case SOUTH -> switch (layerAxis) {
                case X -> UP;
                case Y -> LEFT;
                case Z -> LEFT;
            };
            case WEST -> switch (layerAxis) {
                case X -> UP;
                case Y -> BACK;
                case Z -> BACK;
            };
            case EAST -> switch (layerAxis) {
                case X -> UP;
                case Y -> FRONT;
                case Z -> FRONT;
            };
            default -> throw new IllegalArgumentException("Invalid controller face: " + controllerFace);
        };
    }

    public static RelativeDirection getSliceYDirection(Direction.Axis layerAxis, Direction controllerFace) {
        return switch(controllerFace) {
            case NORTH -> switch (layerAxis) {
                case X -> BACK;
                case Y -> BACK;
                case Z -> UP;
            };
            case SOUTH -> switch (layerAxis) {
                case X -> FRONT;
                case Y -> FRONT;
                case Z -> UP;
            };
            case WEST -> switch (layerAxis) {
                case X -> LEFT;
                case Y -> LEFT;
                case Z -> UP;
            };
            case EAST -> switch (layerAxis) {
                case X -> RIGHT;
                case Y -> RIGHT;
                case Z -> UP;
            };
            default -> throw new IllegalArgumentException("Invalid controller face: " + controllerFace);
        };
    }

    public Direction getActualFacing(Direction facing) {
        return actualFacing.apply(facing);
    }

    public boolean isSameAxis(RelativeDirection dir) {
        return this.axis == dir.axis;
    }

}

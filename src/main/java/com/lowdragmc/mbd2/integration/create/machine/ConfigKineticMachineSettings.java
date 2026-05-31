package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigKineticMachineSettings implements IConfigurable, IPersistedSerializable {
    public enum RotationFacing {
        FRONT, BACK, LEFT, RIGHT, UP, DOWN;

        public Direction getDirection(Direction frontFacing) {
            if (frontFacing.getAxis() == Direction.Axis.Y) {
                return switch (this) {
                    case FRONT -> frontFacing;
                    case BACK -> frontFacing.getOpposite();
                    case LEFT -> Direction.WEST;
                    case RIGHT -> Direction.EAST;
                    case UP -> Direction.NORTH;
                    case DOWN -> Direction.SOUTH;
                };
            }
            return switch (this) {
                case FRONT -> frontFacing;
                case BACK -> frontFacing.getOpposite();
                case LEFT -> frontFacing.getClockWise();
                case RIGHT -> frontFacing.getCounterClockWise();
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
        }
    }

    /**
     * How this machine accepts kinetic input from neighbors.
     * SHAFT: shaft-axis input only (default, original behavior).
     * SMALL_COGWHEEL / LARGE_COGWHEEL: cogwheel meshing only (no shaft).
     * SHAFT_AND_SMALL_COG / SHAFT_AND_LARGE_COG: both, like Create's PumpBlock.
     */
    public enum ConnectionType {
        SHAFT, SMALL_COGWHEEL, LARGE_COGWHEEL, SHAFT_AND_SMALL_COG, SHAFT_AND_LARGE_COG
    }

    @Configurable(name = "config.kinetic_machine.is_generator", tips = "config.kinetic_machine.is_generator.tooltip")
    @Builder.Default
    public boolean isGenerator = false;

    @Configurable(name = "config.kinetic_machine.torque",
            tips = {"config.kinetic_machine.torque.tooltip.0", "config.kinetic_machine.torque.tooltip.1"})
    @Builder.Default
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    public float torque = 4f;

    @Configurable(name = "config.kinetic_machine.front_rotation", tips = "config.kinetic_machine.front_rotation.tooltip")
    @Builder.Default
    public RotationFacing frontRotation = RotationFacing.FRONT;

    @Configurable(name = "config.kinetic_machine.has_back_rotation",
            tips = {"config.kinetic_machine.has_back_rotation.tooltip.0", "config.kinetic_machine.has_back_rotation.tooltip.1"})
    @Builder.Default
    public boolean hasBackRotation = true;

    @Configurable(name = "config.kinetic_machine.max_rpm",
            tips = {"config.kinetic_machine.max_rpm.tooltip.0", "config.kinetic_machine.max_rpm.tooltip.1"})
    @Builder.Default
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    public int maxRPM = 256;

    @Configurable(name = "config.kinetic_machine.connection_type",
            tips = "config.kinetic_machine.connection_type.tooltip")
    @Builder.Default
    public ConnectionType connectionType = ConnectionType.SHAFT;

    public Direction getRotationFacing(Direction frontFacing) {
        return frontRotation.getDirection(frontFacing);
    }

    public boolean hasShaft() {
        return connectionType == ConnectionType.SHAFT
                || connectionType == ConnectionType.SHAFT_AND_SMALL_COG
                || connectionType == ConnectionType.SHAFT_AND_LARGE_COG;
    }

    public boolean isSmallCog() {
        return connectionType == ConnectionType.SMALL_COGWHEEL
                || connectionType == ConnectionType.SHAFT_AND_SMALL_COG;
    }

    public boolean isLargeCog() {
        return connectionType == ConnectionType.LARGE_COGWHEEL
                || connectionType == ConnectionType.SHAFT_AND_LARGE_COG;
    }

    public boolean hasShaftTowards(Direction towardFace, Direction rotationFacing) {
        if (!hasShaft()) return false;
        if (towardFace.getAxis() == rotationFacing.getAxis()) {
            if (!hasBackRotation) {
                return towardFace == rotationFacing;
            }
            return true;
        }
        return false;
    }

    public float getCapacity() {
        return isGenerator ? Math.max(torque, Float.MIN_VALUE) : 0;
    }

    public float getImpact() {
        return isGenerator ? 0 : torque;
    }
}

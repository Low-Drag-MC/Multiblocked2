package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

@Getter
@Setter
public class ConnectedIO {
    @Configurable(name = "config.definition.trait.capability_io.front")
    private boolean frontIO = true;

    @Configurable(name = "config.definition.trait.capability_io.back")
    private boolean backIO = true;

    @Configurable(name = "config.definition.trait.capability_io.left")
    private boolean leftIO = true;

    @Configurable(name = "config.definition.trait.capability_io.right")
    private boolean rightIO = true;

    @Configurable(name = "config.definition.trait.capability_io.top")
    private boolean topIO = true;

    @Configurable(name = "config.definition.trait.capability_io.bottom")
    private boolean bottomIO = true;

    public boolean getConnection(Direction front, @Nullable Direction side) {
        if (front.getAxis() == Direction.Axis.Y) {
            if (side == front) {
                return frontIO;
            } else if (side == front.getOpposite()) {
                return backIO;
            } else {
                return topIO;
            }
        }
        if (side == Direction.UP) {
            return topIO;
        } else if (side == Direction.DOWN) {
            return bottomIO;
        } else if (side == front) {
            return frontIO;
        } else if (side == front.getOpposite()) {
            return backIO;
        } else if (side == front.getClockWise()) {
            return rightIO;
        } else if (side == front.getCounterClockWise()) {
            return leftIO;
        }
        return false;
    }
}

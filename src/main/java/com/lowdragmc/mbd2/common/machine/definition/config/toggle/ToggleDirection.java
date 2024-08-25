package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

public class ToggleDirection extends ToggleObject<Direction> {

    @Getter
    @Setter
    @Configurable
    private Direction value = Direction.NORTH;

    public ToggleDirection(Direction value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleDirection(Direction value) {
        this(value, true);
    }

    public ToggleDirection(boolean enable) {
        this(Direction.NORTH, enable);
    }

    public ToggleDirection() {
        this(false);
    }

}

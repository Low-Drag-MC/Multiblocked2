package com.lowdragmc.mbd2.utils;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

public class ControllerBlockInfo extends BlockInfo {

    @Getter
    @Setter
    private Direction facing;

    public ControllerBlockInfo() {
        this(Direction.NORTH);
    }

    public ControllerBlockInfo(Direction facing) {
        super();
        this.facing = facing;
    }
}

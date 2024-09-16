package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class ToggleAABB extends ToggleObject<AABB> {

    @Getter
    @Setter
    @Configurable
    @DefaultValue(numberValue = {0, 0, 0, 1, 1, 1})
    private AABB value;

    public ToggleAABB(AABB value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleAABB(AABB value) {
        this(value, true);
    }

    public ToggleAABB(boolean enable) {
        this(new AABB(BlockPos.ZERO), enable);
    }

    public ToggleAABB() {
        this(false);
    }
}

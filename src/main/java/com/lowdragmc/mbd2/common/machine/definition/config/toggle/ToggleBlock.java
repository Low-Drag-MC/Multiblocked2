package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ToggleBlock extends ToggleObject<Block> {

    @Getter
    @Setter
    @Configurable
    @DefaultValue(numberValue = {0, 0, 0, 1, 1, 1})
    private Block value;

    public ToggleBlock(Block value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleBlock(Block value) {
        this(value, false);
    }

    public ToggleBlock(boolean enable) {
        this(Blocks.COBBLESTONE, enable);
    }

    public ToggleBlock() {
        this(false);
    }
}

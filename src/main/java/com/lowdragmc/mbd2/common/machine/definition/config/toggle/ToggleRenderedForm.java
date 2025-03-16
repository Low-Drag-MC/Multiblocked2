package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ToggleRenderedForm extends ToggleObject<ToggleBlock> {
    @Getter
    @Setter
    @Configurable(name = "config.block_pattern.predicate.replaceBlock", tips = { "config.block_pattern.predicate.replaceBlock.tooltip.0", "config.block_pattern.predicate.replaceBlock.tooltip.1", "config.block_pattern.predicate.replaceBlock.tooltip.2" }, subConfigurable = true)
    private ToggleBlock value = new ToggleBlock();

    public ToggleRenderedForm(boolean enable, boolean subEnabled, Block subValue) {
        setEnable(enable);
        value.setEnable(subEnabled);
        value.setValue(subValue);
    }

    public ToggleRenderedForm() {
        this(false, false, Blocks.COBBLESTONE);
    }

    public ToggleRenderedForm(boolean b) {
        this(false, false, Blocks.COBBLESTONE);
    }
}

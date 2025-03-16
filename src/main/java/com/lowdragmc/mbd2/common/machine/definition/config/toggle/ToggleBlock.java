package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.trait.item.ItemFilterSettings;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;

public class ToggleBlock extends ToggleObject<Block> {

    @Getter
    @Setter
    @Configurable
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

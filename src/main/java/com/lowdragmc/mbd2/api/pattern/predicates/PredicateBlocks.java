package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

@LDLRegister(name = "blocks", registry = "mbd2:pattern_predicate", group = "predicate")
@NoArgsConstructor
public class PredicateBlocks extends PatternPredicate {

    @Configurable(name = "config.predicate.blocks", tips = "config.predicate.blocks.tooltip", collapse = false)
    protected Block[] blocks = new Block[] {Blocks.STONE};
    
    public PredicateBlocks(Block... blocks) {
        this.blocks = blocks;
        buildPredicate();
    }

    @ConfigSetter(field = "blocks")
    public void setBlocks(Block[] blocks) {
        this.blocks = blocks;
        buildPredicate();
    }

    @Override
    public PatternPredicate buildPredicate() {
        blocks = Arrays.stream(blocks).filter(Objects::nonNull).toArray(Block[]::new);
        if (blocks.length == 0) blocks = new Block[]{Blocks.BARRIER};
        predicate = state -> ArrayUtils.contains(blocks, state.getBlockState().getBlock());
        candidates = Suppliers.memoize(() -> Arrays.stream(blocks).map(BlockInfo::fromBlock).toArray(BlockInfo[]::new));
        return super.buildPredicate();
    }

}

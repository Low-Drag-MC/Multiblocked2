package com.lowdragmc.mbd2.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockEntityOwner {
    /**
     * Get the block entity holder.
     */
    BlockEntity getHolder();

    /**
     * Get the level.
     */
    default Level getLevel() {
        return getHolder().getLevel();
    }

    /**
     * Get machine position.
     */
    default BlockPos getPos() {
        return getHolder().getBlockPos();
    }

    /**
     * Get the block state.
     */
    default BlockState getBlockState() {
        return getHolder().getBlockState();
    }

    /**
     * Is the machine still valid.
     */
    default boolean isInValid() {
        return getHolder().isRemoved();
    }

    /**
     * Get the random offset.
     */
    long getOffset();

    /**
     * Get the offset timer.
     */
    default long getOffsetTimer() {
        var level = getLevel();
        return level == null ? getOffset() : (level.getGameTime() + getOffset());
    }

}

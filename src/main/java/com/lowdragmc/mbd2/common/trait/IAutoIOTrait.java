package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * This interface is used to mark a trait as an auto IO trait.
 * Auto IO traits are traits that automatically handle the IO of the machine based on the configuration.
 * e.g. Auto extract items from the machine's inventory, auto insert items to the machine's inventory, etc.
 */
public interface IAutoIOTrait extends IProxyAutoIOTrait {
    /**
     * @return the auto IO configuration of this trait by default.
     *        If the trait does not support / do not have the auto IO, return null instead.
     */
    @Nullable
    AutoIO getAutoIO();

    @Override
    default void serverTick() {
        var autoIO = getAutoIO();
        if (autoIO != null && getMachine().getOffsetTimer() % autoIO.getInterval() == 0) {
            var pos = getMachine().getPos();
            var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
            for (var side : Direction.values()) {
                var io = autoIO.getIO(front, side);
                if (io != IO.NONE) {
                    handleAutoIO(pos, side, io);
                }
            }
        }
    }
}

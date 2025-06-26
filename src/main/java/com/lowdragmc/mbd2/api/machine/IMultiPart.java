package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface IMultiPart extends IBlockEntityOwner{
    static Optional<IMultiPart> ofPart(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MULTI_PART).resolve();
    }

    static Optional<IMultiPart> ofPart(@Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return ofPart(level.getBlockEntity(pos));
    }

    /**
     * Can it be shared among multi multiblock.
     */
    default boolean canShared() {
        return true;
    }

    /**
     * Whether it belongs to the specified controller.
     */
    boolean hasController(BlockPos controllerPos);

    /**
     * Whether it belongs to a formed Multiblock.
     */
    boolean isFormed();

    /**
     * Get all attached controllers
     */
    List<IMultiControllerMachine> getControllers();

    /**
     * Called when it was removed from a multiblock.
     */
    void removedFromController(IMultiControllerMachine controller);

    /**
     * Called when it was added to a multiblock.
     */
    void addedToController(IMultiControllerMachine controller);
}

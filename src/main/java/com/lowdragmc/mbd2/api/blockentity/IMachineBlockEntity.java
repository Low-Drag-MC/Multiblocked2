package com.lowdragmc.mbd2.api.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A simple compound Interface for a BlockEntity which is holding a Machine feature.
 * <br>
 * Its using async system to sync data.
 */
public interface IMachineBlockEntity extends ISyncBlockEntity, IRPCBlockEntity, IPersistManagedHolder, Nameable {

    default BlockEntity self() {
        return (BlockEntity) this;
    }

    default Level level() {
        return self().getLevel();
    }

    default BlockPos pos() {
        return self().getBlockPos();
    }

    default void notifyBlockUpdate() {
        if (level() != null) {
            level().updateNeighborsAt(pos(), level().getBlockState(pos()).getBlock());
        }
    }

    default void scheduleRenderUpdate() {
        var pos = pos();
        if (level() != null) {
            var state = level().getBlockState(pos);
            if (level().isClientSide) {
                level().sendBlockUpdated(pos, state, state, 1 << 3);
            } else {
                level().blockEvent(pos, state.getBlock(), 1, 0);
            }
        }
    }

    default long getOffsetTimer() {
        return level() == null ? getOffset() : (level().getGameTime() + getOffset());
    }

    IMachine getMetaMachine();

    long getOffset();

    @Override
    default boolean isAsyncValid() {
        return !getSelf().isRemoved();
    }

    @Override
    default boolean useAsyncThread() {
        return true;
    }

    @Override
    default void saveCustomPersistedData(HolderLookup.Provider provider, CompoundTag tag, boolean forDrop) {
        getMetaMachine().saveCustomPersistedData(provider, tag, forDrop);
    }

    @Override
    default void loadCustomPersistedData(HolderLookup.Provider provider, CompoundTag tag) {
        getMetaMachine().loadCustomPersistedData(provider, tag);
    }

    @Override
    @Nonnull
    default Component getName() {
        return Objects.requireNonNullElse(getCustomName(), self().getBlockState().getBlock().getName());
    }

    @Override
    @Nullable
    default Component getCustomName() {
        return getMetaMachine().getCustomName();
    }
}

package com.lowdragmc.mbd2.api.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncBlockEntity;
import com.lowdragmc.mbd2.MBD2;
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

    /**
     * How far ahead this machine's {@link #getOffsetTimer()} runs, so that machines placed on the
     * same tick don't all do their periodic work on the same tick.
     *
     * <p>Spreading phase is the entire job, so the range only has to cover the longest period
     * anything divides the timer by — a minute is well past that. It used to be a full
     * {@code nextLong()}, which spreads phase just as well but made the timer itself a number around
     * 4e18, and that is a bad number to hand out. It overflows the moment the world time is added to
     * it, and it cannot survive a trip through a {@code float}: a float carries 24 bits of mantissa,
     * so up there a tick and the tick after it are the same float. KilaGraph's arithmetic nodes take
     * their numeric lane from their operands and so keep a {@code long} exact, but the nodes that
     * are genuinely float — {@code Lerp}, {@code Remap}, the trig ones, anything feeding a renderer —
     * cannot, and a timer they cannot tell apart from the next one stops moving without saying so.
     * A small offset keeps it exact in a float for the first ~16.7M ticks of world time.</p>
     */
    static long randomTickOffset() {
        return MBD2.RND.nextInt(20 * 60);
    }

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

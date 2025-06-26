package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface IMultiController extends IBlockEntityOwner {

    static Optional<IMultiController> ofController(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MULTI_CONTROLLER).resolve();
    }

    static Optional<IMultiController> ofController(@Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return ofController(level.getBlockEntity(pos));
    }

    /**
     * Get the front facing of the controller.
     */
    Optional<Direction> getFrontFacing();

    /**
     * Whether it has front face.
     * @return false: structure of all sides are available.
     */
    default boolean hasFrontFacing() {
        return getFrontFacing().isPresent();
    }

    /**
     * should add part to the part list.
     */
    default boolean shouldAddPartToController(IMultiPart part) {
        return true;
    }

    /**
     * Check MultiBlock Pattern. Just checking pattern without any other logic.
     * You can override it, but it's unsafe for calling. because it will also be called in an async thread.
     * <br>
     * you should always use {@link IMultiControllerMachine#checkPatternWithLock()} and {@link IMultiControllerMachine#checkPatternWithTryLock()} instead.
     * @return whether it can be formed.
     */
    default boolean checkPattern() {
        BlockPattern pattern = getPattern();
        return pattern != null && pattern.checkPatternAt(getMultiblockState(), false);
    }

    /**
     * Check pattern with a lock.
     */
    default boolean checkPatternWithLock() {
        var lock = getPatternLock();
        lock.lock();
        try {
            return checkPattern();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check pattern with a try lock
     * @return false - checking failed or cant get the lock.
     */
    default boolean checkPatternWithTryLock() {
        var lock = getPatternLock();
        if (lock.tryLock()) {
            try {
                return checkPattern();
            } finally {
                lock.unlock();
            }
        } else {
            return false;
        }
    }

    /**
     * Get structure pattern.
     * You can override it to create dynamic patterns.
     */
    BlockPattern getPattern();

    /**
     * Whether Multiblock Formed.
     * <br>
     * NOTE: even machine is formed, it doesn't mean to workable!
     * Its parts maybe invalid due to chunk unload.
     * <br>
     * use {@link #isFormedValid()} to check workable.
     */
    boolean isFormed();

    /**
     * Whether the structure is totally valid and workable.
     */
    boolean isFormedValid();

    /**
     * Get MultiblockState. It records all structure-related information.
     */
    @Nonnull
    MultiblockState getMultiblockState();

    /**
     * Called in an async thread. It's unsafe, Don't modify anything of world but checking information.
     * It will be called per 5 tick.
     * <br>
     * to implement it, you should
     * <br>
     * - call {@link MultiblockWorldSavedData#addAsyncLogic(IMultiController)} in {@link IMultiControllerMachine#onLoad()}
     * <br>
     * - call {@link MultiblockWorldSavedData#removeAsyncLogic(IMultiController)} in {@link IMultiControllerMachine#onUnload()}
     * @param periodID period Tick
     */
    default void asyncCheckPattern(long periodID) {
        if ((getMultiblockState().hasError() || !isFormed()) && (getOffset() + periodID) % 4 == 0 && checkPatternWithTryLock()) { // per second
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    var lock = getPatternLock();
                    lock.lock();
                    try {
                        if (checkPattern()) { // formed
                            onStructureFormed();
                            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                            mwsd.addMapping(getMultiblockState());
                            mwsd.removeAsyncLogic(this);
                        }
                    } finally {
                        lock.unlock();
                    }
                });
            }
        }
    }

    /**
     * Called when structure is formed, have to be called after {@link #checkPattern()}. (server-side / fake scene only)
     * <br>
     * Trigger points:
     * <br>
     * 1 - Blocks in structure changed but still formed.
     * <br>
     * 2 - Literally, structure formed.
     */
    void onStructureFormed();

    /**
     * Called when structure is invalid. (server-side / fake scene only)
     * <br>
     * Trigger points:
     * <br>
     * 1 - Blocks in structure changed.
     * <br>
     * 2 - Before controller machine removed.
     */
    default void onStructureInvalid() {
        onStructureInvalid(false);
    }

    /**
     * Called when structure is invalid. (server-side / fake scene only)
     * <br>
     * Trigger points:
     * <br>
     * 1 - Blocks in structure changed.
     * <br>
     * 2 - Before controller machine removed.
     */
    void onStructureInvalid(boolean isControllerRemoved);

    /**
     * Get all parts
     */
    List<IMultiPart> getParts();

    /**
     * Called from part, when part is invalid due to chunk unload or broken.
     */
    void onPartUnload();

    /**
     * Get lock for async pattern checking.
     */
    Lock getPatternLock();

    /**
     * Whether we check the multi pattern in an async thread?
     */
    default boolean supportAsyncPatternChecking() {
        return true;
    }
}

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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface IMultiController extends IMachine {

    static Optional<IMultiController> ofController(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve()
                .filter(IMultiController.class::isInstance)
                .map(IMultiController.class::cast);
    }

    static Optional<IMultiController> ofController(@NotNull BlockGetter level, @NotNull BlockPos pos) {
        return ofController(level.getBlockEntity(pos));
    }

    /**
     * Check MultiBlock Pattern. Just checking pattern without any other logic.
     * You can override it but it's unsafe for calling. because it will also be called in an async thread.
     * <br>
     * you should always use {@link IMultiController#checkPatternWithLock()} and {@link IMultiController#checkPatternWithTryLock()} instead.
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
        var result = checkPattern();
        lock.unlock();
        return result;
    }

    /**
     * Check pattern with a try lock
     * @return false - checking failed or cant get the lock.
     */
    default boolean checkPatternWithTryLock() {
        var lock = getPatternLock();
        if (lock.tryLock()) {
            var result = checkPattern();
            lock.unlock();
            return result;
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
     */
    boolean isFormed();


    /**
     * Get MultiblockState. It records all structure-related information.
     */
    @NotNull
    MultiblockState getMultiblockState();

    /**
     * Called in an async thread. It's unsafe, Don't modify anything of world but checking information.
     * It will be called per 5 tick.
     * <br>
     * to implement it, you should
     * <br>
     * - call {@link MultiblockWorldSavedData#addAsyncLogic(IMultiController)} in {@link IMultiController#onLoad()}
     * <br>
     * - call {@link MultiblockWorldSavedData#removeAsyncLogic(IMultiController)} in {@link IMultiController#onUnload()}
     * @param periodID period Tick
     */
    default void asyncCheckPattern(long periodID) {
        if ((getMultiblockState().hasError() || !isFormed()) && (getOffset() + periodID) % 4 == 0 && checkPatternWithTryLock()) { // per second
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    getPatternLock().lock();
                    if (checkPatternWithLock()) { // formed
                        onStructureFormed();
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.addMapping(getMultiblockState());
                        mwsd.removeAsyncLogic(this);
                    }
                    getPatternLock().unlock();
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
    void onStructureInvalid();

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
     * should add part to the part list.
     */
    default boolean shouldAddPartToController(IMultiPart part) {
        return true;
    }

    /**
     * get parts' Appearance. same as IForgeBlock.getAppearance() / IFabricBlock.getAppearance()
     */
    @Nullable
    default BlockState getPartAppearance(IMultiPart part, Direction side, BlockState sourceState, BlockPos sourcePos) {
        return null;
    }
}

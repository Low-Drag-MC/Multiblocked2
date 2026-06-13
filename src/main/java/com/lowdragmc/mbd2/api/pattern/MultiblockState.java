package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.error.PatternError;
import com.lowdragmc.mbd2.api.pattern.error.PatternStringError;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.api.pattern.snapshot.PatternSnapshot;
import com.lowdragmc.mbd2.api.pattern.snapshot.SnapshotEntry;
import com.lowdragmc.mbd2.api.pattern.util.PatternMatchContext;
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.block.Blocks;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiblockState {
    public final static PatternError UNLOAD_ERROR = new PatternStringError("mbd2.multiblock.pattern.error.chunk");
    public final static PatternError UNINIT_ERROR = new PatternStringError("mbd2.multiblock.pattern.error.init");

    private BlockPos pos;
    private BlockState blockState;
    private BlockEntity tileEntity;
    private boolean tileEntityInitialized;
    @Getter
    private final PatternMatchContext matchContext;
    @Getter
    private Map<PatternPredicate, Integer> globalCount;
    @Getter
    private Map<PatternPredicate, Integer> layerCount;
    public TraceabilityPredicate predicate;
    public IO io;
    public PatternError error;
    @Getter
    public final Level world;
    public final BlockPos controllerPos;
    public IMultiController lastController;
    @Getter
    private boolean isInternalStructureForming;
    @Getter
    private boolean isInternalStructureInvaliding;
    /** Controller facing for the active checkPatternAt pass; null when not inside a pattern check. */
    @Getter
    @Nullable
    private Direction checkingFacing;
    /** Toggled by {@link PatternPredicate} when its {@code rotateFollowController} flag is set, so
     *  {@link #getBlockState()} returns the canonical (NORTH-frame) view to predicate lambdas. */
    private boolean rotationActive;
    /** When non-null, BlockState / BE-data reads come from this snapshot instead of the live world.
     *  Set by the async pattern check pass; null on main-thread checks. */
    @Nullable
    private PatternSnapshot snapshot;
    @Getter
    @Nullable
    private BlockPattern checkingPattern;
    private final Long2IntOpenHashMap matchedPredicateIds = new Long2IntOpenHashMap();

    // persist
    public LongOpenHashSet cache = new LongOpenHashSet();

    public MultiblockState(Level world, BlockPos controllerPos) {
        this.world = world;
        this.controllerPos = controllerPos;
        this.error = UNINIT_ERROR;
        this.matchContext = new PatternMatchContext();
        this.matchedPredicateIds.defaultReturnValue(-1);
    }

    protected void clean() {
        this.matchContext.reset();
        this.globalCount = new HashMap<>();
        this.layerCount = new HashMap<>();
        cache = new LongOpenHashSet();
    }

    public void setCheckingPattern(@Nullable BlockPattern checkingPattern) {
        this.checkingPattern = checkingPattern;
    }

    public void commitMatchedPredicateIds(@Nullable Long2IntMap matches) {
        matchedPredicateIds.clear();
        if (matches != null) {
            matchedPredicateIds.putAll(matches);
        }
    }

    public void clearMatchedPredicateIds() {
        matchedPredicateIds.clear();
    }

    public int getMatchedPredicateId(BlockPos pos) {
        return matchedPredicateIds.get(pos.asLong());
    }

    protected boolean update(BlockPos posIn, TraceabilityPredicate predicate) {
        this.pos = posIn;
        this.blockState = null;
        this.tileEntity = null;
        this.tileEntityInitialized = false;
        this.predicate = predicate;
        this.error = null;
        if (snapshot != null) {
            // Snapshot mode is authoritative; tracked positions were verified loaded at capture time.
            return true;
        }
        if (!world.isLoaded(posIn)) {
            error = UNLOAD_ERROR;
            return false;
        }
        return true;
    }

    public IMultiController getController() {
        if (snapshot != null) {
            return lastController = snapshot.getOwner();
        }
        if (world.isLoaded(controllerPos)) {
            var machineOptional = IMachine.ofMachine(world, controllerPos);
            if (machineOptional.isPresent() && machineOptional.get() instanceof IMultiController controller) {
                return lastController = controller;
            }
        } else {
            error = UNLOAD_ERROR;
        }
        return null;
    }

    public boolean hasError() {
        return error != null;
    }

    public void setError(PatternError error) {
        this.error = error;
        if (error != null) {
            error.setWorldState(this);
        }
    }

    public BlockState getBlockState() {
        if (this.blockState == null) {
            this.blockState = readBlockStateRaw();
        }
        if (rotationActive && checkingFacing != null && checkingFacing != Direction.NORTH) {
            return this.blockState.rotate(RotationHelper.inverse(RotationHelper.rotationFromFacing(checkingFacing)));
        }
        return this.blockState;
    }

    /** Raw world state, ignoring any active rotation context. Used by inner condition checks
     *  (NBT, slot names) that should always see the on-disk state. */
    public BlockState getRawBlockState() {
        if (this.blockState == null) {
            this.blockState = readBlockStateRaw();
        }
        return this.blockState;
    }

    private BlockState readBlockStateRaw() {
        if (snapshot != null) {
            SnapshotEntry entry = snapshot.getEntry(this.pos);
            return entry != null ? entry.state() : Blocks.AIR.defaultBlockState();
        }
        return this.world.getBlockState(this.pos);
    }

    @Nullable
    public BlockEntity getTileEntity() {
        if (snapshot != null) {
            // Snapshot/async mode: live BE access is not safe. Use getTileEntityData() instead.
            return null;
        }
        if (!getRawBlockState().hasBlockEntity()) {
            return null;
        }
        if (this.tileEntity == null && !this.tileEntityInitialized) {
            this.tileEntity = this.world.getBlockEntity(this.pos);
            this.tileEntityInitialized = true;
        }

        return this.tileEntity;
    }

    /** @return the saved NBT of the BE at the current pos, or null if there is no BE. */
    @Nullable
    public CompoundTag getTileEntityData() {
        if (snapshot != null) {
            SnapshotEntry entry = snapshot.getEntry(this.pos);
            return entry == null ? null : entry.beData();
        }
        var te = getTileEntity();
        return te == null ? null : te.saveWithFullMetadata(world.registryAccess());
    }

    /** @return the saved NBT of the controller's BE, or null if the controller is absent. */
    @Nullable
    public CompoundTag getControllerTileEntityData() {
        if (snapshot != null) {
            SnapshotEntry entry = snapshot.getEntry(controllerPos);
            return entry == null ? null : entry.beData();
        }
        var controller = getController();
        if (controller == null) return null;
        var holder = controller.getHolder();
        return holder == null ? null : holder.saveWithFullMetadata(world.registryAccess());
    }

    public void setCheckingFacing(@Nullable Direction facing) {
        this.checkingFacing = facing;
    }

    /** Bind this state to a snapshot. While set, all world reads route through the snapshot. */
    public void setSnapshot(@Nullable PatternSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Nullable
    public PatternSnapshot getSnapshot() {
        return snapshot;
    }

    /** Push the rotation-active flag; returns the previous value so the caller can restore. */
    public boolean pushRotationActive(boolean active) {
        boolean prev = this.rotationActive;
        this.rotationActive = active;
        return prev;
    }

    public void popRotationActive(boolean previous) {
        this.rotationActive = previous;
    }

    public BlockPos getPos() {
        return this.pos.immutable();
    }

    public void addPosCache(BlockPos pos) {
        cache.add(pos.asLong());
    }

    public boolean isPosInCache(BlockPos pos) {
        return cache.contains(pos.asLong());
    }

    public Collection<BlockPos> getCache() {
        return cache.stream().map(BlockPos::of).collect(Collectors.toList());
    }

    public void runInternalStructureInvaliding(Runnable action) {
        boolean previous = isInternalStructureInvaliding;
        isInternalStructureInvaliding = true;
        try {
            action.run();
        } finally {
            isInternalStructureInvaliding = previous;
        }
    }

    public void onBlockStateChanged(BlockPos pos, BlockState state) {
        if (world instanceof ServerLevel serverLevel) {
            if (pos.equals(controllerPos)) {
                if (lastController != null) {
                    if (!state.is(lastController.getBlockState().getBlock())) {
                        if (!isInternalStructureInvaliding) {
                            lastController.onStructureInvalid(true);
                            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                            mwsd.removeMapping(this);
                        }
                    }
                }
            } else if (state.getBlock() == ProxyPartBlock.BLOCK) {
                // ignore if it's a proxy part block
            } else {
                if (isInternalStructureForming || isInternalStructureInvaliding) {
                    // ignore if it's internal structure forming or invaliding
                    return;
                }
                IMultiController controller = getController();
                if (controller != null) {
                    // TODO vaBlocks
//                    if (controller.isFormed() && state.getBlock() instanceof ActiveBlock) {
//                        LongSet activeBlocks = getMatchContext().getOrDefault("vaBlocks", LongSets.emptySet());
//                        if (activeBlocks.contains(pos.asLong())) {
//                            // fine! it's caused by active blocks.
//                            // speed up here!
//                            return;
//                        }
//                    }
                    if (controller.checkPatternWithLock()) {
                        // refresh structure
                        isInternalStructureForming = true;
                        controller.onStructureFormed();
                        isInternalStructureForming = false;
                    } else {
                        isInternalStructureInvaliding = true;
                        // invalid structure
                        controller.onStructureInvalid();
                        isInternalStructureInvaliding = false;
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.removeMapping(this);
                        mwsd.addAsyncLogic(controller);
                    }
                }
            }
        }
    }

}

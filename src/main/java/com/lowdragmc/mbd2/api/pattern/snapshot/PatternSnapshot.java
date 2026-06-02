package com.lowdragmc.mbd2.api.pattern.snapshot;

import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Main-thread-captured cache of {@link BlockState}/BE-NBT for the area covered by a
 * multiblock controller's pattern. The async pattern checker reads only from here, so it
 * never touches the live world. The main-thread tick captures up to
 * {@link #CAPTURES_PER_TICK} positions per tick from {@link #pendingPositions}.
 * <p>
 * All mutations and reads must hold {@code synchronized(this)} so the executor thread
 * sees a consistent view while reading {@link #entries}.
 */
public class PatternSnapshot {

    public static final int CAPTURES_PER_TICK = 20;

    @Getter private final IMultiController owner;
    @Getter private final ServerLevel level;
    /** {@link BlockPattern} identity at snapshot creation. If the controller hands back a
     *  different instance, the snapshot is rebuilt. */
    @Getter private final BlockPattern patternRef;

    /** Positions whose predicate is NOT {@code isAny()} — i.e. where the actual block matters. */
    @Getter private final LongOpenHashSet trackedPositions;
    /** Subset of {@link #trackedPositions} where some predicate has a non-empty {@code nbt}
     *  config — these positions cache BE NBT and also subscribe to {@code BlockEntity.setChanged}. */
    @Getter private final LongOpenHashSet nbtSensitivePositions;
    /** Captured state per pos. May be missing entries until {@link #fullyBuilt}. */
    private final Long2ObjectOpenHashMap<SnapshotEntry> entries = new Long2ObjectOpenHashMap<>();
    /** Positions awaiting (re)capture. Drained at up to {@link #CAPTURES_PER_TICK} per tick. */
    private final LongOpenHashSet pendingPositions = new LongOpenHashSet();
    /** True once every tracked position has been captured at least once. */
    private volatile boolean fullyBuilt;
    /** Bumped on every batch of captures; lets async readers detect stale views. */
    private volatile long version;
    /** True iff the snapshot has new data since the last async pattern check consumed it.
     *  Starts true (no check has run yet). {@link #markDirty} re-sets it. The executor
     *  consumes via {@link #tryConsumeDirtyForCheck} — no consume, no work. */
    private volatile boolean dirtyForCheck = true;
    /** True iff a worker task for this snapshot has been submitted but hasn't finished yet.
     *  Used by {@code MultiblockWorldSavedData.dispatchPendingChecks} to coalesce: a frequently-
     *  changing world (e.g. a redstone block ticking next to the controller) marks dirty every
     *  tick, but at most one worker task is in flight per snapshot. When the in-flight task
     *  finishes it checks the dirty flag again, re-submitting only if more changes accumulated. */
    private volatile boolean checkInFlight;

    public PatternSnapshot(IMultiController owner, ServerLevel level, BlockPattern patternRef,
                           LongOpenHashSet trackedPositions, LongOpenHashSet nbtSensitivePositions) {
        this.owner = owner;
        this.level = level;
        this.patternRef = patternRef;
        this.trackedPositions = trackedPositions;
        this.nbtSensitivePositions = nbtSensitivePositions;
        this.pendingPositions.addAll(trackedPositions);
    }

    public boolean isFullyBuilt() {
        return fullyBuilt;
    }

    public long getVersion() {
        return version;
    }

    public synchronized int trackedSize() {
        return trackedPositions.size();
    }

    public synchronized int capturedSize() {
        return entries.size();
    }

    public synchronized int pendingSize() {
        return pendingPositions.size();
    }

    /** @return whether {@code posLong} is in this snapshot's bbox. */
    public boolean contains(long posLong) {
        return trackedPositions.contains(posLong);
    }

    /** @return whether {@code posLong} is an NBT-sensitive position. */
    public boolean isNbtSensitive(long posLong) {
        return nbtSensitivePositions.contains(posLong);
    }

    @Nullable
    public synchronized SnapshotEntry getEntry(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    /** Mark {@code posLong} as needing re-capture. No-op if the position is outside the bbox. */
    public synchronized void markDirty(long posLong) {
        if (!trackedPositions.contains(posLong)) return;
        pendingPositions.add(posLong);
        fullyBuilt = false;
        dirtyForCheck = true;
    }

    /** Mark the snapshot as needing an async check, without changing what's captured. Use this
     *  when something outside the captured world state has changed (e.g. the controller's
     *  pattern was reconfigured but the cached instance still matches). */
    public synchronized void markDirtyForCheck() {
        dirtyForCheck = true;
    }

    /** @return true iff the dirty-for-check flag was set; clears it. The worker uses this so
     *  it only spends time on a snapshot when something has actually changed since the last
     *  pass. */
    public synchronized boolean tryConsumeDirtyForCheck() {
        if (dirtyForCheck) {
            dirtyForCheck = false;
            return true;
        }
        return false;
    }

    /** Non-clearing read of the dirty flag. Used by the main-thread post-check to detect
     *  whether world state changed during the async pass — if so, the just-completed check is
     *  stale and the form attempt should be skipped (next dispatch will retry). */
    public boolean isDirtyForCheck() {
        return dirtyForCheck;
    }

    /** Test-and-set the {@code checkInFlight} flag. @return true if no task was in flight (the
     *  caller now owns the slot and may submit); false otherwise. */
    public synchronized boolean tryClaimCheckSlot() {
        if (checkInFlight) return false;
        checkInFlight = true;
        return true;
    }

    /** Release the in-flight slot. Called by the worker after its task completes. */
    public synchronized void releaseCheckSlot() {
        checkInFlight = false;
    }

    public boolean isCheckInFlight() {
        return checkInFlight;
    }

    /**
     * Capture up to {@link #CAPTURES_PER_TICK} pending positions from the live world. Called
     * on the main server thread.
     * @return true if {@link #fullyBuilt} flipped true during this call (i.e. the snapshot is
     *         now ready for an async check pass).
     */
    public synchronized boolean tickCapture() {
        if (pendingPositions.isEmpty()) {
            if (!fullyBuilt) {
                fullyBuilt = true;
                version++;
                return true;
            }
            return false;
        }
        int budget = CAPTURES_PER_TICK;
        LongIterator it = pendingPositions.iterator();
        Set<Long> done = new HashSet<>();
        while (it.hasNext() && budget > 0) {
            long posLong = it.nextLong();
            BlockPos pos = BlockPos.of(posLong);
            if (!level.isLoaded(pos)) {
                // Leave it pending; try again next tick when the chunk loads.
                continue;
            }
            BlockState state = level.getBlockState(pos);
            CompoundTag beData = null;
            if (nbtSensitivePositions.contains(posLong)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    beData = be.saveWithFullMetadata(level.registryAccess());
                }
            }
            entries.put(posLong, new SnapshotEntry(state == null ? Blocks.AIR.defaultBlockState() : state, beData));
            done.add(posLong);
            budget--;
        }
        for (long pl : done) pendingPositions.remove(pl);
        version++;
        boolean nowBuilt = pendingPositions.isEmpty();
        if (nowBuilt && !fullyBuilt) {
            fullyBuilt = true;
            return true;
        }
        return false;
    }

}

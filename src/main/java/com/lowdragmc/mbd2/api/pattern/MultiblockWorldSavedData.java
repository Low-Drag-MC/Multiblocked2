package com.lowdragmc.mbd2.api.pattern;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.snapshot.PatternSnapshot;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MultiblockWorldSavedData extends SavedData {
    @Getter
    private final ServerLevel serverLevel;
    public static MultiblockWorldSavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(new Factory<>(() -> new MultiblockWorldSavedData(serverLevel),
                (tag, provider) -> new MultiblockWorldSavedData(serverLevel, tag, provider)),
                "MBD2_multiblock");
    }

    /**
     * Store all formed multiblocks' structure info
     */
    public final Map<BlockPos, MultiblockState> mapping;
    /**
     * Structure Cache pos mapping.
     */
    public final Long2ObjectOpenHashMap<Set<MultiblockState>> structureCachePosMapping;

    /**
     * Per-controller world snapshots used by the async pattern checker. The snapshot is the
     * single source of truth for the async pass; the executor never reads from the live world.
     * {@link java.util.concurrent.ConcurrentHashMap} so worker thread can safely read while
     * main thread writes.
     */
    private final Map<IMultiController, PatternSnapshot> snapshots = new ConcurrentHashMap<>();
    /**
     * Pos → snapshots whose {@code trackedPositions} include that pos. Mixins use this to find
     * which snapshots need re-capture in O(1) when a tracked block or BE changes.
     */
    private final Long2ObjectOpenHashMap<Set<PatternSnapshot>> snapshotPosIndex = new Long2ObjectOpenHashMap<>();

    private MultiblockWorldSavedData(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
        this.mapping = new Object2ObjectOpenHashMap<>();
        this.structureCachePosMapping = new Long2ObjectOpenHashMap<>();
    }

    private MultiblockWorldSavedData(ServerLevel serverLevel, CompoundTag tag, HolderLookup.Provider provider) {
        this(serverLevel);
    }

    public MultiblockState[] getControllerInPos(BlockPos pos) {
        return structureCachePosMapping.getOrDefault(pos.asLong(), Collections.emptySet()).toArray(MultiblockState[]::new);
    }

    public void addMapping(MultiblockState state) {
        this.mapping.put(state.controllerPos, state);
        for (var blockPos : state.getCache()) {
            structureCachePosMapping.computeIfAbsent(blockPos.asLong(), c-> new HashSet<>()).add(state);
        }
    }

    public void removeMapping(MultiblockState state) {
        this.mapping.remove(state.controllerPos);
        var iterator = structureCachePosMapping.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var stateSet = entry.getValue();
            stateSet.remove(state);
            if (stateSet.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag compound, @Nonnull HolderLookup.Provider provider) {
        return compound;
    }

    // ********************************* snapshot tracking ********************************* //
    /** Tracked controllers. Used only for legacy {@link #periodID} accounting; the actual
     *  check dispatch iterates {@link #snapshots} directly. */
    private final CopyOnWriteArrayList<IMultiController> controllers = new CopyOnWriteArrayList<>();
    /** Legacy counter, retained because some downstream code reads it via {@link #getPeriodID}. */
    @Getter
    private long periodID = Long.MIN_VALUE;

    /** Single worker thread that runs snapshot-backed pattern checks off the main thread.
     *  Tasks are submitted only when a snapshot becomes ready + dirty (event-driven, never
     *  polled). On a successful match the worker posts back to the main thread for the live
     *  re-check + form. Lazily created on first use. */
    @Nullable
    private ExecutorService asyncChecker;
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("MBD2 Multiblock Async Thread-%d")
            .setDaemon(true)
            .build();
    private static final ThreadLocal<Boolean> IN_SERVICE = ThreadLocal.withInitial(() -> false);

    private synchronized ExecutorService asyncChecker() {
        if (asyncChecker == null || asyncChecker.isShutdown()) {
            asyncChecker = Executors.newSingleThreadExecutor(THREAD_FACTORY);
        }
        return asyncChecker;
    }

    /** Legacy entry point preserved for binary compat; the worker is now started lazily on
     *  first dispatch, so this is a no-op. */
    public void createExecutorService() {
        // intentionally empty
    }

    /** Shut the async worker down. Called on level unload + server stop. {@link
     *  ExecutorService#shutdown} (not {@code shutdownNow}) lets the in-flight task — if any —
     *  release its locks normally before the thread exits. */
    public synchronized void releaseExecutorService() {
        if (asyncChecker != null) {
            asyncChecker.shutdown();
            try {
                asyncChecker.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            asyncChecker = null;
        }
    }

    /** Register a controller for snapshot-based pattern detection. Creates (or reuses) the
     *  controller's {@link PatternSnapshot} and registers it in {@link #snapshotPosIndex}. */
    public void addAsyncLogic(IMultiController controller) {
        if (controller instanceof MBDMultiblockMachine machine) {
            // catalyst-required machines don't auto-form, so they don't need snapshot tracking.
            if (machine.getDefinition().multiblockSettings().catalyst().isEnable()) return;
        }
        ensureSnapshot(controller, false);
        if (!controllers.contains(controller)) {
            controllers.add(controller);
        }
    }

    /** Unregister a controller from snapshot tracking. Removes the snapshot and clears its
     *  entries from {@link #snapshotPosIndex}. */
    public void removeAsyncLogic(IMultiController controller) {
        controllers.remove(controller);
        removeSnapshot(controller);
    }

    /** Ensure the controller has a current {@link PatternSnapshot}. Rebuilds when the
     *  controller's {@link BlockPattern} instance differs from the cached one, or when
     *  {@code forceRebuild} is set (used by {@link #notifyPatternDirty} for in-place pattern
     *  mutations that keep the same instance). Holds the MWSD monitor for the duration of
     *  {@link #snapshotPosIndex} mutations — any reader of that index must take the same monitor. */
    private synchronized void ensureSnapshot(IMultiController controller, boolean forceRebuild) {
        BlockPattern pattern = controller.getPattern();
        if (pattern == null) return;
        PatternSnapshot existing = snapshots.get(controller);
        if (!forceRebuild && existing != null && existing.getPatternRef() == pattern) return;
        if (existing != null) {
            unregisterFromIndex(existing);
            // Do NOT call existing.clear() — a worker thread may be mid-check holding the snapshot
            // monitor. Just drop the map reference; the old snapshot will be GC'd once the
            // worker finishes. Its data is immutable from the worker's perspective.
        }
        BlockPos centerPos = controller.getPos();
        LongOpenHashSet tracked = new LongOpenHashSet();
        LongOpenHashSet nbtSensitive = new LongOpenHashSet();
        Direction[] facings = controller.hasFrontFacing()
                ? new Direction[]{controller.getFrontFacing().orElseThrow()}
                : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
        for (Direction facing : facings) {
            tracked.addAll(pattern.collectTrackedPositions(centerPos, facing));
            nbtSensitive.addAll(pattern.collectNbtSensitivePositions(centerPos, facing));
        }
        PatternSnapshot snapshot = new PatternSnapshot(controller, serverLevel, pattern, tracked, nbtSensitive);
        snapshots.put(controller, snapshot);
        for (long posLong : tracked) {
            snapshotPosIndex.computeIfAbsent(posLong, k -> new HashSet<>()).add(snapshot);
        }
    }

    private synchronized void removeSnapshot(IMultiController controller) {
        PatternSnapshot snapshot = snapshots.remove(controller);
        if (snapshot == null) return;
        unregisterFromIndex(snapshot);
        // Same rationale as ensureSnapshot: don't clear() — a worker may still hold the monitor.
    }

    private void unregisterFromIndex(PatternSnapshot snapshot) {
        var iterator = snapshotPosIndex.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var set = entry.getValue();
            set.remove(snapshot);
            if (set.isEmpty()) iterator.remove();
        }
    }

    @Nullable
    public PatternSnapshot getSnapshot(IMultiController controller) {
        return snapshots.get(controller);
    }

    /** Mark every snapshot containing {@code pos} as needing a re-capture at that pos. Called
     *  from {@code ChunkMixin} on every BlockState change. */
    public void markPositionDirty(BlockPos pos) {
        var set = snapshotPosIndex.get(pos.asLong());
        if (set == null || set.isEmpty()) return;
        long posLong = pos.asLong();
        for (PatternSnapshot snapshot : set.toArray(new PatternSnapshot[0])) {
            snapshot.markDirty(posLong);
        }
    }

    /** Like {@link #markPositionDirty}, but only triggers re-capture for snapshots that flagged
     *  the pos as NBT-sensitive (i.e. there's at least one predicate with non-empty {@code nbt}
     *  watching that pos). Called from {@code BlockEntityMixin#setChanged}. */
    public void markBlockEntityChanged(BlockPos pos) {
        var set = snapshotPosIndex.get(pos.asLong());
        if (set == null || set.isEmpty()) return;
        long posLong = pos.asLong();
        for (PatternSnapshot snapshot : set.toArray(new PatternSnapshot[0])) {
            if (snapshot.isNbtSensitive(posLong)) {
                snapshot.markDirty(posLong);
            }
        }
    }

    /** Capture pending positions for every snapshot. Called from the server tick. Cheap;
     *  drains up to {@link PatternSnapshot#CAPTURES_PER_TICK} pending positions per snapshot. */
    public void tickSnapshots() {
        if (snapshots.isEmpty()) return;
        for (PatternSnapshot snapshot : snapshots.values()) {
            snapshot.tickCapture();
        }
    }

    /** For every fully-built snapshot whose state has changed since the previous check, submit
     *  one snapshot-backed check task to the worker thread. Tasks are submitted only when
     *  there's actual work to do; no polling. Coalesces under load: a snapshot with a task
     *  already in flight does not submit again — the in-flight task picks up the latest dirty
     *  marks when it finishes (see {@link #runAsyncCheck}). This keeps the worker queue bounded
     *  even if a block in the structure changes every tick. */
    public void dispatchPendingChecks() {
        if (snapshots.isEmpty()) return;
        for (Map.Entry<IMultiController, PatternSnapshot> entry : snapshots.entrySet()) {
            PatternSnapshot snapshot = entry.getValue();
            if (!snapshot.isFullyBuilt() || !snapshot.tryConsumeDirtyForCheck()) continue;
            if (!snapshot.tryClaimCheckSlot()) {
                // A previous task is still running. Restore the dirty flag so when that task
                // finishes (or the next dispatch fires after release), the new changes get a check.
                snapshot.markDirtyForCheck();
                continue;
            }
            IMultiController controller = entry.getKey();
            if (!trySubmitCheck(controller, snapshot)) {
                // Executor shut down between claim and submit; release the slot + restore dirty.
                snapshot.releaseCheckSlot();
                snapshot.markDirtyForCheck();
            }
        }
    }

    /** @return false if the task could not be submitted (executor shut down mid-call). */
    private boolean trySubmitCheck(IMultiController controller, PatternSnapshot snapshot) {
        ExecutorService executor = asyncChecker();
        try {
            executor.submit(() -> runAsyncCheck(controller, snapshot));
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    /** Worker-thread entry point. Runs the snapshot-backed check (in-memory; no live world
     *  reads). On a match, schedules the live re-check + form back onto the main thread.
     *  Releases the snapshot's in-flight slot at the end so the next dispatch can submit
     *  another task. */
    private void runAsyncCheck(IMultiController controller, PatternSnapshot snapshot) {
        IN_SERVICE.set(true);
        try {
            // The snapshot may have been disposed (controller unloaded) between submit and run.
            if (snapshots.get(controller) != snapshot) return;
            // Likewise the worker may have been queued while the controller already formed.
            if (!controller.getMultiblockState().hasError() && controller.isFormed()) return;
            var lock = controller.getPatternLock();
            if (!lock.tryLock()) {
                // Main thread is holding the pattern lock (likely doing its own checkPatternWithLock).
                // Re-mark dirty so the next dispatch retries — otherwise we'd permanently skip the
                // check after a single transient contention.
                snapshot.markDirtyForCheck();
                return;
            }
            boolean matched;
            try {
                matched = runSnapshotCheckLocked(controller, snapshot);
            } finally {
                lock.unlock();
            }
            if (matched) {
                var server = serverLevel.getServer();
                if (server != null) {
                    server.execute(() -> tryFormOnMain(controller));
                }
            }
        } catch (Throwable e) {
            MBD2.LOGGER.error("snapshot check failed for {}: {}", controller.getPos(), e.getMessage());
        } finally {
            IN_SERVICE.set(false);
            // Free the in-flight slot so the next tick's dispatch can submit again. Must be the
            // very last action — releasing earlier opens a window where two tasks could overlap.
            snapshot.releaseCheckSlot();
        }
    }

    /** Caller must hold {@code controller.getPatternLock()}. Individual snapshot reads are
     *  synchronized inside {@link PatternSnapshot#getEntry}, so concurrent {@code tickCapture}
     *  or {@code markDirty} on the same snapshot won't tear an individual lookup. Across the
     *  full {@code checkPatternAt} pass the snapshot view may shift (a position re-captured
     *  mid-check), but only positions tagged for re-capture have new data, and any change to a
     *  tracked position also sets {@code dirtyForCheck} — so {@link #tryFormOnMain} will see
     *  the dirty flag and skip the form, the next dispatch reruns with a coherent view.
     *  <p>
     *  We do NOT hold {@code synchronized(snapshot)} for the whole check, because {@code
     *  markDirty} runs from ChunkMixin on the main server thread on every block change — a
     *  worker holding the monitor would block the server thread for every nearby block edit. */
    private boolean runSnapshotCheckLocked(IMultiController controller, PatternSnapshot snapshot) {
        var state = controller.getMultiblockState();
        state.setSnapshot(snapshot);
        try {
            BlockPattern pattern = snapshot.getPatternRef();
            return pattern != null && pattern.checkPatternAt(state, false);
        } finally {
            state.setSnapshot(null);
        }
    }

    /** Notify that the controller's pattern shape changed — call this whenever the
     *  controller's {@link BlockPattern} returns a different instance OR was mutated in place.
     *  Unconditionally rebuilds the snapshot (so {@code trackedPositions} reflects the new
     *  pattern) and re-marks dirty for the next async pass. Safe to call on the main server
     *  thread; prefer {@link IMultiController#notifyPatternDirty()} from controller code. */
    public void notifyPatternDirty(IMultiController controller) {
        if (!snapshots.containsKey(controller)) return;
        ensureSnapshot(controller, true);
        PatternSnapshot rebuilt = snapshots.get(controller);
        if (rebuilt != null) rebuilt.markDirtyForCheck();
    }

    /** Test hook: block until the async worker is idle, or {@code timeoutMillis} elapses.
     *  Implemented by submitting a sentinel task and waiting for it to complete — since the
     *  worker is single-threaded, the sentinel runs after every previously-submitted task. */
    public void awaitAsyncIdleForTests(long timeoutMillis) {
        ExecutorService executor;
        synchronized (this) {
            executor = asyncChecker;
        }
        if (executor == null || executor.isShutdown()) return;
        try {
            executor.submit(() -> {}).get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException | RejectedExecutionException e) {
            // best-effort; tests will fail downstream if state isn't ready
        }
    }

    /** Test/utility hook: run one snapshot check pass for a controller on the current thread,
     *  bypassing the dirty-gate. Used by gametests to drive deterministic check execution. */
    public void triggerInlineCheck(IMultiController controller) {
        PatternSnapshot snapshot = snapshots.get(controller);
        if (snapshot == null || !snapshot.isFullyBuilt()) return;
        snapshot.tryConsumeDirtyForCheck();
        if (!snapshot.tryClaimCheckSlot()) return; // an async task is already in flight
        try {
            if (runSnapshotCheck(controller, snapshot)) {
                tryFormOnMain(controller);
            }
        } finally {
            snapshot.releaseCheckSlot();
        }
    }

    /** @return true if the snapshot-backed pattern check matched. Acquires the controller's
     *  pattern lock; re-marks the snapshot dirty if the lock can't be acquired so the check is
     *  retried on the next dispatch. Used by {@link #triggerInlineCheck} from tests. */
    private boolean runSnapshotCheck(IMultiController controller, PatternSnapshot snapshot) {
        if (!controller.getMultiblockState().hasError() && controller.isFormed()) return false;
        var lock = controller.getPatternLock();
        if (!lock.tryLock()) {
            snapshot.markDirtyForCheck();
            return false;
        }
        try {
            return runSnapshotCheckLocked(controller, snapshot);
        } finally {
            lock.unlock();
        }
    }

    /** Main-thread formation: re-check the pattern against the live world and, on success,
     *  fire {@code onStructureFormed} and detach the controller from async tracking.
     *  <p>
     *  If the snapshot was re-marked dirty between the async pass and this callback running,
     *  skip — the next tick will re-dispatch with the fresh data and reach here again, so doing
     *  the live check now (against state that's just about to be re-examined) is wasted work.
     *  The live re-check itself is still required when we do run: the async pass populated the
     *  match context from snapshot data without live BE access, so {@code parts}, {@code ioMap},
     *  and {@code slots} are not filled — only a live {@code checkPattern} can build them, and
     *  {@code onStructureFormed} depends on them. */
    private void tryFormOnMain(IMultiController controller) {
        if (controller.isFormed()) return;
        PatternSnapshot snapshot = snapshots.get(controller);
        if (snapshot == null) return; // controller was unregistered between submit and now
        if (snapshot.isDirtyForCheck()) return; // world changed; let the next dispatch try
        var lock = controller.getPatternLock();
        lock.lock();
        try {
            if (controller.checkPattern()) {
                controller.onStructureFormed();
                addMapping(controller.getMultiblockState());
                removeAsyncLogic(controller);
            }
        } finally {
            lock.unlock();
        }
    }

    /** @return true if the current thread is running an async snapshot check. External code
     *  reads this to decide whether off-thread world access is permitted. Returns false on
     *  the main thread. */
    public static boolean isThreadService() {
        return IN_SERVICE.get();
    }

}

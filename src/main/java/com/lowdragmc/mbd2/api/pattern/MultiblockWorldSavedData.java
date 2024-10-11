package com.lowdragmc.mbd2.api.pattern;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.async.AsyncThreadData;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;

public class MultiblockWorldSavedData extends SavedData {
    @Getter
    private final ServerLevel serverLevel;
    public static MultiblockWorldSavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(tag -> new MultiblockWorldSavedData(serverLevel, tag), () -> new MultiblockWorldSavedData(serverLevel), "MBD2_multiblock");
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
     * Pos Cache of multiblock.
     */
    public final LongOpenHashSet posCache = new LongOpenHashSet();

    private MultiblockWorldSavedData(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
        this.mapping = new Object2ObjectOpenHashMap<>();
        this.structureCachePosMapping = new Long2ObjectOpenHashMap<>();
    }

    private MultiblockWorldSavedData(ServerLevel serverLevel, CompoundTag tag) {
        this(serverLevel);
    }

    public Collection<MultiblockState> getControllerInPos(BlockPos pos) {
        return structureCachePosMapping.getOrDefault(pos.asLong(), Collections.emptySet());
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
    public CompoundTag save(@Nonnull CompoundTag compound) {
        return compound;
    }

    // ********************************* thread for searching ********************************* //
    private final CopyOnWriteArrayList<IMultiController> controllers = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService executorService;
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("MBD2 Multiblock Async Thread-%d")
            .setDaemon(true)
            .build();
    private static final ThreadLocal<Boolean> IN_SERVICE = ThreadLocal.withInitial(()->false);
    @Getter
    private long periodID = Long.MIN_VALUE;

    public void createExecutorService() {
        if (executorService != null && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        executorService.scheduleAtFixedRate(this::searchingTask, 0, 250, TimeUnit.MILLISECONDS); // per 5 tick
    }

    /**
     * add a async logic runnable
     * @param controller controller
     */
    public void addAsyncLogic(IMultiController controller) {
        if (controller instanceof MBDMultiblockMachine machine) {
            // if it requires catalyst, don't add it to async logic.
            if (machine.getDefinition().multiblockSettings().catalyst().isEnable()) return;
        }
        controllers.add(controller);
        createExecutorService();
    }

    /**
     * remove async controller
     * @param controller controller
     */
    public void removeAsyncLogic(IMultiController controller) {
        if (controllers.contains(controller)) {
            controllers.remove(controller);
            if (controllers.isEmpty()) {
                releaseExecutorService();
            }
        }
    }

    private void searchingTask() {
        try {
            if (Platform.isServerNotSafe()) return;
            IN_SERVICE.set(true);
            for (var controller : controllers) {
                controller.asyncCheckPattern(periodID);
            }
        } catch (Throwable e) {
            MBD2.LOGGER.error("asyncThreadLogic error: {}", e.getMessage());
        } finally {
            IN_SERVICE.set(false);
        }
        periodID++;
    }

    public static boolean isThreadService() {
        return IN_SERVICE.get();
    }

    public void releaseExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        AsyncThreadData
        executorService = null;
    }

}

package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.error.PatternError;
import com.lowdragmc.mbd2.api.pattern.error.PatternStringError;
import com.lowdragmc.mbd2.api.pattern.error.SinglePredicateError;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.api.pattern.util.PatternMatchContext;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class BlockPattern {

    static Direction[] FACINGS = {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};
    static Direction[] FACINGS_H = {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
    public final int[][] aisleRepetitions;
    public final RelativeDirection[] structureDir;
    protected final TraceabilityPredicate[][][] blockMatches; //[z][y][x]
    protected final int fingerLength; //z size
    protected final int thumbLength; //y size
    protected final int palmLength; //x size
    protected final int[] centerOffset; // x, y, z, minZ, maxZ

    public BlockPattern(TraceabilityPredicate[][][] predicatesIn, RelativeDirection[] structureDir, int[][] aisleRepetitions, int[] centerOffset) {
        this.blockMatches = predicatesIn;
        this.fingerLength = predicatesIn.length;
        this.structureDir = structureDir;
        this.aisleRepetitions = aisleRepetitions;

        if (this.fingerLength > 0) {
            this.thumbLength = predicatesIn[0].length;

            if (this.thumbLength > 0) {
                this.palmLength = predicatesIn[0][0].length;
            } else {
                this.palmLength = 0;
            }
        } else {
            this.thumbLength = 0;
            this.palmLength = 0;
        }

        this.centerOffset = centerOffset;
    }

    public boolean checkPatternAtWithoutController(MultiblockState worldState, Direction facing) {
        var centerPos = worldState.controllerPos;
        return checkPatternAt(worldState, centerPos, facing, false);
    }

    public boolean checkPatternAt(MultiblockState worldState, boolean savePredicate) {
        IMultiController controller = worldState.getController();
        if (controller == null) {
            worldState.setError(new PatternStringError("no controller found"));
            return false;
        }
        BlockPos centerPos = controller.getPos();
        Direction[] facings = controller.hasFrontFacing() ?
                new Direction[]{controller.getFrontFacing().orElseThrow()} :
                FACINGS_H;
        for (Direction facing : facings) {
            if (checkPatternAt(worldState, centerPos, facing, savePredicate)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkPatternAt(MultiblockState worldState, BlockPos centerPos, Direction facing, boolean savePredicate) {
        boolean findFirstAisle = false;
        int minZ = -centerOffset[4];
        worldState.clean();
        worldState.setCheckingFacing(facing);
        PatternMatchContext matchContext = worldState.getMatchContext();
        Map<PatternPredicate, Integer> globalCount = worldState.getGlobalCount();
        Map<PatternPredicate, Integer> layerCount = worldState.getLayerCount();
        //Checking aisles
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            //Checking repeatable slices
            loop:
            for (r = 0; (findFirstAisle ? r < aisleRepetitions[c][1] : z <= -centerOffset[3]); r++) {
                //Checking single slice
                layerCount.clear();

                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        worldState.setError(null);
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing).offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        if (!worldState.update(pos, predicate)) {
                            return false;
                        }
                        if (predicate.addCache()) {
                            worldState.addPosCache(pos);
                            if (savePredicate) {
                                matchContext.getOrCreate("predicates", HashMap::new).put(pos, predicate);
                            }
                        }
                        boolean canPartShared = true;
                        var machineOptional = IMachine.ofMachine(worldState.getTileEntity());
                        if (machineOptional.isPresent() && machineOptional.orElseThrow() instanceof IMultiPart part) { // add detected parts
                            if (!predicate.isAny()) {
                                if (part.isFormed() && !part.canShared() && !part.hasController(worldState.controllerPos)) { // check part can be shared
                                    canPartShared = false;
                                    worldState.setError(new PatternStringError("multiblocked.pattern.error.share"));
                                } else {
                                    matchContext.getOrCreate("parts", HashSet::new).add(part);
                                }
                            }
                        }
                        // TODO vaBlock
//                        if (worldState.getBlockState().getBlock() instanceof ActiveBlock) {
//                            matchContext.getOrCreate("vaBlocks", LongOpenHashSet::new).add(worldState.getPos().asLong());
//                        }
                        if (!predicate.test(worldState) || !canPartShared) { // matching failed
                            if (findFirstAisle) {
                                if (r < aisleRepetitions[c][0]) {//retreat to see if the first aisle can start later
                                    r = c = 0;
                                    z = minZ++;
                                    matchContext.reset();
                                    findFirstAisle = false;
                                }
                            } else {
                                z++;//continue searching for the first aisle
                            }
                            continue loop;
                        }
                        matchContext.getOrCreate("ioMap", Long2ObjectOpenHashMap::new).put(worldState.getPos().asLong(), worldState.io);
                    }
                }
                findFirstAisle = true;
                z++;

                //Check layer-local matcher predicate
                for (Map.Entry<PatternPredicate, Integer> entry : layerCount.entrySet()) {
                    if (entry.getValue() < entry.getKey().minLayerCount) {
                        worldState.setError(new SinglePredicateError(entry.getKey(), 3));
                        return false;
                    }
                }
            }
            //Repetitions out of range
            if (r < aisleRepetitions[c][0] || worldState.hasError() || !findFirstAisle) {
                if (!worldState.hasError()) {
                    worldState.setError(new PatternError());
                }
                return false;
            }
        }

        //Check count matches amount
        for (Map.Entry<PatternPredicate, Integer> entry : globalCount.entrySet()) {
            if (entry.getValue() < entry.getKey().minCount) {
                worldState.setError(new SinglePredicateError(entry.getKey(), 1));
                return false;
            }
        }

        worldState.setError(null);
        return true;
    }

    public void autoBuild(Player player, MultiblockState worldState) {
        Level world = player.level();
        int minZ = -centerOffset[4];
        worldState.clean();
        IMultiController controller = worldState.getController();
        BlockPos centerPos = controller.getPos();
        Direction facing = controller.getFrontFacing().orElse(Direction.NORTH);
        Map<PatternPredicate, Integer> cacheGlobal = worldState.getGlobalCount();
        Map<PatternPredicate, Integer> cacheLayer = worldState.getLayerCount();
        Map<BlockPos, Object> blocks = new HashMap<>();
        blocks.put(centerPos, controller);
        // Fluids are placed AFTER every solid block in the pattern is settled. If we placed
        // them inline, source fluids could flow into adjacent still-empty pattern cells (or
        // through air-predicate cells) and corrupt subsequent placements. The deferred queue
        // runs after the {@code blocks.forEach} fixup pass below, so by the time fluids land,
        // every other cell is final.
        List<Runnable> deferredFluidPlacements = new ArrayList<>();
        // Bucket slots claimed for deferred placement; the inventory-scan loop skips them so
        // the same bucket isn't picked twice.
        Set<Integer> reservedSlots = new HashSet<>();
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            for (r = 0; r < aisleRepetitions[c][0]; r++) {
                cacheLayer.clear();
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing).offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        worldState.update(pos, predicate);
                        if (!world.isEmptyBlock(pos)) {
                            blocks.put(pos, world.getBlockState(pos));
                            for (PatternPredicate limit : predicate.limited) {
                                limit.testLimited(worldState);
                            }
                        } else {
                            boolean find = false;
                            BlockInfo[] infos = new BlockInfo[0];
                            PatternPredicate chosenPredicate = null;
                            for (PatternPredicate limit : predicate.limited) {
                                if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != facing) continue;
                                if (limit.minLayerCount > 0) {
                                    if (!cacheLayer.containsKey(limit)) {
                                        cacheLayer.put(limit, 1);
                                    } else if (cacheLayer.get(limit) < limit.minLayerCount && (limit.maxLayerCount == -1 || cacheLayer.get(limit) < limit.maxLayerCount)) {
                                        cacheLayer.put(limit, cacheLayer.get(limit) + 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = limit.getCandidates();
                                chosenPredicate = limit;
                                find = true;
                                break;
                            }
                            if (!find) {
                                for (PatternPredicate limit : predicate.limited) {
                                    if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != facing) continue;
                                    if (limit.minCount > 0) {
                                        if (!cacheGlobal.containsKey(limit)) {
                                            cacheGlobal.put(limit, 1);
                                        } else if (cacheGlobal.get(limit) < limit.minCount && (limit.maxCount == -1 || cacheGlobal.get(limit) < limit.maxCount)) {
                                            cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                        } else {
                                            continue;
                                        }
                                    } else {
                                        continue;
                                    }
                                    infos = limit.getCandidates();
                                    chosenPredicate = limit;
                                    find = true;
                                    break;
                                }
                            }
                                if (!find) { // no limited
                                for (PatternPredicate limit : predicate.limited) {
                                    if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != facing) continue;
                                    if (limit.maxLayerCount != -1 && cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount)
                                        continue;
                                    if (limit.maxCount != -1 && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount)
                                        continue;
                                    if (cacheLayer.containsKey(limit)) {
                                        cacheLayer.put(limit, cacheLayer.get(limit) + 1);
                                    } else {
                                        cacheLayer.put(limit, 1);
                                    }
                                    if (cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        cacheGlobal.put(limit, 1);
                                    }
                                    infos = ArrayUtils.addAll(infos, limit.getCandidates());
                                    if (chosenPredicate == null) chosenPredicate = limit;
                                }
                                for (PatternPredicate common : predicate.common) {
                                    if (common.controllerFront.isEnable() && common.controllerFront.getValue() != facing) continue;
                                    infos = ArrayUtils.addAll(infos, common.getCandidates());
                                    if (chosenPredicate == null) chosenPredicate = common;
                                }
                            }

                            List<ItemStack> candidates = new ArrayList<>();
                            if (infos != null) {
                                for (BlockInfo info : infos) {
                                    if (info.getBlockState().getBlock() != Blocks.AIR) {
                                        candidates.add(info.getItemStackForm());
                                    }
                                }
                            }

                            // check inventory
                            ItemStack found = null;
                            int foundSlot = -1;
                            if (!player.isCreative()) {
                                for (int i = 0; i < player.getInventory().items.size(); i++) {
                                    if (reservedSlots.contains(i)) continue;
                                    ItemStack itemStack = player.getInventory().items.get(i);
                                    if (candidates.stream().anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, itemStack)) && !itemStack.isEmpty() && (itemStack.getItem() instanceof BlockItem || itemStack.getItem() instanceof BucketItem)) {
                                        found = itemStack.copy();
                                        foundSlot = i;
                                        break;
                                    }
                                }
                            } else {
                                for (ItemStack candidate : candidates) {
                                    found = candidate.copy();
                                    if (!found.isEmpty() && (found.getItem() instanceof BlockItem || found.getItem() instanceof BucketItem)) {
                                        break;
                                    }
                                    found = null;
                                }
                            }
                            if (found == null) continue;
                            if (found.getItem() instanceof BlockItem itemBlock) {
                                BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, found, BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
                                InteractionResult interactionResult = itemBlock.place(context);
                                var placed = interactionResult != InteractionResult.FAIL;
                                if (placed && foundSlot >= 0) {
                                    player.getInventory().getItem(foundSlot).shrink(1);
                                }
                                // rotateFollowController: BlockItem.place picks orientation from
                                // player look direction, which isn't what we want. Force the
                                // canonical (NORTH-authored) state and rotate it to the controller's
                                // facing — including NORTH itself, since the placed state may
                                // disagree with the canonical (e.g. mock player puts stairs SOUTH).
                                if (placed && chosenPredicate != null && chosenPredicate.rotateFollowController) {
                                    BlockState placedState = world.getBlockState(pos);
                                    BlockInfo[] sourceInfos = chosenPredicate.getCandidates();
                                    if (sourceInfos != null) {
                                        for (BlockInfo info : sourceInfos) {
                                            BlockState canonical = info.getBlockState();
                                            if (canonical != null && canonical.is(placedState.getBlock())) {
                                                world.setBlock(pos, canonical.rotate(RotationHelper.rotationFromFacing(facing)), 3);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else if (found.getItem() instanceof BucketItem itemBucket) {
                                // Defer fluid placement until after all solid blocks land.
                                final BlockPos fluidPos = pos;
                                final ItemStack bucketStack = found.copy();
                                final int slot = foundSlot;
                                if (slot >= 0) reservedSlots.add(slot);
                                deferredFluidPlacements.add(() -> {
                                    if (itemBucket.emptyContents(player, world, fluidPos, null, null)) {
                                        itemBucket.checkExtraContent(player, world, bucketStack, fluidPos);
                                        if (player instanceof ServerPlayer serverPlayer) {
                                            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, fluidPos, bucketStack);
                                        }
                                        player.awardStat(Stats.ITEM_USED.get(itemBucket));
                                        var emptyBucket = BucketItem.getEmptySuccessItem(bucketStack, player);
                                        if (slot >= 0 && !player.isCreative()) {
                                            player.getInventory().setItem(slot, emptyBucket);
                                        }
                                    }
                                });
                                // Don't touch the blocks map for fluid cells — the fixup pass
                                // would re-stamp air over the cell before our deferred placement
                                // runs. The deferred runnable is the sole authority for this pos.
                                continue;
                            }

                            var machineOptional = IMachine.ofMachine(world, pos);
                            if (machineOptional.isPresent()) {
                                blocks.put(pos, machineOptional.orElseThrow());
                            } else {
                                blocks.put(pos, world.getBlockState(pos));
                            }
                        }
                    }
                }
                z++;
            }
        }
        blocks.forEach((pos, block) -> { // adjust facing
            if (!(block instanceof IMultiController)) {
                if (block instanceof BlockState state) {
                    world.setBlock(pos, state, 3);
                } else if (block instanceof IMachine machine) {
                    world.setBlock(pos, machine.getBlockState(), 3);
                }
            }
        });
        // Now that every solid cell is final, place fluids. Anything they flow into is by
        // construction outside this pattern (or an explicit air cell, which is the author's
        // intent anyway).
        deferredFluidPlacements.forEach(Runnable::run);
    }

    public BlockInfo[][][] getPreview(int[] repetition) {
        Map<PatternPredicate, Integer> cacheGlobal = new HashMap<>();
        Map<BlockPos, BlockInfo> blocks = new HashMap<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int l = 0, x = 0; l < this.fingerLength; l++) {
            for (int r = 0; r < repetition[l]; r++) {
                //Checking single slice
                Map<PatternPredicate, Integer> cacheLayer = new HashMap<>();
                for (int y = 0; y < this.thumbLength; y++) {
                    for (int z = 0; z < this.palmLength; z++) {
                        var predicate = this.blockMatches[l][y][z];
                        boolean find = false;
                        BlockInfo[] infos = null;
                        for (PatternPredicate limit : predicate.limited) { // check layer and previewCount
                            if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != Direction.NORTH) continue;
                            if (limit.minLayerCount > 0) {
                                if (!cacheLayer.containsKey(limit)) {
                                    cacheLayer.put(limit, 1);
                                } else if (cacheLayer.get(limit) < limit.minLayerCount) {
                                    cacheLayer.put(limit, cacheLayer.get(limit) + 1);
                                } else {
                                    continue;
                                }
                                if (cacheGlobal.getOrDefault(limit, 0) < limit.previewCount) {
                                    if (!cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, 1);
                                    } else if (cacheGlobal.get(limit) < limit.previewCount) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                continue;
                            }
                            infos = limit.getCandidates();
                            find = true;
                            break;
                        }
                        if (!find) { // check global and previewCount
                            for (PatternPredicate limit : predicate.limited) {
                                if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != Direction.NORTH) continue;
                                if (limit.minCount == -1 && limit.previewCount == -1) continue;
                                if (cacheGlobal.getOrDefault(limit, 0) < limit.previewCount) {
                                    if (!cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, 1);
                                    } else if (cacheGlobal.get(limit) < limit.previewCount) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        continue;
                                    }
                                } else if (limit.minCount > 0) {
                                    if (!cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, 1);
                                    } else if (cacheGlobal.get(limit) < limit.minCount) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = limit.getCandidates();
                                find = true;
                                break;
                            }
                        }
                        if (!find) { // check common with previewCount
                            for (PatternPredicate common : predicate.common) {
                                if (common.controllerFront.isEnable() && common.controllerFront.getValue() != Direction.NORTH) continue;
                                if (common.previewCount > 0) {
                                    if (!cacheGlobal.containsKey(common)) {
                                        cacheGlobal.put(common, 1);
                                    } else if (cacheGlobal.get(common) < common.previewCount) {
                                        cacheGlobal.put(common, cacheGlobal.get(common) + 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = common.getCandidates();
                                find = true;
                                break;
                            }
                        }
                        if (!find) { // check without previewCount
                            for (PatternPredicate common : predicate.common) {
                                if (common.controllerFront.isEnable() && common.controllerFront.getValue() != Direction.NORTH) continue;
                                if (common.previewCount == -1) {
                                    infos = common.getCandidates();
                                    find = true;
                                    break;
                                }
                            }
                        }
                        if (!find) { // check max
                            for (PatternPredicate limit : predicate.limited) {
                                if (limit.controllerFront.isEnable() && limit.controllerFront.getValue() != Direction.NORTH) continue;
                                if (limit.previewCount != -1) {
                                    continue;
                                } else if (limit.maxCount != -1 || limit.maxLayerCount != -1) {
                                    if (cacheGlobal.getOrDefault(limit, 0) < limit.maxCount) {
                                        if (!cacheGlobal.containsKey(limit)) {
                                            cacheGlobal.put(limit, 1);
                                        } else {
                                            cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                        }
                                    } else if (cacheLayer.getOrDefault(limit, 0) < limit.maxLayerCount) {
                                        if (!cacheLayer.containsKey(limit)) {
                                            cacheLayer.put(limit, 1);
                                        } else {
                                            cacheLayer.put(limit, cacheLayer.get(limit) + 1);
                                        }
                                    } else {
                                        continue;
                                    }
                                }

                                infos = limit.getCandidates();
                                break;
                            }
                        }
                        BlockInfo info = infos == null || infos.length == 0 ? BlockInfo.EMPTY : infos[0];
                        BlockPos pos = setActualRelativeOffset(z, y, x, Direction.NORTH);

                        blocks.put(pos, info);
                        minX = Math.min(pos.getX(), minX);
                        minY = Math.min(pos.getY(), minY);
                        minZ = Math.min(pos.getZ(), minZ);
                        maxX = Math.max(pos.getX(), maxX);
                        maxY = Math.max(pos.getY(), maxY);
                        maxZ = Math.max(pos.getZ(), maxZ);
                    }
                }
                x++;
            }
        }
        var result = new BlockInfo[maxX - minX + 1][maxY - minY + 1][maxZ - minZ + 1];
        int finalMinX = minX;
        int finalMinY = minY;
        int finalMinZ = minZ;
        blocks.forEach((pos, info) -> result[pos.getX() - finalMinX][pos.getY() - finalMinY][pos.getZ() - finalMinZ] = info);
        return result;
    }


    /**
     * Walk the pattern at its maximum-repetition extent and collect every world position whose
     * predicate is not {@link TraceabilityPredicate#isAny()}. Used by the snapshot system to size
     * its bbox.
     */
    public LongOpenHashSet collectTrackedPositions(BlockPos centerPos, Direction facing) {
        LongOpenHashSet out = new LongOpenHashSet();
        int minZ = -centerOffset[4];
        for (int c = 0, z = minZ; c < this.fingerLength; c++) {
            int reps = aisleRepetitions[c][1];
            for (int r = 0; r < reps; r++) {
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        if (predicate == null || predicate.isAny()) continue;
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        out.add(pos.asLong());
                    }
                }
                z++;
            }
        }
        return out;
    }

    /**
     * Like {@link #collectTrackedPositions} but only includes positions whose predicate set
     * contains at least one predicate with a non-empty {@code nbt} config — i.e. positions
     * where the snapshot must also capture the BE's NBT.
     */
    public LongOpenHashSet collectNbtSensitivePositions(BlockPos centerPos, Direction facing) {
        LongOpenHashSet out = new LongOpenHashSet();
        int minZ = -centerOffset[4];
        for (int c = 0, z = minZ; c < this.fingerLength; c++) {
            int reps = aisleRepetitions[c][1];
            for (int r = 0; r < reps; r++) {
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        if (predicate == null || predicate.isAny()) continue;
                        boolean sensitive = false;
                        for (var p : predicate.common) if (p.nbt != null && !p.nbt.isEmpty()) { sensitive = true; break; }
                        if (!sensitive) for (var p : predicate.limited) if (p.nbt != null && !p.nbt.isEmpty()) { sensitive = true; break; }
                        if (!sensitive) continue;
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        out.add(pos.asLong());
                    }
                }
                z++;
            }
        }
        return out;
    }

    private BlockPos setActualRelativeOffset(int x, int y, int z, Direction facing) {
        int[] c0 = new int[]{x, y, z}, c1 = new int[3];
        for (int i = 0; i < 3; i++) {
            switch (structureDir[i].getActualFacing(facing)) {
                case UP -> c1[1] = c0[i];
                case DOWN -> c1[1] = -c0[i];
                case WEST -> c1[0] = -c0[i];
                case EAST -> c1[0] = c0[i];
                case NORTH -> c1[2] = -c0[i];
                case SOUTH -> c1[2] = c0[i];
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }
}

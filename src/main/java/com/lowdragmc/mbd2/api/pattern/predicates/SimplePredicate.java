package com.lowdragmc.mbd2.api.pattern.predicates;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IAutoPersistedSerializable;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.error.PatternStringError;
import com.lowdragmc.mbd2.api.pattern.error.SinglePredicateError;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.mbd2.integration.ldlib.MBDLDLibPlugin;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SimplePredicate implements IAutoPersistedSerializable, IConfigurable {
    public static SimplePredicate ANY = new SimplePredicate(x -> true, null);
    public static SimplePredicate AIR = new SimplePredicate(blockWorldState -> blockWorldState.getWorld().isEmptyBlock(blockWorldState.getPos()), null);
    @Nullable
    public Supplier<BlockInfo[]> candidates;
    public Predicate<MultiblockState> predicate;
    @Configurable(name = "config.block_pattern.predicate.tooltips", tips = "config.block_pattern.predicate.tooltips.tooltip")
    public final List<Component> toolTips = new ArrayList<>();
    @Configurable(name = "config.block_pattern.predicate.minCount", tips = { "config.block_pattern.predicate.minCount.tooltip.0", "config.block_pattern.predicate.minCount.tooltip.1" })
    @NumberRange(range = {-1, Integer.MAX_VALUE})
    public int minCount = -1;
    @Configurable(name = "config.block_pattern.predicate.maxCount", tips = { "config.block_pattern.predicate.maxCount.tooltip.0", "config.block_pattern.predicate.maxCount.tooltip.1" })
    @NumberRange(range = {-1, Integer.MAX_VALUE})
    public int maxCount = -1;
    @Configurable(name = "config.block_pattern.predicate.minLayerCount", tips = { "config.block_pattern.predicate.minLayerCount.tooltip.0", "config.block_pattern.predicate.minLayerCount.tooltip.1" })
    @NumberRange(range = {-1, Integer.MAX_VALUE})
    public int minLayerCount = -1;
    @Configurable(name = "config.block_pattern.predicate.maxLayerCount", tips = { "config.block_pattern.predicate.maxLayerCount.tooltip.0", "config.block_pattern.predicate.maxLayerCount.tooltip.1" })
    @NumberRange(range = {-1, Integer.MAX_VALUE})
    public int maxLayerCount = -1;
    @Configurable(name = "config.block_pattern.predicate.previewCount", tips = { "config.block_pattern.predicate.previewCount.tooltip.0", "config.block_pattern.predicate.previewCount.tooltip.1" })
    @NumberRange(range = {-1, Integer.MAX_VALUE})
    public int previewCount = -1;
    @Configurable(name = "config.block_pattern.predicate.disableRenderFormed", tips = "config.block_pattern.predicate.disableRenderFormed.tooltip")
    public boolean disableRenderFormed = false;
    @Configurable(name = "config.block_pattern.io", tips = "config.block_pattern.predicate.io.tooltip")
    public IO io = IO.BOTH;
    @Configurable(name = "config.block_pattern.slotName", tips = "config.block_pattern.predicate.slotName.tooltip")
    public String slotName;
    @Configurable(name = "config.block_pattern.nbt", tips = "config.block_pattern.predicate.nbt.tooltip")
    public CompoundTag nbt = new CompoundTag();

    protected SimplePredicate() {
        this(x -> true, null);
    }

    public SimplePredicate(Predicate<MultiblockState> predicate, @Nullable Supplier<BlockInfo[]> candidates) {
        this.predicate = predicate;
        this.candidates = candidates;
    }


    public static CompoundTag serializeWrapper(SimplePredicate predicate) {
        if (predicate == AIR) {
            var tag = new CompoundTag();
            tag.putString("_type", "air");
            return tag;
        }
        if (predicate == ANY) {
            var tag = new CompoundTag();
            tag.putString("_type", "any");
            return tag;
        }
        return predicate.serializeNBT();
    }

    public static SimplePredicate deserializeWrapper(CompoundTag tag) {
        var type = tag.getString("_type");
        if (type.equals("air")) {
            return AIR;
        }
        if (type.equals("any")) {
            return ANY;
        }
        var wrapper = MBDLDLibPlugin.REGISTER_PREDICATES.get(type);
        if (wrapper != null) {
            var renderer = wrapper.creator().get();
            renderer.deserializeNBT(tag);
            renderer.buildPredicate();
            return renderer;
        }
        return null;
    }

    public SimplePredicate buildPredicate() {
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Component> getToolTips(TraceabilityPredicate predicates) {
        List<Component> result = new ArrayList<>();
        if (toolTips != null && !toolTips.isEmpty()) {
            result.addAll(toolTips);
        }
        if (minCount == maxCount && maxCount != -1) {
            result.add(Component.translatable("mbd2.multiblock.pattern.error.limited_exact", minCount));
        } else if (minCount != maxCount && minCount != -1 && maxCount != -1) {
            result.add(Component.translatable("mbd2.multiblock.pattern.error.limited_within", minCount, maxCount));
        } else {
            if (minCount != -1) {
                result.add(Component.translatable("mbd2.multiblock.pattern.error.limited.1", minCount));
            }
            if (maxCount != -1) {
                result.add(Component.translatable("mbd2.multiblock.pattern.error.limited.0", maxCount));
            }
        }
        if (predicates == null) return result;
        if (predicates.isSingle()) {
            result.add(Component.translatable("mbd2.multiblock.pattern.single"));
        }
        if (predicates.hasAir()) {
            result.add(Component.translatable("mbd2.multiblock.pattern.replaceable_air"));
        }
        return result;
    }

    public boolean test(MultiblockState blockWorldState) {
        if (predicate.test(blockWorldState)) {
            return checkInnerConditions(blockWorldState);
        }
        return false;
    }

    public boolean testLimited(MultiblockState blockWorldState) {
        if (testGlobal(blockWorldState) && testLayer(blockWorldState)) {
            return checkInnerConditions(blockWorldState);
        }
        return false;
    }

    private boolean checkInnerConditions(MultiblockState blockWorldState) {
        if (disableRenderFormed) {
            blockWorldState.getMatchContext().getOrCreate("renderMask", LongOpenHashSet::new).add(blockWorldState.getPos().asLong());
        }
        if (io != IO.BOTH) {
            if (blockWorldState.io == IO.BOTH) {
                blockWorldState.io = io;
            } else if (blockWorldState.io != io) {
                blockWorldState.io = null;
            }
        }
        if (!nbt.isEmpty() && !blockWorldState.world.isClientSide) {
            BlockEntity te = blockWorldState.getTileEntity();
            if (te != null) {
                var tag = te.saveWithFullMetadata();
                var merged = tag.copy().merge(nbt);
                if (tag.equals(merged)) {
                    return true;
                }
            }
            blockWorldState.setError(new PatternStringError("The NBT fails to match"));
            return false;
        }
        if (slotName != null && !slotName.isEmpty()) {
            Map<Long, Set<String>> slots = blockWorldState.getMatchContext().getOrCreate("slots", Long2ObjectArrayMap::new);
            slots.computeIfAbsent(blockWorldState.getPos().asLong(), s->new HashSet<>()).add(slotName);
            return true;
        }
        return true;
    }

    public boolean testGlobal(MultiblockState blockWorldState) {
        if (minCount == -1 && maxCount == -1) return true;
        Integer count = blockWorldState.getGlobalCount().get(this);
        boolean base = predicate.test(blockWorldState);
        count = (count == null ? 0 : count) + (base ? 1 : 0);
        blockWorldState.getGlobalCount().put(this, count);
        if (maxCount == -1 || count <= maxCount) return base;
        blockWorldState.setError(new SinglePredicateError(this, 0));
        return false;
    }

    public boolean testLayer(MultiblockState blockWorldState) {
        if (minLayerCount == -1 && maxLayerCount == -1) return true;
        Integer count = blockWorldState.getLayerCount().get(this);
        boolean base = predicate.test(blockWorldState);
        count = (count == null ? 0 : count) + (base ? 1 : 0);
        blockWorldState.getLayerCount().put(this, count);
        if (maxLayerCount == -1 || count <= maxLayerCount) return base;
        blockWorldState.setError(new SinglePredicateError(this, 2));
        return false;
    }

    public List<ItemStack> getCandidates() {
        if (LDLib.isClient()) {
            return candidates == null ? Collections.emptyList() : Arrays.stream(this.candidates.get()).filter(info -> info.getBlockState().getBlock() != Blocks.AIR)
                    .map(blockInfo -> blockInfo.getItemStackForm(Minecraft.getInstance().level, BlockPos.ZERO)).collect(Collectors.toList());
        }
        return candidates == null ? Collections.emptyList() : Arrays.stream(this.candidates.get()).filter(info -> info.getBlockState().getBlock() != Blocks.AIR).map(BlockInfo::getItemStackForm).collect(Collectors.toList());
    }

}

package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TraceabilityPredicate {

    public List<PatternPredicate> common = new ArrayList<>();
    public List<PatternPredicate> limited = new ArrayList<>();
    public boolean isController;

    public TraceabilityPredicate() {}

    public TraceabilityPredicate(TraceabilityPredicate predicate) {
        common.addAll(predicate.common);
        limited.addAll(predicate.limited);
        isController = predicate.isController;
    }

    public TraceabilityPredicate(Predicate<MultiblockState> predicate, Supplier<BlockInfo[]> candidates) {
        common.add(new PatternPredicate(predicate, candidates));
    }

    public TraceabilityPredicate(PatternPredicate patternPredicate) {
        if (patternPredicate.minCount != -1 || patternPredicate.maxCount != -1) {
            limited.add(patternPredicate);
        } else {
            common.add(patternPredicate);
        }
    }

    /**
     * Mark it as the controller of this multi. Normally you won't call it yourself. Use plz.
     */
    public TraceabilityPredicate setController() {
        isController = true;
        return this;
    }

    public TraceabilityPredicate sort() {
        limited.sort(Comparator.comparingInt(a -> a.minCount));
        return this;
    }

    /**
     * Add tooltips for candidates. They are shown in JEI Pages.
     */
    public TraceabilityPredicate addTooltips(Component... tips) {
        if (tips.length > 0) {
            List<Component> tooltips = Arrays.stream(tips).toList();
            common.forEach(predicate -> {
                predicate.toolTips.addAll(tooltips);
            });
            limited.forEach(predicate -> {
                predicate.toolTips.addAll(tooltips);
            });
        }
        return this;
    }

    /**
     * Set the minimum number of candidate blocks.
     */
    public TraceabilityPredicate setMinGlobalLimited(int min) {
        limited.addAll(common);
        common.clear();
        for (PatternPredicate predicate : limited) {
            predicate.minCount = min;
        }
        return this;
    }

    public TraceabilityPredicate setMinGlobalLimited(int min, int previewCount) {
        return this.setMinGlobalLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks.
     */
    public TraceabilityPredicate setMaxGlobalLimited(int max) {
        limited.addAll(common);
        common.clear();
        for (PatternPredicate predicate : limited) {
            predicate.maxCount = max;
        }
        return this;
    }

    public TraceabilityPredicate setMaxGlobalLimited(int max, int previewCount) {
        return this.setMaxGlobalLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Set the minimum number of candidate blocks for each aisle layer.
     */
    public TraceabilityPredicate setMinLayerLimited(int min) {
        limited.addAll(common);
        common.clear();
        for (PatternPredicate predicate : limited) {
            predicate.minLayerCount = min;
        }
        return this;
    }

    public TraceabilityPredicate setMinLayerLimited(int min, int previewCount) {
        return this.setMinLayerLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks for each aisle layer.
     */
    public TraceabilityPredicate setMaxLayerLimited(int max) {
        limited.addAll(common);
        common.clear();
        for (PatternPredicate predicate : limited) {
            predicate.maxLayerCount = max;
        }
        return this;
    }

    public TraceabilityPredicate setMaxLayerLimited(int max, int previewCount) {
        return this.setMaxLayerLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Sets the Minimum and Maximum limit to the passed value
     * @param limit The Maximum and Minimum limit
     */
    public TraceabilityPredicate setExactLimit(int limit) {
        return this.setMinGlobalLimited(limit).setMaxGlobalLimited(limit);
    }

    /**
     * Set the number of it appears in JEI pages. It only affects JEI preview. (The specific number)
     */
    public TraceabilityPredicate setPreviewCount(int count) {
        common.forEach(predicate -> predicate.previewCount = count);
        limited.forEach(predicate -> predicate.previewCount = count);
        return this;
    }

    /**
     * Replace matched blocks with proxy blocks while the multiblock is formed.
     */
    public TraceabilityPredicate proxyWhileFormed(java.util.function.Consumer<PatternPredicate.ProxyWhileFormed> configurator) {
        common.forEach(predicate -> {
            predicate.proxyWhileFormed.setEnable(true);
            configurator.accept(predicate.proxyWhileFormed);
        });
        limited.forEach(predicate -> {
            predicate.proxyWhileFormed.setEnable(true);
            configurator.accept(predicate.proxyWhileFormed);
        });
        return this;
    }

    public TraceabilityPredicate proxyWhileFormed() {
        return proxyWhileFormed(proxy -> {});
    }

    /**
     * Set io.
     */
    public TraceabilityPredicate setIO(IO io) {
        common.forEach(predicate -> predicate.io = io);
        limited.forEach(predicate -> predicate.io = io);
        return this;
    }

    public TraceabilityPredicate setNBT(CompoundTag nbt) {
        common.forEach(predicate -> predicate.nbt = nbt);
        limited.forEach(predicate -> predicate.nbt = nbt);
        return this;
    }

    public TraceabilityPredicate setSlotName(String slotName) {
        common.forEach(predicate -> predicate.slotName = slotName);
        limited.forEach(predicate -> predicate.slotName = slotName);
        return this;
    }

    /**
     * Mark every contained {@link PatternPredicate} as rotation-following so its expected
     * block state auto-rotates to the controller's horizontal facing during pattern checks,
     * autoBuild placement, and preview rendering.
     */
    public TraceabilityPredicate rotateFollowController() {
        common.forEach(predicate -> predicate.rotateFollowController = true);
        limited.forEach(predicate -> predicate.rotateFollowController = true);
        return this;
    }

    public boolean test(MultiblockState blockWorldState) {
        blockWorldState.io = IO.BOTH;
        boolean flag = false;
        for (PatternPredicate predicate : limited) {
            if (predicate.testLimited(blockWorldState)) {
                flag = true;
            }
        }
        flag = flag || common.stream().anyMatch(predicate->predicate.test(blockWorldState));
        if (flag) {
            blockWorldState.setError(null);
        }
        return flag;
    }

    public TraceabilityPredicate or(TraceabilityPredicate other) {
        if (other != null) {
            TraceabilityPredicate newPredicate = new TraceabilityPredicate(this);
            newPredicate.common.addAll(other.common);
            newPredicate.limited.addAll(other.limited);
            return newPredicate;
        }
        return this;
    }

    public boolean isAny() {
        return this.common.size() == 1 && this.limited.isEmpty() && this.common.get(0) == PatternPredicate.ANY;
    }

    public boolean addCache() {
        return !isAny();
    }

    public boolean isAir() {
        return this.common.size() == 1 && this.limited.isEmpty() && this.common.get(0) == PatternPredicate.AIR;
    }

    public boolean isSingle() {
        return !isAny() && !isAir() && this.common.size() + this.limited.size() == 1;
    }

    public boolean hasAir() {
        return this.common.contains(PatternPredicate.AIR);
    }

}

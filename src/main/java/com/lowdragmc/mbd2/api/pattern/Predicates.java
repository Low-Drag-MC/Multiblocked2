package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateFluids;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicatePartialState;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateStates;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Predicates {

    public static TraceabilityPredicate controller(TraceabilityPredicate predicate) {
        return predicate.setController();
    }

    public static TraceabilityPredicate states(BlockState... allowedStates) {
        return new TraceabilityPredicate(new PredicateStates(allowedStates));
    }

    public static TraceabilityPredicate blocks(Block... blocks) {
        return new TraceabilityPredicate(new PredicateBlocks(blocks));
    }

    public static TraceabilityPredicate partialState(Block block, Map<String, String> requiredProperties) {
        return new TraceabilityPredicate(new PredicatePartialState(block, requiredProperties));
    }

    public static TraceabilityPredicate partialState(Block block) {
        return new TraceabilityPredicate(new PredicatePartialState(block));
    }

    public static TraceabilityPredicate partialState(Block block, PredicatePartialState.PropertyRequirement... requiredProperties) {
        return new TraceabilityPredicate(new PredicatePartialState(block, requiredProperties));
    }

    public static TraceabilityPredicate partialState(BlockState referenceState, Property<?>... requiredProperties) {
        return new TraceabilityPredicate(new PredicatePartialState(referenceState, requiredProperties));
    }

    public static TraceabilityPredicate fluids(Fluid... fluids) {
        return new TraceabilityPredicate(new PredicateFluids(fluids));
    }

    public static TraceabilityPredicate custom(Predicate<MultiblockState> predicate, Supplier<BlockInfo[]> candidates) {
        return new TraceabilityPredicate(predicate, candidates);
    }

    public static TraceabilityPredicate any() {
        return new TraceabilityPredicate(PatternPredicate.ANY);
    }

    public static TraceabilityPredicate air() {
        return new TraceabilityPredicate(PatternPredicate.AIR);
    }

}

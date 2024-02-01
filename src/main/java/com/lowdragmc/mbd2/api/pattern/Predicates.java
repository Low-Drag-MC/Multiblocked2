package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateFluids;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateStates;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Predicates {

    public static TraceabilityPredicate controller(TraceabilityPredicate predicate) {
        return predicate.setController();
    }

    public static TraceabilityPredicate states(BlockState... allowedStates) {
        var candidates = new ArrayList<BlockState>();
        for (BlockState state : allowedStates) {
            candidates.add(state);
            // TODO vaBlocks
//            if (state.getBlock() instanceof ActiveBlock block) {
//                candidates.add(block.changeActive(state, !block.isActive(state)));
//            }
        }
        return new TraceabilityPredicate(new PredicateStates(candidates.toArray(BlockState[]::new)));
    }

    public static TraceabilityPredicate blocks(Block... blocks) {
        return new TraceabilityPredicate(new PredicateBlocks(blocks));
    }

    public static TraceabilityPredicate fluids(Fluid... fluids) {
        return new TraceabilityPredicate(new PredicateFluids(fluids));
    }

    public static TraceabilityPredicate custom(Predicate<MultiblockState> predicate, Supplier<BlockInfo[]> candidates) {
        return new TraceabilityPredicate(predicate, candidates);
    }
    public static TraceabilityPredicate any() {
        return new TraceabilityPredicate(SimplePredicate.ANY);
    }

    public static TraceabilityPredicate air() {
        return new TraceabilityPredicate(SimplePredicate.AIR);
    }

}

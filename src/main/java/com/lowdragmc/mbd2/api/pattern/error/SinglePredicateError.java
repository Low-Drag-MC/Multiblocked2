package com.lowdragmc.mbd2.api.pattern.error;

import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class SinglePredicateError extends PatternError {
    public final SimplePredicate predicate;
    public final int type;

    public SinglePredicateError(SimplePredicate predicate, int type) {
        this.predicate = predicate;
        this.type = type;
    }

    @Override
    public List<List<ItemStack>> getCandidates() {
        return Collections.singletonList(predicate.getCandidates());
    }

    @Override
    public Component getErrorInfo() {
        int number = -1;
        if (type == 0) {
            number = predicate.maxCount;
        }
        if (type == 1) {
            number = predicate.minCount;
        }
        if (type == 2) {
            number = predicate.maxLayerCount;
        }
        if (type == 3) {
            number = predicate.minLayerCount;
        }
        return Component.translatable("MBD2.multiblock.pattern.error.limited." + type, number);
    }
}

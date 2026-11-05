package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.LumenLike;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Several handlers as one, for a part proxying its controllers' storage. */
public record LumenHandlerList(ILumenHandler[] handlers) implements ILumenHandler {

    @Override
    public List<LumenStack> getContainedLumen() {
        var merged = new LinkedHashMap<Lumen, Integer>();
        for (var handler : handlers) {
            for (var stack : handler.getContainedLumen()) {
                merged.merge(stack.getLumen(), stack.getAmount(), Integer::sum);
            }
        }
        return merged.entrySet().stream().map(entry -> entry.getKey().stack(entry.getValue())).toList();
    }

    @Override
    public Optional<LumenStack> getContainedLumen(LumenLike type) {
        var lumen = type.asLumen();
        var amount = Arrays.stream(handlers)
                .mapToInt(handler -> handler.getContainedLumen(type).map(LumenStack::getAmount).orElse(0))
                .sum();
        return amount > 0 ? Optional.of(lumen.stack(amount)) : Optional.empty();
    }

    @Override
    public int getCapacity(LumenLike type) {
        return Arrays.stream(handlers).mapToInt(handler -> handler.getCapacity(type)).sum();
    }

    @Override
    public int fill(LumenStack stack, Action action) {
        if (stack.isEmpty()) return 0;
        int filled = 0;
        for (var handler : handlers) {
            filled += handler.fill(stack.copyWithAmount(stack.getAmount() - filled), action);
            if (filled >= stack.getAmount()) break;
        }
        return filled;
    }

    @Override
    public LumenStack drain(LumenLike type, int amount, Action action) {
        int drained = 0;
        for (var handler : handlers) {
            drained += handler.drain(type, amount - drained, action).getAmount();
            if (drained >= amount) break;
        }
        return drained > 0 ? type.asLumen().stack(drained) : LumenStack.EMPTY;
    }

    /** @see LumenHandlerWrapper#isEmpty() */
    @Override
    public boolean isEmpty() {
        return Arrays.stream(handlers).allMatch(ILumenHandler::isEmpty);
    }
}

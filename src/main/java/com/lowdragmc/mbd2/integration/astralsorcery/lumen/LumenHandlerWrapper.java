package com.lowdragmc.mbd2.integration.astralsorcery.lumen;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.LumenLike;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;

import java.util.List;
import java.util.Optional;

/**
 * The storage as one side sees it: gated by that side's IO and the per-operation rates. Astral Sorcery's network
 * drains with a {@code null} side, so the internal IO decides whether its devices may take from the machine.
 */
public record LumenHandlerWrapper(LumenStorage storage, IO io, int maxReceive, int maxExtract) implements ILumenHandler {

    @Override
    public List<LumenStack> getContainedLumen() {
        return storage.getContents();
    }

    @Override
    public Optional<LumenStack> getContainedLumen(LumenLike type) {
        var lumen = type.asLumen();
        var amount = storage.getAmount(lumen);
        return amount > 0 ? Optional.of(lumen.stack(amount)) : Optional.empty();
    }

    @Override
    public int getCapacity(LumenLike type) {
        return storage.getCapacity(type.asLumen());
    }

    @Override
    public int fill(LumenStack stack, Action action) {
        if (stack.isEmpty() || !io.support(IO.IN)) return 0;
        return storage.fill(stack.getLumen(), Math.min(maxReceive, stack.getAmount()), action.isSimulate());
    }

    @Override
    public LumenStack drain(LumenLike type, int amount, Action action) {
        if (!io.support(IO.OUT)) return LumenStack.EMPTY;
        var lumen = type.asLumen();
        var moved = storage.drain(lumen, Math.min(maxExtract, amount), action.isSimulate());
        return moved > 0 ? lumen.stack(moved) : LumenStack.EMPTY;
    }

    /** The interface default is inverted (it answers "not empty"). */
    @Override
    public boolean isEmpty() {
        return storage.isEmpty();
    }
}

package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.Arrays;

public record ChemicalHandlerList(IChemicalHandler[] handlers) implements IChemicalHandler {

    @Override
    public int getChemicalTanks() {
        return Arrays.stream(handlers).mapToInt(IChemicalHandler::getChemicalTanks).sum();
    }

    private record Resolved(IChemicalHandler handler, int localTank) {}

    private Resolved resolve(int tank) {
        int index = 0;
        for (var handler : handlers) {
            var count = handler.getChemicalTanks();
            if (tank - index < count) return new Resolved(handler, tank - index);
            index += count;
        }
        return null;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        var r = resolve(tank);
        return r == null ? ChemicalStack.EMPTY : r.handler.getChemicalInTank(r.localTank);
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        var r = resolve(tank);
        if (r != null) r.handler.setChemicalInTank(r.localTank, stack);
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        var r = resolve(tank);
        return r == null ? 0 : r.handler.getChemicalTankCapacity(r.localTank);
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        var r = resolve(tank);
        return r != null && r.handler.isValid(r.localTank, stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        var r = resolve(tank);
        return r == null ? stack : r.handler.insertChemical(r.localTank, stack, action);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        var r = resolve(tank);
        return r == null ? ChemicalStack.EMPTY : r.handler.extractChemical(r.localTank, amount, action);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) return stack;
        ChemicalStack remaining = stack.copy();
        for (var handler : handlers) {
            if (remaining.isEmpty()) break;
            remaining = handler.insertChemical(remaining, action);
        }
        return remaining;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        if (amount <= 0) return ChemicalStack.EMPTY;
        ChemicalStack total = ChemicalStack.EMPTY;
        long remaining = amount;
        for (var handler : handlers) {
            if (remaining <= 0) break;
            ChemicalStack extracted = total.isEmpty()
                    ? handler.extractChemical(remaining, action)
                    : handler.extractChemical(total.copyWithAmount(remaining), action);
            if (extracted.isEmpty()) continue;
            if (total.isEmpty()) total = extracted;
            else total.grow(extracted.getAmount());
            remaining -= extracted.getAmount();
        }
        return total;
    }
}

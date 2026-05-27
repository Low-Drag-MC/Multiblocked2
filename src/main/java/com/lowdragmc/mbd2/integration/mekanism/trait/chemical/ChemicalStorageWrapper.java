package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

public record ChemicalStorageWrapper(ChemicalStorage[] storages, IO io, boolean allowSameChemicals) implements IChemicalHandler {

    private boolean canInput() {
        return io == IO.IN || io == IO.BOTH;
    }

    private boolean canOutput() {
        return io == IO.OUT || io == IO.BOTH;
    }

    @Override
    public int getChemicalTanks() {
        return storages.length;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return storages[tank].getStack();
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        storages[tank].setStack(stack);
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return storages[tank].getCapacity();
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return storages[tank].isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (!canInput()) return stack;
        return storages[tank].insert(stack, action, mekanism.api.AutomationType.EXTERNAL);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        if (!canOutput()) return ChemicalStack.EMPTY;
        return storages[tank].extract(amount, action, mekanism.api.AutomationType.EXTERNAL);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (!canInput() || stack.isEmpty()) return stack;
        ChemicalStack remaining = stack.copy();
        ChemicalStorage matching = null;
        if (!allowSameChemicals) {
            for (var storage : storages) {
                if (!storage.isEmpty() && ChemicalStack.isSameChemical(storage.getStack(), stack)) {
                    matching = storage;
                    break;
                }
            }
        }
        if (matching != null) {
            remaining = matching.insert(remaining, action, mekanism.api.AutomationType.EXTERNAL);
        } else {
            for (var storage : storages) {
                if (remaining.isEmpty()) break;
                remaining = storage.insert(remaining, action, mekanism.api.AutomationType.EXTERNAL);
                if (!allowSameChemicals && remaining.getAmount() < stack.getAmount()) break;
            }
        }
        return remaining;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        if (!canOutput() || amount <= 0) return ChemicalStack.EMPTY;
        ChemicalStack total = ChemicalStack.EMPTY;
        long remaining = amount;
        for (var storage : storages) {
            if (remaining <= 0) break;
            var stack = storage.getStack();
            if (stack.isEmpty()) continue;
            if (!total.isEmpty() && !ChemicalStack.isSameChemical(total, stack)) continue;
            var extracted = storage.extract(remaining, action, mekanism.api.AutomationType.EXTERNAL);
            if (extracted.isEmpty()) continue;
            if (total.isEmpty()) {
                total = extracted;
            } else {
                total.grow(extracted.getAmount());
            }
            remaining -= extracted.getAmount();
        }
        return total;
    }
}

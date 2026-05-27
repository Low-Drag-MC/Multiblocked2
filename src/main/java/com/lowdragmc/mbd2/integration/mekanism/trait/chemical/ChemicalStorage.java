package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.functions.ConstantPredicates;

import java.util.function.Predicate;

public class ChemicalStorage extends BasicChemicalTank implements IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};

    public ChemicalStorage(long capacity) {
        this(capacity, ConstantPredicates.alwaysTrue());
    }

    public ChemicalStorage(long capacity, Predicate<ChemicalStack> validator) {
        super(capacity,
                ConstantPredicates.alwaysTrueBi(),
                ConstantPredicates.alwaysTrueBi(),
                validator,
                null,
                null,
                null);
    }

    @Override
    public void onContentsChanged() {
        super.onContentsChanged();
        onContentsChanged.run();
    }

    public ChemicalStorage copy() {
        var copy = new ChemicalStorage(getCapacity());
        copy.setStack(getStack().copy());
        return copy;
    }
}

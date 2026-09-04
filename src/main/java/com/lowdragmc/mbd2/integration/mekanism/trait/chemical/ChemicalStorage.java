package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.Action;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.functions.ConstantPredicates;

import java.util.function.LongSupplier;
import java.util.function.Predicate;

public class ChemicalStorage extends BasicChemicalTank implements IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};
    /**
     * The live capacity, so a {@code capacity} runtime value override takes effect without rebuilding
     * the tank.
     * <p>
     * Possible here and not for the fluid or energy storages because {@link BasicChemicalTank} routes
     * every internal capacity read through {@link #getCapacity()} — {@code setStackSize} and
     * {@code getNeeded} both call it — where {@code FluidTank.fill} and {@code EnergyStorage.receiveEnergy}
     * read their field directly.
     */
    private final LongSupplier capacity;

    public ChemicalStorage(long capacity) {
        this(() -> capacity, ConstantPredicates.alwaysTrue());
    }

    public ChemicalStorage(long capacity, Predicate<ChemicalStack> validator) {
        this(() -> capacity, validator);
    }

    public ChemicalStorage(LongSupplier capacity, Predicate<ChemicalStack> validator) {
        // 0, not a real capacity: the superclass stores it in a private final field that nothing reads
        // once getCapacity() is overridden, and the supplier is not assignable until after super().
        super(0,
                ConstantPredicates.alwaysTrueBi(),
                ConstantPredicates.alwaysTrueBi(),
                validator,
                null,
                null,
                null);
        this.capacity = capacity;
    }

    @Override
    public long getCapacity() {
        return Math.max(0, capacity.getAsLong());
    }

    /**
     * Spill anything that no longer fits, after the capacity has shrunk.
     *
     * @return whether anything was spilled
     */
    public boolean clampToCapacity() {
        var max = getCapacity();
        if (getStored() <= max) return false;
        setStackSize(max, Action.EXECUTE);
        return true;
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

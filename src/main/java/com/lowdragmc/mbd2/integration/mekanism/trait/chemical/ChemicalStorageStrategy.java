package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.syncdata.mark.IMarkStrategy;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;

/**
 * Compares a copy of the stack instead of encoding it. Not an {@code IContentVersion}, since
 * {@link BasicChemicalTank#getStack()} hands out the live stack.
 */
public final class ChemicalStorageStrategy implements IMarkStrategy<ChemicalStorage, ChemicalStack> {
    @Override
    public boolean test(Class<?> type) {
        return ChemicalStorage.class.isAssignableFrom(type)
                && MarkStrategies.isDeclaredBy(type, BasicChemicalTank.class, "serializeNBT", HolderLookup.Provider.class)
                && MarkStrategies.isDeclaredBy(type, BasicChemicalTank.class, "getStack");
    }

    @Override
    public @NotNull ChemicalStack obtainManagedMark(@NotNull ChemicalStorage value) {
        return value.getStack().copy();
    }

    @Override
    public boolean areDifferent(@NotNull ChemicalStack managedMark, @NotNull ChemicalStorage value) {
        var stack = value.getStack();
        return managedMark.getAmount() != stack.getAmount() || !ChemicalStack.isSameChemical(managedMark, stack);
    }
}

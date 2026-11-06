package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.syncdata.mark.IMarkStrategy;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import com.lowdragmc.lowdraglib2.syncdata.utils.StackSnapshots;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * LDLib2's item handler strategy refuses classes that override {@code serializeNBT}; without one the storage would be
 * saved every async period to be compared.
 */
public final class LargeItemStackTransferStrategy implements IMarkStrategy<LargeItemStackTransfer, ItemStack[]> {
    @Override
    public boolean test(Class<?> type) {
        return LargeItemStackTransfer.class.isAssignableFrom(type)
                && MarkStrategies.isDeclaredBy(type, LargeItemStackTransfer.class, "serializeNBT", HolderLookup.Provider.class)
                && MarkStrategies.isDeclaredBy(type, ItemStackHandler.class, "getSlots")
                && MarkStrategies.isDeclaredBy(type, ItemStackHandler.class, "getStackInSlot", int.class);
    }

    @Override
    public ItemStack @NotNull [] obtainManagedMark(@NotNull LargeItemStackTransfer value) {
        var stacks = new ItemStack[value.getSlots()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = StackSnapshots.of(value.getStackInSlot(i));
        }
        return stacks;
    }

    @Override
    public boolean areDifferent(ItemStack @NotNull [] managedMark, @NotNull LargeItemStackTransfer value) {
        if (managedMark.length != value.getSlots()) return true;
        for (int i = 0; i < managedMark.length; i++) {
            if (!ItemStack.matches(managedMark[i], value.getStackInSlot(i))) return true;
        }
        return false;
    }
}

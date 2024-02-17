package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class ItemHandlerWrapper implements IItemHandlerModifiable {
    private final ItemStackTransfer storage;
    private final IO io;

    public ItemHandlerWrapper(ItemStackTransfer storage, IO io) {
        this.storage = storage;
        this.io = io;
    }

    private boolean canCapInput() {
        return io == IO.IN || io == IO.BOTH;
    }

    private boolean canCapOutput() {
        return io == IO.OUT || io == IO.BOTH;
    }

    @Override
    public int getSlots() {
        return storage.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return storage.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        storage.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (canCapInput()) {
            return storage.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (canCapOutput()) {
            return storage.extractItem(slot, amount, simulate);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return storage.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return storage.isItemValid(slot, stack);
    }

}

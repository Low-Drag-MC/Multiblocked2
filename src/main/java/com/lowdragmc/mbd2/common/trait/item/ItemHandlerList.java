package com.lowdragmc.mbd2.common.trait.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record ItemHandlerList(IItemHandler[] handlers) implements IItemHandlerModifiable {

    @Override
    public int getSlots() {
        return Arrays.stream(handlers).mapToInt(IItemHandler::getSlots).sum();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                return handler.getStackInSlot(slot - index);
            }
            index += handler.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                if (handler instanceof IItemHandlerModifiable modifiable) {
                    modifiable.setStackInSlot(slot - index, stack);
                }
                return;
            }
            index += handler.getSlots();
        }
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                return handler.insertItem(slot - index, stack, simulate);
            }
            index += handler.getSlots();
        }
        return stack;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                return handler.extractItem(slot - index, amount, simulate);
            }
            index += handler.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                return handler.getSlotLimit(slot - index);
            }
            index += handler.getSlots();
        }
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        int index = 0;
        for (var handler : handlers) {
            if (slot - index < handler.getSlots()) {
                return handler.isItemValid(slot - index, stack);
            }
            index += handler.getSlots();
        }
        return false;
    }
}

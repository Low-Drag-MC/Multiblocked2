package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * The menu slot of a {@link LargeItemStackTransfer}. Vanilla's clicks assume a slot holds at most one stack; here a
 * click takes at most one stack, and a swap that would leave the rest with nowhere to go does nothing.
 */
public class LargeItemHandlerSlot extends ItemHandlerSlot {
    private final LargeItemStackTransfer storage;
    private final int storageSlot;

    public LargeItemHandlerSlot(LargeItemStackTransfer storage, int storageSlot) {
        super(storage, storageSlot);
        this.storage = storage;
        this.storageSlot = storageSlot;
    }

    public boolean isOversized() {
        var stack = getItem();
        return stack.getCount() > stack.getMaxStackSize();
    }

    /**
     * Never less than the slot holds: {@code Slot.safeInsert} moves {@code getMaxStackSize(stack) - held} unchecked,
     * so a slot over its limit would move the difference onto the cursor.
     */
    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        if (!storage.isItemValid(storageSlot, stack)) return 0;
        var limit = storage.getStackLimit(storageSlot, stack);
        var held = getItem();
        return ItemStack.isSameItemSameComponents(held, stack) ? Math.max(limit, held.getCount()) : limit;
    }

    /**
     * A right click asks for {@code (count + 1) / 2}, which overflows for a full int slot.
     */
    @Override
    public Optional<ItemStack> tryRemove(int count, int decrement, Player player) {
        return super.tryRemove(count < 0 ? Integer.MAX_VALUE : count, decrement, player);
    }

    /**
     * Refuses swapping the carried item with the whole content.
     */
    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        if (isOversized() && !ItemStack.isSameItemSameComponents(getItem(), stack)) {
            return false;
        }
        return super.mayPlace(stack);
    }

    /**
     * A number-key swap in place of {@link AbstractContainerMenu}'s, which would move the whole content: one stack
     * into an empty inventory slot, nothing into an occupied one.
     *
     * @return whether the menu must skip its own swap
     */
    public boolean swapWithInventory(Player player, int inventorySlot) {
        if (!isOversized()) return false;
        var inventory = player.getInventory();
        if (inventory.getItem(inventorySlot).isEmpty()) {
            var taken = safeTake(getItem().getMaxStackSize(), Integer.MAX_VALUE, player);
            if (!taken.isEmpty()) {
                inventory.setItem(inventorySlot, taken);
            }
        }
        return true;
    }
}

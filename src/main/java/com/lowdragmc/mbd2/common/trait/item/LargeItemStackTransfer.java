package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Item slots that can hold more than the item's max stack size. An extraction still hands out at most one stack.
 * <p>
 * {@link ItemStack#CODEC} rejects counts above {@link Item#ABSOLUTE_MAX_STACK_SIZE}, so such a slot is saved as one
 * item plus {@value #LARGE_COUNT}; every other slot is saved exactly as {@code ItemStackHandler} saves it.
 */
public class LargeItemStackTransfer extends ItemStackTransfer {
    public static final String LARGE_COUNT = "LargeCount";

    private final IntSupplier slotLimit;
    private final BooleanSupplier ignoreItemStackLimit;
    // ItemStackTransfer keeps its own private
    private Function<ItemStack, Boolean> filter;

    public LargeItemStackTransfer(int size, IntSupplier slotLimit, BooleanSupplier ignoreItemStackLimit) {
        this(NonNullList.withSize(size, ItemStack.EMPTY), slotLimit, ignoreItemStackLimit);
    }

    protected LargeItemStackTransfer(NonNullList<ItemStack> stacks, IntSupplier slotLimit, BooleanSupplier ignoreItemStackLimit) {
        super(stacks);
        this.slotLimit = slotLimit;
        this.ignoreItemStackLimit = ignoreItemStackLimit;
    }

    @Override
    public void setFilter(Function<ItemStack, Boolean> filter) {
        super.setFilter(filter);
        this.filter = filter;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimit.getAsInt();
    }

    @Override
    public int getStackLimit(int slot, @Nonnull ItemStack stack) {
        var itemLimit = stack.getMaxStackSize();
        // items that do not stack keep one per slot: a stack of them would share one damage value
        if (itemLimit > 1 && ignoreItemStackLimit.getAsBoolean()) {
            return getSlotLimit(slot);
        }
        return Math.min(getSlotLimit(slot), itemLimit);
    }

    /**
     * {@code ItemStackHandler} only refuses 0: a negative amount overflows the count of a full int slot and empties it.
     */
    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public LargeItemStackTransfer copy() {
        var copied = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            copied.set(i, stacks.get(i).copy());
        }
        var transfer = new LargeItemStackTransfer(copied, slotLimit, ignoreItemStackLimit);
        transfer.setFilter(filter);
        return transfer;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var items = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            var stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            var slotTag = new CompoundTag();
            slotTag.putInt("Slot", i);
            if (stack.getCount() > Item.ABSOLUTE_MAX_STACK_SIZE) {
                var itemTag = (CompoundTag) stack.copyWithCount(1).save(provider, slotTag);
                itemTag.putInt(LARGE_COUNT, stack.getCount());
                items.add(itemTag);
            } else {
                items.add(stack.save(provider, slotTag));
            }
        }
        var nbt = new CompoundTag();
        nbt.put("Items", items);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        var items = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            var itemTag = items.getCompound(i);
            var slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= stacks.size()) continue;
            ItemStack.parse(provider, itemTag).ifPresent(stack -> {
                if (itemTag.contains(LARGE_COUNT, Tag.TAG_INT)) {
                    stack.setCount(itemTag.getInt(LARGE_COUNT));
                }
                stacks.set(slot, stack);
            });
        }
        onLoad();
    }
}

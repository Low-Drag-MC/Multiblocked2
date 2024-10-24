package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ItemSlotCapabilityTrait extends SimpleCapabilityTrait<IItemHandler, Ingredient> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ItemSlotCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final ItemStackTransfer storage;
    private Boolean isEmpty;

    public ItemSlotCapabilityTrait(MBDMachine machine, ItemSlotCapabilityTraitDefinition definition) {
        super(machine, definition);
        this.storage = createStorage();
        this.storage.setOnContentsChanged(this::onContentsChanged);
    }

    /**
     * pop storage to the world.
     */
    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        var level = getMachine().getLevel();
        var pos = getMachine().getPos();
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack stackInSlot = storage.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                storage.setStackInSlot(i, ItemStack.EMPTY);
                storage.onContentsChanged();
                Block.popResource(level, pos, stackInSlot);
            }
        }
    }

    @Override
    public ItemSlotCapabilityTraitDefinition getDefinition() {
        return (ItemSlotCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        if (storage.getSlots() > 0) {
            this.storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 32));
        }
    }

    protected ItemStackTransfer createStorage() {
        var transfer = new ItemStackTransfer(getDefinition().getSlotSize()) {
            @Override
            public int getSlotLimit(int slot) {
                return getDefinition().getSlotLimit();
            }
        };
        if (getDefinition().getItemFilterSettings().isEnable()) {
            transfer.setFilter(getDefinition().getItemFilterSettings()::test);
        }
        return transfer;
    }

    public void onContentsChanged() {
        isEmpty = null;
        notifyListeners();
    }

    @Override
    public List<Ingredient> handleRecipeInner(IO io, MBDRecipe recipe, List<Ingredient> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        var capability = simulate ? storage.copy() : storage;
        Iterator<Ingredient> iterator = left.iterator();
        if (io == IO.IN) {
            while (iterator.hasNext()) {
                Ingredient ingredient = iterator.next();
                SLOT_LOOKUP:
                for (int i = 0; i < capability.getSlots(); i++) {
                    ItemStack itemStack = capability.getStackInSlot(i);
                    //Does not look like a good implementation, but I think it's at least equal to vanilla Ingredient::test
                    if (ingredient.test(itemStack)) {
                        ItemStack[] ingredientStacks = ingredient.getItems();
                        for (ItemStack ingredientStack : ingredientStacks) {
                            if (ingredientStack.is(itemStack.getItem())) {
                                ItemStack extracted = capability.extractItem(i, ingredientStack.getCount(), false);
                                ingredientStack.setCount(ingredientStack.getCount() - extracted.getCount());
                                if (ingredientStack.isEmpty()) {
                                    iterator.remove();
                                    break SLOT_LOOKUP;
                                }
                            }
                        }
                    }
                }
            }
        } else if (io == IO.OUT) {
            while (iterator.hasNext()) {
                Ingredient ingredient = iterator.next();
                var items = ingredient.getItems();
                if (items.length == 0) {
                    iterator.remove();
                    continue;
                }
                if (items.length == 1) {
                    ItemStack output = items[0];
                    if (!output.isEmpty()) {
                        for (int i = 0; i < capability.getSlots(); i++) {
                            ItemStack leftStack = capability.insertItem(i, output.copy(), false);
                            output.setCount(leftStack.getCount());
                            if (output.isEmpty()) break;
                        }
                    }
                    if (output.isEmpty()) iterator.remove();
                } else { // random output
                    var shuffledItems = Arrays.asList(items);
                    Collections.shuffle(shuffledItems);
                    // find index
                    var index = -1;
                    for (int i = 0; i < shuffledItems.size(); i++) {
                        ItemStack output = shuffledItems.get(i);
                        if (!output.isEmpty()) {
                            for (int slot = 0; i < capability.getSlots(); i++) {
                                ItemStack leftStack = capability.insertItem(slot, output.copy(), true);
                                output.setCount(leftStack.getCount());
                                if (output.isEmpty()) break;
                            }
                        }
                        if (output.isEmpty()) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        if (!simulate) {
                            ItemStack output = shuffledItems.get(index);
                            for (int slot = 0; slot < capability.getSlots(); slot++) {
                                ItemStack leftStack = capability.insertItem(slot, output.copy(), false);
                                output.setCount(leftStack.getCount());
                                if (output.isEmpty()) break;
                            }
                        }
                        iterator.remove();
                    }
                }
            }
        }
        return left.isEmpty() ? null : left;
    }

    public boolean isEmpty() {
        if (isEmpty == null) {
            isEmpty = true;
            for (int i = 0; i < storage.getSlots(); i++) {
                if (!storage.getStackInSlot(i).isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    @Override
    public IItemHandler getCapContent(IO capbilityIO) {
        return new ItemHandlerWrapper(this.storage, capbilityIO);
    }

    @Override
    public IItemHandler mergeContents(List<IItemHandler> contents) {
        return new ItemHandlerList(contents.toArray(new IItemHandler[0]));
    }

    //////////////////////////////////////
    //********     AUTO IO     *********//
    //////////////////////////////////////
    @Override
    public void serverTick() {
        var timer = getMachine().getOffsetTimer();
        var autoInput = getDefinition().getAutoInput();
        var autoOutput = getDefinition().getAutoOutput();
        if (autoInput.isEnable() && timer % autoInput.getInterval() == 0) {
            var items = getMachine().getLevel().getEntitiesOfClass(ItemEntity.class,
                    autoInput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos()),
                            EntitySelector.ENTITY_STILL_ALIVE);
            var leftCount = autoInput.getSpeed();
            for (ItemEntity itemEntity : items) {
                if (leftCount <= 0) break;
                var stored = itemEntity.getItem().copy();
                var remaining = stored.copyWithCount(Math.min(leftCount, stored.getCount()));
                var inserted = 0;
                for (int i = 0; i < storage.getSlots(); i++) {
                    var beforeCount = remaining.getCount();
                    remaining = storage.insertItem(i, remaining, false);
                    if (remaining.getCount() < beforeCount) {
                        inserted += beforeCount - remaining.getCount();
                        if (remaining.isEmpty()) break;
                    }
                }
                if (inserted > 0) {
                    stored.shrink(inserted);
                    if (stored.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(stored);
                    }
                }
                leftCount -= inserted;
            }
        }
        if (autoOutput.isEnable() && timer % autoOutput.getInterval() == 0) {
            var leftCount = autoOutput.getSpeed();
            var range = autoOutput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos());
            for (int i = 0; i < storage.getSlots(); i++) {
                if (leftCount <= 0) break;
                var stored = storage.getStackInSlot(i);
                if (stored.isEmpty()) continue;
                var pop = stored.copyWithCount(Math.min(leftCount, stored.getCount()));
                leftCount -= pop.getCount();
                storage.extractItem(i, pop.getCount(), false);
                // drop items
                var level = getMachine().getLevel();
                var d0 = (double) EntityType.ITEM.getHeight() / 2.0D;
                var x = level.random.nextFloat() * range.getXsize() + range.minX;
                var y = level.random.nextFloat() * range.getYsize() + range.minY - d0;
                var z = level.random.nextFloat() * range.getZsize() + range.minZ;
                var itemEntity = new ItemEntity(level, x, y, z, pop);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
    }

}

package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

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
                ItemStack output = items[0];
                if (!output.isEmpty()) {
                    for (int i = 0; i < capability.getSlots(); i++) {
                        ItemStack leftStack = capability.insertItem(i, output.copy(), false);
                        output.setCount(leftStack.getCount());
                        if (output.isEmpty()) break;
                    }
                }
                if (output.isEmpty()) iterator.remove();
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

}

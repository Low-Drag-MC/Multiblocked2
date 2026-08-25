package com.lowdragmc.mbd2.common.trait.item;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.ItemDurabilityRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoWorldIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class ItemSlotCapabilityTrait extends SimpleCapabilityTrait<IItemHandler, @Nullable Direction> implements IAutoIOTrait {
    @Persisted
    @DescSynced
    @ConditionalSynced(methodName = "shouldSyncStorage")
    public final ItemStackTransfer storage;

    private final ItemRecipeHandler itemRecipeHandler = new ItemRecipeHandler();
    private final ItemDurabilityRecipeHandler durabilityRecipeHandler = new ItemDurabilityRecipeHandler();

    // per-machine overrides of the values authored on the definition
    public final RuntimeAutoIO autoIO =
            new RuntimeAutoIO(runtimeValues, "auto_io", () -> getDefinition().getAutoIO());
    public final RuntimeAutoWorldIO autoWorldInput =
            new RuntimeAutoWorldIO(runtimeValues, "auto_world_input", () -> getDefinition().getAutoWorldInput());
    public final RuntimeAutoWorldIO autoWorldOutput =
            new RuntimeAutoWorldIO(runtimeValues, "auto_world_output", () -> getDefinition().getAutoWorldOutput());
    public final RuntimeValue<Boolean> allowSameItems =
            runtimeValues.ofBool("allow_same_items", () -> getDefinition().isAllowSameItems())
            .onChanged(() -> {
                // the value is baked into the wrapper handed out at capability-resolution time, and a
                // neighbour's BlockCapabilityCache keeps that wrapper until the position is invalidated
                getMachine().invalidateCapabilities();
                getMachine().notifyBlockUpdate();
            });
    public final RuntimeValue<Integer> slotLimit =
            runtimeValues.ofInt("slot_limit", () -> getDefinition().getSlotLimit());

    // runtime
    private final Random random = new Random();
    private Boolean isEmpty;
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IItemHandler, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public ItemSlotCapabilityTrait(MBDMachine machine, ItemSlotCapabilityTraitDefinition definition) {
        super(machine, definition);
        this.storage = createStorage();
        this.storage.setOnContentsChanged(this::onContentsChanged);
    }

    @Override
    public void onMachineDrop(Entity entity, List<ItemStack> drops) {
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack stackInSlot = storage.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                drops.add(stackInSlot);
            }
        }
    }

    @Override
    public ItemSlotCapabilityTraitDefinition getDefinition() {
        return (ItemSlotCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public IItemHandler getCapContent(IO capabilityIO) {
        return new ItemHandlerWrapper(storage, capabilityIO, allowSameItems.get());
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
                return slotLimit.get();
            }
        };
        // Read the filter live instead of baking the definition's object in at construction. handleAutoIO
        // always read it live, so a filter toggled after the machine was placed — an editor edit plus
        // /mbd2 reload_machine_projects — applied to auto IO but not to the slots themselves.
        transfer.setFilter(stack -> {
            var filter = getDefinition().getItemFilterSettings();
            return !filter.isEnable() || filter.test(stack);
        });
        return transfer;
    }

    public void onContentsChanged() {
        isEmpty = null;
        notifyListeners();
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
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(itemRecipeHandler, durabilityRecipeHandler);
    }

    //////////////////////////////////////
    //********     AUTO IO     *********//
    //////////////////////////////////////

    @Override
    public RuntimeAutoIO getRuntimeAutoIO() {
        return autoIO;
    }

    @Override
    public void serverTick() {
        IAutoIOTrait.super.serverTick();
        var timer = getMachine().getOffsetTimer();
        var autoInput = autoWorldInput;
        var autoOutput = autoWorldOutput;
        if (autoInput.enable.get() && timer % autoInput.intervalTicks() == 0) {
            var items = getMachine().getLevel().getEntitiesOfClass(ItemEntity.class,
                    autoInput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos()),
                            EntitySelector.ENTITY_STILL_ALIVE);
            var leftCount = autoInput.speed.get();
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
        if (autoOutput.enable.get() && timer % autoOutput.intervalTicks() == 0) {
            var leftCount = autoOutput.speed.get();
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

    @Nonnull
    public BlockCapabilityCache<IItemHandler, @Nullable Direction> getNearbyCache(ServerLevel serverLevel,
                                                                                  BlockPos pos,
                                                                                  @Nonnull Direction side) {
        return nearbyCache.computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK,
                        serverLevel,
                        pos, direction
                ));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
            var nearby = getNearbyCache(serverLevel, port.relative(side), side.getOpposite()).getCapability();
            if (nearby == null) return;
            if (io.support(IO.IN)) {
                var source = nearby;

                Predicate<ItemStack> filter = getDefinition().getItemFilterSettings().isEnable() ?
                        getDefinition().getItemFilterSettings() : Predicates.alwaysTrue();

                int maxAmount = Integer.MAX_VALUE;

                for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                    var sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
                    if (sourceStack.isEmpty() || !filter.test(sourceStack)) {
                        continue;
                    }
                    var remainder = ItemHandlerHelper.insertItemStacked(storage, sourceStack, true);
                    int amountToInsert = sourceStack.getCount() - remainder.getCount();
                    if (amountToInsert > 0) {
                        sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                        ItemHandlerHelper.insertItemStacked(storage, sourceStack, false);
                        maxAmount -= Math.min(maxAmount, amountToInsert);
                    }
                    if (maxAmount <= 0) return;
                }
            }
            if (io.support(IO.OUT) && !isEmpty()){
                var target = nearby;

                int maxAmount = Integer.MAX_VALUE;

                for (int srcIndex = 0; srcIndex < storage.getSlots(); srcIndex++) {
                    var sourceStack = storage.extractItem(srcIndex, Integer.MAX_VALUE, true);
                    if (sourceStack.isEmpty()) {
                        continue;
                    }
                    var remainder = ItemHandlerHelper.insertItemStacked(target, sourceStack, true);
                    int amountToInsert = sourceStack.getCount() - remainder.getCount();
                    if (amountToInsert > 0) {
                        sourceStack = storage.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                        ItemHandlerHelper.insertItemStacked(target, sourceStack, false);
                        maxAmount -= Math.min(maxAmount, amountToInsert);
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }

    public boolean shouldSyncStorage(ItemStackTransfer value) {
        return getDefinition().getItemRendererSettings().isEnable();
    }

    public class ItemRecipeHandler extends RecipeHandlerTrait<SizedIngredient> {

        protected ItemRecipeHandler() {
            super(ItemSlotCapabilityTrait.this, ItemRecipeCapability.CAP);
        }

        @Override
        public @Nullable List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var container = simulate ? storage.copy() : storage;
            var result = new ArrayList<SizedIngredient>();
            var iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    var sizedIngredient = iterator.next();
                    var need = sizedIngredient.count();
                    for (int i = 0; i < container.getSlots(); i++) {
                        var itemStack = container.getStackInSlot(i);
                        if (sizedIngredient.ingredient().test(itemStack)) {
                            var extracted = container.extractItem(i, need, false);
                            need -= extracted.getCount();
                            if (need <= 0) {
                                break;
                            }
                        }
                    }
                    if (need > 0) {
                        if (need == sizedIngredient.count()) {
                            result.add(sizedIngredient);
                        } else {
                            result.add(new SizedIngredient(sizedIngredient.ingredient(), need));
                        }
                    }
                }
            } else if (io == IO.OUT) {
                while (iterator.hasNext()) {
                    var sizedIngredient = iterator.next();
                    var items = sizedIngredient.getItems();
                    if (items.length == 0) {
                        continue;
                    }
                    if (items.length == 1) {
                        ItemStack output = items[0];
                        var leftCount = sizedIngredient.count();
                        if (leftCount > 0) {
                            for (int i = 0; i < container.getSlots(); i++) {
                                var remainder = container.insertItem(i, output.copyWithCount(leftCount), false);
                                leftCount = remainder.getCount();
                                if (leftCount == 0) break;
                            }
                        }
                        if (leftCount > 0) {
                            result.add(leftCount == sizedIngredient.count() ?
                                    sizedIngredient :
                                    new SizedIngredient(sizedIngredient.ingredient(), leftCount));
                        }
                    } else { // random output
                        var shuffledItems = Arrays.asList(Arrays.copyOf(items, items.length));
                        random.setSeed(getMachine().getOffsetTimer());
                        Collections.shuffle(shuffledItems, random);

                        var probe = container.copy();

                        // find index
                        var index = -1;
                        for (int i = 0; i < shuffledItems.size(); i++) {
                            var output = shuffledItems.get(i).copy();
                            var leftCount = sizedIngredient.count();
                            if (leftCount > 0) {
                                for (int slot = 0; slot < probe.getSlots(); slot++) {
                                    var leftStack = probe.insertItem(slot, output.copyWithCount(leftCount), true);
                                    leftCount = leftStack.getCount();
                                    if (leftCount == 0) break;
                                }
                            }
                            if (leftCount == 0) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            if (!simulate) {
                                var output = shuffledItems.get(index);
                                var leftCount = sizedIngredient.count();
                                for (int slot = 0; slot < container.getSlots(); slot++) {
                                    var leftStack = container.insertItem(slot, output.copyWithCount(leftCount), false);
                                    leftCount = leftStack.getCount();
                                    if (leftCount == 0) break;
                                }
                            }
                        } else {
                            result.add(sizedIngredient);
                        }
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }
    }

    public class ItemDurabilityRecipeHandler extends RecipeHandlerTrait<SizedIngredient> {


        protected ItemDurabilityRecipeHandler() {
            super(ItemSlotCapabilityTrait.this, ItemDurabilityRecipeCapability.CAP);
        }

        @Override
        public List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var container = simulate ? storage.copy() : storage;
            var it = left.listIterator();
            while (it.hasNext()) {
                var sized = it.next();
                int need = sized.count();
                if (need <= 0) {
                    it.remove();
                    continue;
                }

                var remaining = need;
                for (int slot = 0; slot < container.getSlots() && remaining > 0; slot++) {
                    ItemStack stack = container.getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
                    if (!stack.isDamageableItem()) continue;
                    // For durability recipes, sized.count() is the damage amount to consume,
                    // not the stack count required. Test only the ingredient predicate.
                    if (!sized.ingredient().test(stack)) continue;

                    int damage = stack.getDamageValue();
                    int maxDamage = stack.getMaxDamage();

                    int transferable;
                    if (io == IO.IN) {
                        int room = maxDamage - damage;
                        transferable = Math.min(room, remaining);
                        if (transferable <= 0) continue;

                        ItemStack copy = stack.copy();
                        copy.setDamageValue(damage + transferable);
                        container.setStackInSlot(slot, copy);

                    } else { // IO.OUT
                        transferable = Math.min(damage, remaining);
                        if (transferable <= 0) continue;

                        ItemStack copy = stack.copy();
                        copy.setDamageValue(damage - transferable);
                        container.setStackInSlot(slot, copy);
                    }

                    remaining -= transferable;
                }

                if (remaining <= 0) {
                    it.remove();
                } else if (remaining != need) {
                    it.set(new SizedIngredient(sized.ingredient(), remaining));
                }
            }
            return left.isEmpty() ? null : left;
        }
    }
}

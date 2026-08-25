package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.util.ConfigInventory;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class MEInterfaceTrait extends SimpleCapabilityTrait<MEStorage, @Nullable Direction> implements IGridConnectedBlockEntity, InterfaceLogicHost, PatternContainer {
    private final Random random = new Random();

    @Persisted
    private final SerializableManagedGridNode mainNode;
    @Persisted
    private final SerializableInterfaceLogic interfaceLogic;
    private final ItemRecipeHandler itemRecipeHandler = new ItemRecipeHandler();
    private final FluidRecipeHandler fluidRecipeHandler = new FluidRecipeHandler();

    public MEInterfaceTrait(MBDMachine machine, MEInterfaceTraitDefinition definition) {
        super(machine, definition);
        mainNode = createMainNode();
        interfaceLogic = createLogic();
        applyCapacities();
    }

    protected SerializableManagedGridNode createMainNode() {
        return (SerializableManagedGridNode) new SerializableManagedGridNode(this, (nodeOwner, node) -> nodeOwner.interfaceLogic.gridChanged())
                .setVisualRepresentation(getMachine().getDropItem())
                .setInWorldNode(true)
                .setTagName("proxy");
    }

    protected SerializableInterfaceLogic createLogic() {
        return new SerializableInterfaceLogic(getMainNode(), this, getMachine().getDropItem().getItem(), getDefinition().getSlotSize() * 2);
    }

    @Override
    public MEInterfaceTraitDefinition getDefinition() {
        return (MEInterfaceTraitDefinition) super.getDefinition();
    }

    @Override
    public BlockEntity getBlockEntity() {
        return getMachine().getHolder();
    }

    @Override
    public void saveChanges() {
        onchange();
    }

    public void onchange() {
        getMachine().markDirty();
        notifyListeners();
    }

    public void applyCapacities() {
        var definition = getDefinition();
        interfaceLogic.getStorage().setCapacity(AEKeyType.items(), definition.getItemCapacity());
        interfaceLogic.getStorage().setCapacity(AEKeyType.fluids(), definition.getFluidCapacity());
        interfaceLogic.getConfig().setCapacity(AEKeyType.items(), definition.getItemCapacity());
        interfaceLogic.getConfig().setCapacity(AEKeyType.fluids(), definition.getFluidCapacity());
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        if (getCapabilityIO(dir) == IO.NONE) return null;
        return IGridConnectedBlockEntity.super.getGridNode(dir);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (getMainNode().hasGridBooted()) {
            this.interfaceLogic.notifyNeighbors();
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getMachine().getDropItem();
    }

    @Override
    public void onMachineDrop(Entity entity, List<ItemStack> drops) {
        this.interfaceLogic.addDrops(drops);
        this.interfaceLogic.clearContent();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return this.interfaceLogic.getCableConnectionType(dir);
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(itemRecipeHandler, fluidRecipeHandler);
    }

    @Override
    public @Nullable MEStorage getCapContent(IO capabilityIO) {
        return capabilityIO != IO.NONE ? interfaceLogic.getInventory() : null;
    }

    public @Nullable GenericInternalInventory getGenericInternalInventory(IO capabilityIO) {
        return capabilityIO != IO.NONE ? interfaceLogic.getStorage() : null;
    }

    public IInWorldGridNodeHost getGridNodeHost() {
        return this;
    }

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public boolean isVisibleInTerminal() {
        return findPatternProviderTrait() != null;
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        var patternProviderTrait = findPatternProviderTrait();
        return patternProviderTrait == null ? InternalInventory.empty() : patternProviderTrait.getPatternProviderLogic().getPatternInv();
    }

    @Override
    public long getTerminalSortOrder() {
        var patternProviderTrait = findPatternProviderTrait();
        return patternProviderTrait == null ? 0 : patternProviderTrait.getPatternProviderLogic().getSortValue();
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        var patternProviderTrait = findPatternProviderTrait();
        return patternProviderTrait == null ? PatternContainerGroup.nothing() : patternProviderTrait.getTerminalGroup();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.getMainNode().destroy();
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        applyCapacities();
        if (getMachine().getLevel() instanceof ServerLevel serverLevel && !this.getMainNode().isReady()) {
            serverLevel.getServer().tell(new TickTask(0, () -> this.getMainNode().create(serverLevel, getBlockEntity().getBlockPos())));
        }
    }

    @Override
    public void onMachineUnLoad() {
        super.onMachineUnLoad();
        this.getMainNode().destroy();
    }

    private ConfigInventory getInterfaceStorage() {
        return interfaceLogic.getStorage();
    }

    private @Nullable MEPatternProviderTrait findPatternProviderTrait() {
        for (var trait : getMachine().getAdditionalTraits()) {
            if (trait instanceof MEPatternProviderTrait patternProviderTrait) {
                return patternProviderTrait;
            }
        }
        return null;
    }

    public class ItemRecipeHandler extends RecipeHandlerTrait<SizedIngredient> {
        protected ItemRecipeHandler() {
            super(MEInterfaceTrait.this, ItemRecipeCapability.CAP);
        }

        protected IItemHandlerModifiable getSafeStorage() {
            var source = getInterfaceStorage();
            var transfer = new ItemStackTransfer(source.size() / 2);
            for (int i = 0; i < transfer.getSlots(); i++) {
                var stack = source.getStack(i * 2);
                if (stack != null && stack.what() instanceof AEItemKey itemKey) {
                    transfer.setStackInSlot(i, itemKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount())), false);
                }
            }
            return transfer;
        }

        protected List<IItemHandlerModifiable> getStorage() {
            var source = getInterfaceStorage();
            List<IItemHandlerModifiable> handlers = new ArrayList<>();
            for (int i = 0; i < source.size() / 2; i++) {
                handlers.add(AEInterfaceSlot.createAEItemHandler(source, i * 2));
            }
            return handlers;
        }

        @Override
        public List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var result = new ArrayList<SizedIngredient>();
            if (io == IO.IN) {
                var capability = simulate ? getSafeStorage() : null;
                for (var sizedIngredient : left) {
                    var need = sizedIngredient.count();
                    if (simulate) {
                        need = consumeFromHandler(capability, sizedIngredient, need, false);
                    } else {
                        for (var handler : getStorage()) {
                            need = consumeFromHandler(handler, sizedIngredient, need, false);
                            if (need <= 0) break;
                        }
                    }
                    if (need > 0) {
                        result.add(need == sizedIngredient.count() ? sizedIngredient : new SizedIngredient(sizedIngredient.ingredient(), need));
                    }
                }
            } else if (io == IO.OUT) {
                List<IItemHandler> handlers = new ArrayList<>();
                if (simulate) {
                    handlers.add(getSafeStorage());
                } else {
                    handlers.addAll(getStorage());
                }
                for (var sizedIngredient : left) {
                    var items = sizedIngredient.getItems();
                    if (items.length == 0) continue;
                    if (items.length == 1) {
                        var output = items[0].copyWithCount(sizedIngredient.count());
                        for (var handler : handlers) {
                            output = insertIntoHandler(handler, output, false);
                            if (output.isEmpty()) break;
                        }
                        if (!output.isEmpty()) {
                            result.add(output.getCount() == sizedIngredient.count() ? sizedIngredient : new SizedIngredient(sizedIngredient.ingredient(), output.getCount()));
                        }
                    } else {
                        var shuffledItems = Arrays.asList(Arrays.copyOf(items, items.length));
                        random.setSeed(getMachine().getOffsetTimer());
                        Collections.shuffle(shuffledItems, random);
                        var index = -1;
                        for (int i = 0; i < shuffledItems.size(); i++) {
                            var output = shuffledItems.get(i).copyWithCount(sizedIngredient.count());
                            var probe = copyItemHandlers(handlers);
                            output = insertIntoHandlers(probe, output);
                            if (output.isEmpty()) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            insertIntoHandlers(handlers, shuffledItems.get(index).copyWithCount(sizedIngredient.count()));
                        } else {
                            result.add(sizedIngredient);
                        }
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }

        private int consumeFromHandler(IItemHandler handler, SizedIngredient ingredient, int need, boolean simulate) {
            for (int slot = 0; slot < handler.getSlots() && need > 0; slot++) {
                var itemStack = handler.getStackInSlot(slot);
                if (itemStack.isEmpty() || !ingredient.ingredient().test(itemStack)) continue;
                var extracted = handler.extractItem(slot, need, simulate);
                need -= extracted.getCount();
            }
            return need;
        }

        private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack, boolean simulate) {
            var leftStack = stack;
            for (int slot = 0; slot < handler.getSlots() && !leftStack.isEmpty(); slot++) {
                leftStack = handler.insertItem(slot, leftStack, simulate);
            }
            return leftStack;
        }

        private ItemStack insertIntoHandlers(List<IItemHandler> handlers, ItemStack stack) {
            var leftStack = stack;
            for (var handler : handlers) {
                leftStack = insertIntoHandler(handler, leftStack, false);
                if (leftStack.isEmpty()) break;
            }
            return leftStack;
        }

        private List<IItemHandler> copyItemHandlers(List<IItemHandler> handlers) {
            var copy = new ItemStackTransfer(handlers.stream().mapToInt(IItemHandler::getSlots).sum());
            var targetSlot = 0;
            for (var handler : handlers) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    copy.setStackInSlot(targetSlot++, handler.getStackInSlot(slot).copy(), false);
                }
            }
            return List.of(copy);
        }
    }

    public class FluidRecipeHandler extends RecipeHandlerTrait<SizedFluidIngredient> {
        protected FluidRecipeHandler() {
            super(MEInterfaceTrait.this, FluidRecipeCapability.CAP);
        }

        protected List<IFluidHandler> getSafeStorage() {
            var source = getInterfaceStorage();
            List<IFluidHandler> storages = new ArrayList<>();
            for (int i = 0; i < source.size() / 2; i++) {
                var transfer = new FluidStorage((int) Math.min(Integer.MAX_VALUE, source.getCapacity(AEKeyType.fluids())));
                var stack = source.getStack(i * 2 + 1);
                if (stack != null && stack.what() instanceof AEFluidKey fluidKey) {
                    transfer.setFluidInTank(fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount())), false);
                }
                storages.add(transfer);
            }
            return storages;
        }

        protected List<IFluidHandler> getStorage() {
            var source = getInterfaceStorage();
            List<IFluidHandler> storages = new ArrayList<>();
            for (int i = 0; i < source.size() / 2; i++) {
                storages.add(AEInterfaceSlot.createAEFluidHandler(source, i * 2 + 1));
            }
            return storages;
        }

        @Override
        public List<SizedFluidIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedFluidIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var capabilities = simulate ? getSafeStorage() : getStorage();
            var result = new ArrayList<SizedFluidIngredient>();
            if (io == IO.IN) {
                for (var sizedIngredient : left) {
                    var need = sizedIngredient.amount();
                    for (var capability : capabilities) {
                        for (int i = 0; i < capability.getTanks() && need > 0; i++) {
                            var stored = capability.getFluidInTank(i);
                            if (stored.isEmpty() || !sizedIngredient.ingredient().test(stored)) continue;
                            var toDrain = stored.copyWithAmount(need);
                            var drained = capability.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
                            need -= drained.getAmount();
                        }
                        if (need <= 0) break;
                    }
                    if (need > 0) {
                        result.add(need == sizedIngredient.amount() ? sizedIngredient : new SizedFluidIngredient(sizedIngredient.ingredient(), need));
                    }
                }
            } else if (io == IO.OUT) {
                for (var sizedIngredient : left) {
                    var fluids = sizedIngredient.getFluids();
                    if (fluids.length == 0) continue;
                    if (fluids.length == 1) {
                        var output = fluids[0].copyWithAmount(sizedIngredient.amount());
                        var leftAmount = insertFluid(capabilities, output);
                        if (leftAmount > 0) {
                            result.add(leftAmount == sizedIngredient.amount() ? sizedIngredient : new SizedFluidIngredient(sizedIngredient.ingredient(), leftAmount));
                        }
                    } else {
                        var shuffledFluids = Arrays.asList(Arrays.copyOf(fluids, fluids.length));
                        random.setSeed(getMachine().getOffsetTimer());
                        Collections.shuffle(shuffledFluids, random);
                        var index = -1;
                        for (int i = 0; i < shuffledFluids.size(); i++) {
                            var probe = copyFluidHandlers(capabilities);
                            var output = shuffledFluids.get(i).copyWithAmount(sizedIngredient.amount());
                            if (insertFluid(probe, output) <= 0) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            insertFluid(capabilities, shuffledFluids.get(index).copyWithAmount(sizedIngredient.amount()));
                        } else {
                            result.add(sizedIngredient);
                        }
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }

        private int insertFluid(List<IFluidHandler> handlers, FluidStack output) {
            var leftAmount = output.getAmount();
            for (var handler : handlers) {
                if (leftAmount <= 0) break;
                var filled = handler.fill(output.copyWithAmount(leftAmount), IFluidHandler.FluidAction.EXECUTE);
                leftAmount -= filled;
            }
            return leftAmount;
        }

        private List<IFluidHandler> copyFluidHandlers(List<IFluidHandler> handlers) {
            var result = new ArrayList<IFluidHandler>();
            for (var handler : handlers) {
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    var storage = new FluidStorage(handler.getTankCapacity(tank));
                    storage.setFluidInTank(handler.getFluidInTank(tank).copy(), false);
                    result.add(storage);
                }
            }
            return result;
        }
    }
}

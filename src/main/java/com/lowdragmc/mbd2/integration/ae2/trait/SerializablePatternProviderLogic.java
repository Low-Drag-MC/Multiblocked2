package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.settings.TickRates;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.me.helpers.MachineSource;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SerializablePatternProviderLogic implements INBTSerializable<CompoundTag>, IContentChangeAware, InternalInventoryHost, ICraftingProvider {
    private static final String NBT_PATTERNS = "patterns";
    private static final String NBT_STORAGE = "storage";
    private static final String NBT_RETURN_INV = "returnInv";

    private Runnable onContentsChanged = Runnables.doNothing();

    private final MEPatternProviderTrait host;
    private final IManagedGridNode mainNode;
    private IManagedGridNode serviceNode;
    private final IActionSource actionSource;
    private final Ticker ticker = new Ticker();
    private final AppEngInternalInventory patternInventory;
    private final ConfigInventory storage;
    private final ConfigInventory returnInventory;
    private final List<IPatternDetails> patterns = new ArrayList<>();

    public SerializablePatternProviderLogic(IManagedGridNode mainNode, MEPatternProviderTrait host, int patternSize, int storageSize) {
        this.host = host;
        this.mainNode = mainNode
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, ticker)
                .addService(ICraftingProvider.class, this);
        this.serviceNode = this.mainNode;
        this.actionSource = new MachineSource(() -> serviceNode.getNode());
        this.patternInventory = new AppEngInternalInventory(this, patternSize);
        this.storage = ConfigInventory.storage(storageSize)
                .changeListener(this::onStorageChanged)
                .build();
        this.returnInventory = ConfigInventory.storage(storageSize)
                .changeListener(this::onReturnInventoryChanged)
                .build();
    }

    public void attachToSharedNode(IManagedGridNode sharedNode) {
        if (serviceNode == sharedNode) return;
        sharedNode.addService(IGridTickable.class, ticker);
        sharedNode.addService(ICraftingProvider.class, this);
        serviceNode = sharedNode;
        if (mainNode.isReady()) {
            mainNode.destroy();
        }
        updatePatterns();
        alertDevice();
    }

    public boolean isUsingSharedNode() {
        return serviceNode != mainNode;
    }

    public @Nullable IGrid getGrid() {
        return serviceNode.getGrid();
    }

    public long getSortValue() {
        var pos = host.getBlockEntity().getBlockPos();
        return ((long) pos.getZ() << 24) ^ ((long) pos.getX() << 8) ^ pos.getY();
    }

    public void applyCapacities(int itemCapacity, int fluidCapacity) {
        applyCapacities(storage, itemCapacity, fluidCapacity);
        applyCapacities(returnInventory, itemCapacity, fluidCapacity);
    }

    private static void applyCapacities(GenericStackInv inventory, int itemCapacity, int fluidCapacity) {
        inventory.setCapacity(AEKeyType.items(), itemCapacity);
        inventory.setCapacity(AEKeyType.fluids(), fluidCapacity);
    }

    private void onStorageChanged() {
        host.onchange();
    }

    private void onReturnInventoryChanged() {
        alertDevice();
        host.onchange();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        patternInventory.writeToNBT(tag, NBT_PATTERNS, provider);
        storage.writeToChildTag(tag, NBT_STORAGE, provider);
        returnInventory.writeToChildTag(tag, NBT_RETURN_INV, provider);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        patternInventory.readFromNBT(nbt, NBT_PATTERNS, provider);
        storage.readFromChildTag(nbt, NBT_STORAGE, provider);
        returnInventory.readFromChildTag(nbt, NBT_RETURN_INV, provider);
        updatePatterns();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        host.onchange();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        saveChangedInventory(inv);
        updatePatterns();
    }

    @Override
    public boolean isClientSide() {
        var level = host.getBlockEntity().getLevel();
        return level == null || level.isClientSide();
    }

    public InternalInventory getPatternInv() {
        return patternInventory;
    }

    public void updatePatterns() {
        patterns.clear();
        var level = host.getBlockEntity().getLevel();
        for (var stack : patternInventory) {
            var details = PatternDetailsHelper.decodePattern(stack, level);
            if (details != null) {
                patterns.add(details);
            }
        }
        ICraftingProvider.requestUpdate(serviceNode);
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!serviceNode.isActive() || !patterns.contains(patternDetails) || !patternDetails.supportsPushInputsToExternalInventory()) {
            return false;
        }
        if (!insertPatternInputs(patternDetails, inputHolder, Actionable.SIMULATE)) {
            return false;
        }
        insertPatternInputs(patternDetails, inputHolder, Actionable.MODULATE);
        host.onchange();
        return true;
    }

    private boolean insertPatternInputs(IPatternDetails patternDetails, KeyCounter[] inputHolder, Actionable mode) {
        var success = new boolean[] { true };
        patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
            if (!success[0]) return;
            var inserted = insertIntoCache(what, amount, mode);
            if (inserted < amount) {
                success[0] = false;
            }
        });
        return success[0];
    }

    public long insertIntoCache(AEKey what, long amount, Actionable mode) {
        if (amount <= 0) return 0;
        var start = what instanceof AEItemKey ? 0 : what instanceof AEFluidKey ? 1 : 0;
        long inserted = 0;
        for (int slot = start; slot < storage.size() && inserted < amount; slot += 2) {
            inserted += storage.insert(slot, what, amount - inserted, mode);
        }
        return inserted;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    public void onMainNodeStateChanged() {
        if (serviceNode.isActive()) {
            alertDevice();
        }
    }

    private void alertDevice() {
        serviceNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    private boolean hasWorkToDo() {
        return !returnInventory.isEmpty();
    }

    private boolean doWork() {
        if (!serviceNode.isActive()) return false;
        return injectIntoNetwork(returnInventory);
    }

    public boolean returnAllToNetwork() {
        if (!serviceNode.isActive()) return false;
        var changed = injectIntoNetwork(returnInventory) | injectIntoNetwork(storage);
        if (changed) {
            host.onchange();
        }
        return changed;
    }

    private boolean injectIntoNetwork(ConfigInventory inventory) {
        if (!serviceNode.isActive()) return false;
        var network = serviceNode.getGrid().getStorageService().getInventory();
        var changed = false;
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack == null) continue;
            var inserted = network.insert(stack.what(), stack.amount(), Actionable.MODULATE, actionSource);
            if (inserted >= stack.amount()) {
                inventory.setStack(i, null);
            } else if (inserted > 0) {
                inventory.setStack(i, new GenericStack(stack.what(), stack.amount() - inserted));
            }
            changed |= inserted > 0;
        }
        return changed;
    }

    public void addDrops(List<ItemStack> drops) {
        for (var stack : patternInventory) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        addDrops(drops, storage);
        addDrops(drops, returnInventory);
    }

    private void addDrops(List<ItemStack> drops, GenericStackInv inventory) {
        var level = host.getBlockEntity().getLevel();
        var pos = host.getBlockEntity().getBlockPos();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack != null) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    public void clearContent() {
        patternInventory.clear();
        storage.clear();
        returnInventory.clear();
        patterns.clear();
    }

    private class Ticker implements IGridTickable {
        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(TickRates.Interface, !hasWorkToDo());
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!serviceNode.isActive()) {
                return TickRateModulation.SLEEP;
            }
            var didWork = doWork();
            return hasWorkToDo() ? didWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        }
    }
}

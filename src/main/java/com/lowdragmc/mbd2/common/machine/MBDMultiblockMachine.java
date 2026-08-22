package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeHandlerSlotsProxy;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import com.lowdragmc.mbd2.common.trait.IProxyAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.config.ConfigHolder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MBDMultiblockMachine extends MBDMachine implements IMultiController {
    private MultiblockState multiblockState;
    private final List<IMultiPart> parts = new ArrayList<>();
    @Getter
    @DescSynced
    @UpdateListener(methodName = "onPartsUpdated")
    private BlockPos[] partPositions = new BlockPos[0];
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean isFormed;
    @Getter
    protected Set<BlockPos> proxyWhileFormedPositions = new HashSet<>();
    /**
     * Runtime only, rebuilt on every {@link #onStructureFormed()}: the {@link ProxyPartBlock} ports whose
     * predicate asked for auto IO. Those block entities never tick, so the controller drives them itself.
     */
    private final Map<BlockPos, List<ConfigPartSettings.ProxyCapability>> autoIOProxyPorts = new HashMap<>();
    @Getter
    private final Lock patternLock = new ReentrantLock();
    @Getter
    @Setter
    @Persisted
    @Nullable
    private BlockState originalBlock; // original block Before Structure Formed
    // runtime
    @Getter
    private boolean isFormedValid = false;

    public MBDMultiblockMachine(IMachineBlockEntity machineHolder, MultiblockMachineDefinition definition, Object... args) {
        super(machineHolder, definition, args);
    }

    @Deprecated
    public Set<BlockPos> getRenderingDisabledPositions() {
        return proxyWhileFormedPositions;
    }

    /**
     * on machine valid in the chunk.
     * <br>
     * We will add the async pattern checking logic into the next tick.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).addAsyncLogic(this);
        }
    }

    /**
     * on machine invalid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#setRemoved()}.
     */
    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).removeAsyncLogic(this);
        }
    }

    @Override
    public void serverTick() {
        if (isFormed && !isFormedValid && getLevel() instanceof ServerLevel serverLevel) {
            if (checkPatternWithTryLock()) {
                onStructureFormed();
                var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                mwsd.addMapping(getMultiblockState());
                mwsd.removeAsyncLogic(this);
            }
        }
        super.serverTick();
    }

    @Override
    protected void internalServerTick() {
        super.internalServerTick();
        handleProxyPortAutoIO();
    }

    /**
     * Drive the auto IO of every non-MBD block that {@code proxyWhileFormed} turned into a
     * {@link ProxyPartBlock}. Those block entities have no ticker at all, so without this the port
     * exposes the proxied trait but never moves anything (#237). Parts run their own ports from
     * {@link MBDPartMachine#internalServerTick()}, so they are deliberately skipped here.
     */
    private void handleProxyPortAutoIO() {
        // Non-empty only between onStructureFormed and onStructureInvalid, so it doubles as the formed check.
        if (autoIOProxyPorts.isEmpty()) return;
        var level = getLevel();
        if (!(level instanceof ServerLevel)) return;
        var front = getFrontFacing().orElse(Direction.NORTH);
        var timer = getOffsetTimer();
        for (var entry : autoIOProxyPorts.entrySet()) {
            var pos = entry.getKey();
            if (!level.isLoaded(pos)) continue;
            // only drive ports that still proxy us: a shared block may have been claimed by another controller.
            if (level.getBlockEntity(pos) instanceof ProxyPartBlockEntity proxy
                    && getPos().equals(proxy.getControllerPos())) {
                IProxyAutoIOTrait.handleProxyAutoIO(this, entry.getValue(), pos, front, timer);
            }
        }
    }

    @Override
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    /**
     * Called when recipe logic status changed.
     * <br>
     * By default, We will update the machine state to match the recipe logic status.
     * <br>
     * It will also be called when the machine is formed {@link #onStructureFormed()} while the oldStatus is same as the newStatus.
     */
    @Override
    public void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        IMultiController.super.notifyRecipeStatusChanged(oldStatus, newStatus);
        if (isFormed) {
            switch (newStatus) {
                case WORKING -> setMachineState("working");
                case IDLE -> setMachineState("formed");
                case WAITING -> setMachineState("waiting");
                case SUSPEND -> setMachineState("suspend");
            }
        } else {
            setMachineState("base");
        }
        NeoForge.EVENT_BUS.post(new MachineRecipeStatusChangedEvent(this, oldStatus, newStatus).postCustomEvent());
    }

    @Override
    public void updateState(String newValue, String oldValue) {
        super.updateState(newValue, oldValue);
        var level = getLevel();
        if (level == null || proxyWhileFormedPositions.isEmpty()) return;
        for (var pos : proxyWhileFormedPositions) {
            if (level.getBlockEntity(pos) instanceof ProxyPartBlockEntity) {
                var state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 11);
                level.getChunkSource().getLightEngine().checkBlock(pos);
            } else if (IMultiPart.ofPart(level, pos).orElse(null) instanceof MBDPartMachine part) {
                part.notifyBlockUpdate();
                level.getChunkSource().getLightEngine().checkBlock(pos);
            }
        }
    }

    @Override
    public @Nullable MBDRecipe getModifiedRecipe(@Nonnull MBDRecipe recipe) {
        return IMultiController.super.getModifiedRecipe(
                getDefinition().recipeLogicSettings().recipeModifiers().applyModifiers(getRecipeLogic(), recipe));
    }

    @Override
    public ContentModifier getMaxParallel(@Nonnull MBDRecipe recipe) {
        var maxParallel = getDefinition().recipeLogicSettings().recipeModifiers().getMaxParallel(getRecipeLogic(), recipe);
        return maxParallel.merge(IMultiController.super.getMaxParallel(recipe));
    }

    @Override
    public boolean alwaysTryModifyRecipe() {
        return super.alwaysTryModifyRecipe() || IMultiController.super.alwaysTryModifyRecipe();
    }

    @Override
    public boolean beforeWorking(MBDRecipe recipe) {
        if (super.beforeWorking(recipe)) {
            return true;
        }
        return IMultiController.super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        if (super.onWorking()) {
            return true;
        }
        return IMultiController.super.onWorking();
    }

    @Override
    public void onWaiting() {
        super.onWaiting();
        IMultiController.super.onWaiting();
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        IMultiController.super.afterWorking();
    }

    /**
     * Get structure pattern.
     * You can override it to create dynamic patterns.
     */
    @Override
    public BlockPattern getPattern() {
        return getDefinition().getPattern(this);
    }

    /**
     * Get MultiblockState. It records all structure-related information.
     * if it's null, we will create a new one.
     */
    @Override
    @Nonnull
    public MultiblockState getMultiblockState() {
        if (multiblockState == null) {
            multiblockState = new MultiblockState(getLevel(), getPos());
        }
        return multiblockState;
    }

    /**
     * Used for the client side notification, it will be called when parts in the sever side are updated.
     */
    @SuppressWarnings("unused")
    protected void onPartsUpdated(BlockPos[] newValue, BlockPos[] oldValue) {
        parts.clear();
        for (var pos : newValue) {
            IMultiPart.ofPart(getLevel(), pos).ifPresent(parts::add);
        }
    }

    protected void updatePartPositions() {
        this.partPositions = this.parts.isEmpty() ? new BlockPos[0] : this.parts.stream().map(IMachine::getPos).toArray(BlockPos[]::new);
    }

    /**
     * Get all parts
     */
    @Override
    public List<IMultiPart> getParts() {
        // for the client side, when the chunk unloaded
        if (parts.size() != this.partPositions.length) {
            parts.clear();
            for (var pos : this.partPositions) {
                IMultiPart.ofPart(getLevel(), pos).ifPresent(parts::add);
            }
        }
        return this.parts;
    }

    public void setFormed(boolean formed) {
        this.isFormed = formed;
        setMachineState(isFormed ? "formed" : getDefinition().stateMachine().getRootState().name());
    }

    /**
     * Shall we run the recipe logic during the server tick?
     * <br>
     * if the machine has no recipe logic or using the {@link MBDRecipeType#DUMMY}, it will return false.
     * <br>
     * if the controller is not formed, it will return false.
     */
    @Override
    public boolean runRecipeLogic() {
        return super.runRecipeLogic() && isFormed() && isFormedValid();
    }

    /**
     * Initialize the capabilities proxy for recipe logic. see {@link IRecipeCapabilityHolder#getRecipeCapabilitiesProxy()}
     * <br>
     * For a formed multiblock, it will collect all the recipe handlers from all parts.
     */
    @Override
    public void initCapabilitiesProxy() {
        super.initCapabilitiesProxy();
        if (isFormed()) {
            var capabilitiesProxy = getRecipeCapabilitiesProxy();
            Map<Long, IO> ioMap = getMultiblockState().getMatchContext().getOrCreate("ioMap", Long2ObjectMaps::emptyMap);
            Map<Long, Set<String>> slots = getMultiblockState().getMatchContext().getOrDefault("slots", Long2ObjectMaps.emptyMap());
            for (IMultiPart part : getParts()) {
                IO io = ioMap.getOrDefault(part.getPos().asLong(), IO.BOTH);
                Set<String> slotNames = slots.getOrDefault(part.getPos().asLong(), Collections.emptySet());
                if (io == IO.NONE) continue;
                for (var handler : part.getRecipeHandlers()) {
                    // If IO not compatible
                    if (io != IO.BOTH && handler.getHandlerIO() != IO.BOTH && io != handler.getHandlerIO()) continue;
                    var handlerIO = io == IO.BOTH ? handler.getHandlerIO() : io;
                    if (!capabilitiesProxy.contains(handlerIO, handler.getRecipeCapability())) {
                        capabilitiesProxy.put(handlerIO, handler.getRecipeCapability(), new ArrayList<>());
                    }
                    if (slotNames.isEmpty()) {
                        capabilitiesProxy.get(handlerIO, handler.getRecipeCapability()).add(handler);
                    } else {
                        var mergedSlots = new HashSet<>(slotNames);
                        mergedSlots.addAll(handler.getSlotNames());
                        capabilitiesProxy.get(handlerIO, handler.getRecipeCapability()).add(new RecipeHandlerSlotsProxy<>(handler, mergedSlots));
                    }
                }
            }
        }
    }

    /**
     * Called when structure is formed, have to be called after {@link #checkPattern()}. (server-side / fake scene only)
     * <br>
     * Trigger points:
     * <br>
     * 1 - Blocks in structure changed but still formed.
     * <br>
     * 2 - Literally, structure formed.
     */
    @Override
    public void onStructureFormed() {
        setFormed(true);
        this.isFormedValid = true;
        this.parts.clear();
        this.proxyWhileFormedPositions.clear();
        this.autoIOProxyPorts.clear();
        // replace configured formed blocks with proxy part blocks
        Map<Long, PatternPredicate.ProxyWhileFormedMatch> proxies = getMultiblockState().getMatchContext().getOrDefault("proxyWhileFormed", Collections.emptyMap());
        for (var entry : proxies.entrySet()) {
            var blockPos = BlockPos.of(entry.getKey());
            var proxyMatch = entry.getValue();
            proxyWhileFormedPositions.add(blockPos);
            var part = IMultiPart.ofPart(getLevel(), blockPos);
            if (part.orElse(null) instanceof MBDPartMachine mbdPart) {
                mbdPart.setProxyWhileFormedPredicate(this.getPos(), proxyMatch.predicateId());
            } else if (part.isEmpty()) {
                // if it is not a part, replace it with the proxy part block
                // do not replace the proxy part block
                if (getLevel().getBlockEntity(blockPos) instanceof ProxyPartBlockEntity proxyPartBlockEntity) {
                    // setup proxy part block with correct machine
                    proxyPartBlockEntity.setProxyData(this.getPos(), proxyMatch.predicateId());
                } else {
                    ProxyPartBlock.replaceOriginalBlock(this.getPos(), getLevel(), blockPos, proxyMatch.predicateId());
                }
                var autoIOCaps = proxyMatch.proxy().getProxyCapabilities().stream()
                        .filter(cap -> cap.autoIO().isEnable())
                        .toList();
                if (!autoIOCaps.isEmpty()) {
                    autoIOProxyPorts.put(blockPos, autoIOCaps);
                }
            }
        }
        Set<IMultiPart> set = getMultiblockState().getMatchContext().getOrCreate("parts", Collections::emptySet);
        for (IMultiPart part : set) {
            if (shouldAddPartToController(part)) {
                this.parts.add(part);
            }
        }
        getDefinition().sortParts(this.parts);
        for (var part : parts) {
            part.addedToController(this);
        }
        updatePartPositions();
        // refresh traits
        initCapabilitiesProxy();
        // post event
        NeoForge.EVENT_BUS.post(new MachineStructureFormedEvent(this).postCustomEvent());
        // notify recipe logic
        notifyRecipeStatusChanged(getRecipeLogic().getStatus(), getRecipeLogic().getStatus());
    }

    /**
     * Called when structure is invalid. (server-side / fake scene only)
     * <br>
     * Trigger points:
     * <br>
     * 1 - Blocks in structure changed.
     * <br>
     * 2 - Before controller machine removed.
     */
    @Override
    public void onStructureInvalid(boolean isControllerRemoved) {
        setFormed(false);
        this.isFormedValid = false;
        getMultiblockState().clearMatchedPredicateIds();
        // reset recipe Logic
        getRecipeLogic().resetRecipeLogic();
        // clear parts
        for (IMultiPart part : parts) {
            part.removedFromController(this);
        }
        this.parts.clear();
        updatePartPositions();
        // refresh traits
        initCapabilitiesProxy();
        // restore original blocks
        getMultiblockState().runInternalStructureInvaliding(() -> {
            for (var pos : new HashSet<>(proxyWhileFormedPositions)) {
                if (getLevel().getBlockEntity(pos) instanceof ProxyPartBlockEntity proxyPartBlockEntity) {
                    proxyPartBlockEntity.restoreOriginalBlock();
                }
            }
            this.proxyWhileFormedPositions.clear();
        });
        this.autoIOProxyPorts.clear();
        // post event
        NeoForge.EVENT_BUS.post(new MachineStructureInvalidEvent(this).postCustomEvent());
        // back to original block
        if (!isControllerRemoved && originalBlock != null) {
            getLevel().setBlockAndUpdate(getPos(), originalBlock);
        }
    }

    /**
     * mark multiblockState as unload error first.
     * if it's actually cuz by block breaking.
     * {@link #onStructureInvalid()} will be called from {@link MultiblockState#onBlockStateChanged(BlockPos, BlockState)}
     */
    @Override
    public void onPartUnload() {
        parts.removeIf(IMachine::isInValid);
        getMultiblockState().setError(MultiblockState.UNLOAD_ERROR);
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).addAsyncLogic(this);
        }
        updatePartPositions();
    }

    /**
     * Called when the machine is rotated.
     * <br>
     * It has to be triggered somewhere yourself.
     */
    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        if (oldFacing != newFacing && getLevel() instanceof ServerLevel serverLevel) {
            // invalid structure
            this.onStructureInvalid();
            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
            mwsd.removeMapping(getMultiblockState());
            mwsd.addAsyncLogic(this);
        }
    }

    /**
     * Should open UI.
     */
    @Override
    public boolean shouldOpenUI(BlockHitResult hit) {
        return super.shouldOpenUI(hit) && (!getDefinition().multiblockSettings().showUIOnlyFormed() || isFormed());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var result = super.useWithoutItem(state, world, pos, player, hit);
        if (result == InteractionResult.PASS && !isFormed() && player.isShiftKeyDown()) {
            if (world.isClientSide()) {
                MultiblockInWorldPreviewRenderer.showPreview(pos, this, ConfigHolder.MULTIBLOCK_PREVIEW_DURATION.get() * 20);
            }
            return InteractionResult.SUCCESS;
        }
        return result;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack item, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var result = super.useItemOn(item, state, level, pos, player, hand, hit);
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && !isFormed() && getDefinition().multiblockSettings().catalyst().isEnable()) {
            var catalyst = getDefinition().multiblockSettings().catalyst();
            var held = player.getItemInHand(hand);
            if (catalyst.test(held)) {
                if (level instanceof ServerLevel serverLevel && checkPatternWithLock()) { // formed
                    var success = onCatalystUsed(player, hand, held);
                    if (success) {
                        onStructureFormed();
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.addMapping(getMultiblockState());
                        mwsd.removeAsyncLogic(this);
                        return ItemInteractionResult.CONSUME;
                    } else {
                        return ItemInteractionResult.FAIL;
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        return result;
    }

    public boolean onCatalystUsed(Player player, InteractionHand hand, ItemStack held) {
        var catalyst = getDefinition().multiblockSettings().catalyst();
        var event = new MachineUseCatalystEvent(this, held, player, hand);
        NeoForge.EVENT_BUS.post(event.postCustomEvent());
        if (event.isCanceled()) {
            return false;
        }
        if (player.isCreative()) return true;
        return switch (catalyst.getCatalystType()) {
            case CONSUME_ITEM -> {
                if (held.getCount() >= catalyst.getConsumeItemAmount()) {
                    held.shrink(catalyst.getConsumeItemAmount());
                    yield true;
                }
                yield false;
            }
            case CONSUME_DURABILITY -> {
                if (catalyst.getConsumeDurabilityValue() <= held.getMaxDamage() - held.getDamageValue()) {
                    held.hurtAndBreak(catalyst.getConsumeDurabilityValue(), player, LivingEntity.getSlotForHand(hand));
                    yield true;
                }
                yield false;
            }
        };
    }

    @Override
    public ItemStack getDropItem() {
        if (originalBlock != null) {
            return originalBlock.getBlock().asItem().getDefaultInstance();
        }
        return super.getDropItem();
    }

    @Override
    protected void bindMachineUI(UI ui) {
        super.bindMachineUI(ui);

        // proxy part ui
        var prefix = "part:";
        var midTag = "@ui:";
        ui.selectRegex("part:.*?@ui:").forEach(element -> {
            var id = element.getId();
            if (id.startsWith(prefix)) {
                int atIndex = id.indexOf(midTag);
                if (atIndex != -1) {
                    var traitName = id.substring(prefix.length(), atIndex);
                    var uiName = "ui:" + id.substring(atIndex + midTag.length());
                    for (var part : getParts()) {
                        if (part instanceof MBDMachine mbdMachine) {
                            var trait = mbdMachine.getTraitByName(traitName);
                            if (trait != null &&
                                    trait.getDefinition() instanceof IUIProviderTrait provider &&
                                    uiName.startsWith(provider.uiId())) {
                                element.setId(uiName);
                                provider.initTraitUI(trait, ui);
                            }
                        }
                    }
                }
            }
        });
    }
}

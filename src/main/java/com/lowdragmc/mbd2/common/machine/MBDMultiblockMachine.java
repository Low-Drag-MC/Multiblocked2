package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeStatusChangedEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineStructureFormedEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineStructureInvalidEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseCatalystEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MBDMultiblockMachine extends MBDMachine implements IMultiController {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MBDMultiblockMachine.class, MBDMachine.MANAGED_FIELD_HOLDER);
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private MultiblockState multiblockState;
    private final List<IMultiPart> parts = new ArrayList<>();
    @Getter
    @DescSynced @UpdateListener(methodName = "onPartsUpdated")
    private BlockPos[] partPositions = new BlockPos[0];
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean isFormed;
    @Getter
    protected Set<BlockPos> renderingDisabledPositions = new HashSet<>();
    @Getter
    private final Lock patternLock = new ReentrantLock();

    public MBDMultiblockMachine(IMachineBlockEntity machineHolder, MultiblockMachineDefinition definition, Object... args) {
        super(machineHolder, definition, args);
    }

    /**
     * on machine valid in the chunk.
     * <br>
     * We will add the async pattern checking logic int the next tick.
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
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    /**
     * Called when recipe logic status changed.
     * <br>
     * By default, We will update the machine state to match the recipe logic status.
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
        MinecraftForge.EVENT_BUS.post(new MachineRecipeStatusChangedEvent(this, oldStatus, newStatus).postGraphEvent());
    }

    @Override
    public @Nullable MBDRecipe doModifyRecipe(MBDRecipe recipe) {
        return applyParallel(IMultiController.super.doModifyRecipe(recipe));
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
        return super.runRecipeLogic() && isFormed();
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
            for (IMultiPart part : getParts()) {
                IO io = ioMap.getOrDefault(part.getPos().asLong(), IO.BOTH);
                if (io == IO.NONE) continue;
                for (var handler : part.getRecipeHandlers()) {
                    // If IO not compatible
                    if (io != IO.BOTH && handler.getHandlerIO() != IO.BOTH && io != handler.getHandlerIO()) continue;
                    var handlerIO = io == IO.BOTH ? handler.getHandlerIO() : io;
                    if (!capabilitiesProxy.contains(handlerIO, handler.getRecipeCapability())) {
                        capabilitiesProxy.put(handlerIO, handler.getRecipeCapability(), new ArrayList<>());
                    }
                    capabilitiesProxy.get(handlerIO, handler.getRecipeCapability()).add(handler);
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
        this.parts.clear();
        this.renderingDisabledPositions.clear();
        // disable rendering for formed parts
        LongSet disabled = getMultiblockState().getMatchContext().getOrDefault("renderMask", LongSets.EMPTY_SET);
        for (var pos : disabled) {
            var blockPos = BlockPos.of(pos);
            renderingDisabledPositions.add(blockPos);
            // if it not a part, replace it with the proxy part block
            if (IMultiPart.ofPart(getLevel(), blockPos).isEmpty()) {
                // do not replace the proxy part block
                if (getLevel().getBlockEntity(blockPos) instanceof ProxyPartBlockEntity proxyPartBlockEntity) {
                    // setup proxy part block with correct machine
                    proxyPartBlockEntity.setControllerData(this.getPos());
                } else {
                    ProxyPartBlock.replaceOriginalBlock(this.getPos(), getLevel(), blockPos);
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
        MinecraftForge.EVENT_BUS.post(new MachineStructureFormedEvent(this).postGraphEvent());
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
    public void onStructureInvalid() {
        setFormed(false);
        for (IMultiPart part : parts) {
            part.removedFromController(this);
        }
        parts.clear();
        updatePartPositions();
        // refresh traits
        initCapabilitiesProxy();
        // reset recipe Logic
        getRecipeLogic().resetRecipeLogic();
        // restore original blocks
        for (var pos : renderingDisabledPositions) {
            if (getLevel().getBlockEntity(pos) instanceof ProxyPartBlockEntity proxyPartBlockEntity) {
                proxyPartBlockEntity.restoreOriginalBlock();
            }
        }
        renderingDisabledPositions.clear();
        // post event
        MinecraftForge.EVENT_BUS.post(new MachineStructureInvalidEvent(this).postGraphEvent());
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
    public boolean shouldOpenUI(InteractionHand hand, BlockHitResult hit) {
        return super.shouldOpenUI(hand, hit) && (!getDefinition().multiblockSettings().showUIOnlyFormed() || isFormed());
    }

    /**
     * On hand is using on the machine.
     * <br>
     * We will check the catalyst and consume it if it's valid.
     */
    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isFormed() && getDefinition().multiblockSettings().catalyst().isEnable()) {
            var catalyst = getDefinition().multiblockSettings().catalyst();
            var held = player.getItemInHand(hand);
            if (catalyst.test(held)) {
                if (world instanceof ServerLevel serverLevel && checkPatternWithLock()) { // formed
                    var event = new MachineUseCatalystEvent(this, held);
                    MinecraftForge.EVENT_BUS.post(event.postGraphEvent());
                    if (event.isCanceled()) {
                        return InteractionResult.FAIL;
                    }
                    var success = switch (catalyst.getCatalystType()) {
                        case CONSUME_ITEM -> {
                            if (held.getCount() >= catalyst.getConsumeItemAmount()) {
                                held.shrink(catalyst.getConsumeItemAmount());
                                yield true;
                            }
                            yield false;
                        }
                        case CONSUME_DURABILITY -> {
                            if (catalyst.getConsumeDurabilityValue() <= held.getMaxDamage() - held.getDamageValue()) {
                                held.hurtAndBreak(catalyst.getConsumeDurabilityValue(), player, p -> p.broadcastBreakEvent(hand));
                                yield true;
                            }
                            yield false;
                        }
                    };
                    if (success) {
                        onStructureFormed();
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.addMapping(getMultiblockState());
                        mwsd.removeAsyncLogic(this);
                        return InteractionResult.CONSUME;
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}

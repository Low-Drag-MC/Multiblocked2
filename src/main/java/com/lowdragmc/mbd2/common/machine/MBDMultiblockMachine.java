package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private final Lock patternLock = new ReentrantLock();

    public MBDMultiblockMachine(IMachineBlockEntity machineHolder, MultiblockMachineDefinition definition, Object... args) {
        super(machineHolder, definition, args);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).addAsyncLogic(this);
        }
    }

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

    @Override
    public void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        if (isFormed) {
            switch (newStatus) {
                case WORKING -> setMachineState("working");
                case IDLE -> setMachineState("formed");
                case WAITING -> setMachineState("waiting");
                case SUSPEND -> setMachineState("suspend");
            }
        }
    }

    @Override
    public BlockPattern getPattern() {
        return getDefinition().getPattern();
    }

    @Override
    @Nonnull
    public MultiblockState getMultiblockState() {
        if (multiblockState == null) {
            multiblockState = new MultiblockState(getLevel(), getPos());
        }
        return multiblockState;
    }

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

    @Override
    public void onStructureFormed() {
        setFormed(true);
        this.parts.clear();
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
    }

    @Override
    public void onStructureInvalid() {
        setFormed(false);
        for (IMultiPart part : parts) {
            part.removedFromController(this);
        }
        parts.clear();
        updatePartPositions();
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

}

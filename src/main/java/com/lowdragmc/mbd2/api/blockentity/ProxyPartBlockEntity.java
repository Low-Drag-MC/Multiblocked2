package com.lowdragmc.mbd2.api.blockentity;

import lombok.Getter;
import lombok.Setter;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * @author KilaBash
 * @implNote It is used to replace the non mbd blocks that do not need to be rendered after forming in the multiblock structure,
 * and to restore the original blocks when the structure invalid.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProxyPartBlockEntity extends BlockEntity {
    @Getter
    @Setter
    private boolean isAsyncSyncing = false;

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ProxyPartBlockEntity>> TYPE;
    public static BlockEntityType<?> TYPE() {
        return TYPE.get();
    }

    @Nullable
    @Getter
    private BlockState originalState;
    @Nullable
    @Getter
    private CompoundTag originalData;
    @Nullable
    @Getter
    private BlockPos controllerPos;
    @Nullable
    private StateMachine<MachineState> proxyStateMachine;
    @Getter
    private boolean restoringOriginalBlock;

    public ProxyPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE(), pPos, pBlockState);
    }

    public void setControllerData(BlockPos controllerPos) {
        setProxyData(controllerPos, getProxyStateMachine());
    }

    public void setOriginalData(BlockState originalState, CompoundTag originalData, BlockPos controllerPos) {
        setOriginalData(originalState, originalData, controllerPos, getProxyStateMachine());
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        if (!Objects.equals(this.originalState, originalState) ||
                !Objects.equals(this.originalData, originalData) ||
                !Objects.equals(this.controllerPos, controllerPos) ||
                this.proxyStateMachine != proxyStateMachine) {
            this.originalState = originalState;
            this.originalData = originalData;
            this.controllerPos = controllerPos;
            this.proxyStateMachine = proxyStateMachine;
            sync();
        }
    }

    public void setProxyData(BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        if (!Objects.equals(this.controllerPos, controllerPos) || this.proxyStateMachine != proxyStateMachine) {
            this.controllerPos = controllerPos;
            this.proxyStateMachine = proxyStateMachine;
            sync();
        }
    }

    /**
     * Place the original block back to the world. and restore the original block entity data.
     */
    public void restoreOriginalBlock() {
        if (level != null && originalState != null) {
            restoringOriginalBlock = true;
            level.setBlockAndUpdate(getBlockPos(), originalState);
            if (originalData != null) {
                var blockEntity = level.getBlockEntity(worldPosition);
                if (blockEntity != null) {
                    blockEntity.loadWithComponents(originalData, level.registryAccess());
                }
            }
            restoringOriginalBlock = false;
        }
    }

    public StateMachine<MachineState> getProxyStateMachine() {
        if (proxyStateMachine == null) {
            proxyStateMachine = new StateMachine<>(MachineState.baseBuilder().build());
        }
        return proxyStateMachine;
    }

    public MachineState getProxyState() {
        var stateMachine = getProxyStateMachine();
        if (level != null && controllerPos != null) {
            return IMachine.ofMachine(level, controllerPos)
                    .filter(MBDMachine.class::isInstance)
                    .map(MBDMachine.class::cast)
                    .map(machine -> stateMachine.getState(machine.getMachineStateName()))
                    .orElseGet(stateMachine::getRootState);
        }
        return stateMachine.getRootState();
    }

    public Direction getProxyFacing() {
        if (level != null && controllerPos != null) {
            return IMachine.ofMachine(level, controllerPos)
                    .filter(MBDMachine.class::isInstance)
                    .map(MBDMachine.class::cast)
                    .flatMap(MBDMachine::getFrontFacing)
                    .orElse(Direction.NORTH);
        }
        return Direction.NORTH;
    }

    public VoxelShape getProxyShape() {
        return getProxyState().getShape(getProxyFacing());
    }

    @Nullable
    public AABB getProxyRenderBoundingBox() {
        var aabb = getProxyState().getRenderingBox(getProxyFacing());
        return aabb == null ? null : aabb.move(getBlockPos());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (originalState != null) {
            tag.put("originalState", NbtUtils.writeBlockState(originalState));
        }

        if (originalData != null) {
            tag.put("originalData", originalData);
        }

        if (controllerPos != null) {
            tag.put("controllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        if (proxyStateMachine != null) {
            tag.put("proxyStateMachine", proxyStateMachine.serializeNBT(provider));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        if (tag.contains("originalState")) {
            originalState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("originalState"));
        }

        if (tag.contains("originalData")) {
            originalData = tag.getCompound("originalData");
        }

        if (tag.contains("controllerPos")) {
            controllerPos = NbtUtils.readBlockPos(tag, "controllerPos").orElse(BlockPos.ZERO);
        }

        if (tag.contains("proxyStateMachine")) {
            proxyStateMachine = new StateMachine<>(MachineState.baseBuilder().build());
            proxyStateMachine.deserializeNBT(provider, tag.getCompound("proxyStateMachine"));
        } else {
            proxyStateMachine = null;
        }

    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        var tag = new CompoundTag();

        if (originalState != null) {
            tag.put("originalState", NbtUtils.writeBlockState(originalState));
        }

        if (originalData != null) {
            tag.put("originalData", originalData);
        }

        if (controllerPos != null) {
            tag.put("controllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        if (proxyStateMachine != null) {
            tag.put("proxyStateMachine", proxyStateMachine.serializeNBT(provider));
        }

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 11);
        }
    }

}

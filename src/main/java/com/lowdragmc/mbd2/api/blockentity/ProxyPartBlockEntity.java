package com.lowdragmc.mbd2.api.blockentity;

import lombok.Getter;
import lombok.Setter;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private List<ConfigPartSettings.ProxyCapability> proxyCapabilities = new ArrayList<>();
    @Getter
    private boolean restoringOriginalBlock;

    public List<ConfigPartSettings.ProxyCapability> getProxyCapabilities() {
        return proxyCapabilities;
    }

    public ProxyPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE(), pPos, pBlockState);
    }

    public void setControllerData(BlockPos controllerPos) {
        setProxyData(controllerPos, getProxyStateMachine(), proxyCapabilities);
    }

    public void setOriginalData(BlockState originalState, CompoundTag originalData, BlockPos controllerPos) {
        setOriginalData(originalState, originalData, controllerPos, getProxyStateMachine(), proxyCapabilities);
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        setOriginalData(originalState, originalData, controllerPos, proxyStateMachine, Collections.emptyList());
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine, List<ConfigPartSettings.ProxyCapability> proxyCapabilities) {
        if (!Objects.equals(this.originalState, originalState) ||
                !Objects.equals(this.originalData, originalData) ||
                !Objects.equals(this.controllerPos, controllerPos) ||
                this.proxyStateMachine != proxyStateMachine ||
                !this.proxyCapabilities.equals(proxyCapabilities)) {
            this.originalState = originalState;
            this.originalData = originalData;
            this.controllerPos = controllerPos;
            this.proxyStateMachine = proxyStateMachine;
            this.proxyCapabilities = new ArrayList<>(proxyCapabilities);
            sync();
        }
    }

    public void setProxyData(BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        setProxyData(controllerPos, proxyStateMachine, Collections.emptyList());
    }

    public void setProxyData(BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine, List<ConfigPartSettings.ProxyCapability> proxyCapabilities) {
        if (!Objects.equals(this.controllerPos, controllerPos) ||
                this.proxyStateMachine != proxyStateMachine ||
                !this.proxyCapabilities.equals(proxyCapabilities)) {
            this.controllerPos = controllerPos;
            this.proxyStateMachine = proxyStateMachine;
            this.proxyCapabilities = new ArrayList<>(proxyCapabilities);
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
            proxyStateMachine = PatternPredicate.ProxyWhileFormed.createDefaultStateMachine();
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
        writeSyncedFields(tag, provider);
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
            proxyStateMachine = PatternPredicate.ProxyWhileFormed.createDefaultStateMachine();
            proxyStateMachine.deserializeNBT(provider, tag.getCompound("proxyStateMachine"));
        } else {
            proxyStateMachine = null;
        }

        proxyCapabilities = new ArrayList<>();
        if (tag.contains("proxyCapabilities", Tag.TAG_LIST)) {
            var list = tag.getList("proxyCapabilities", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var cap = new ConfigPartSettings.ProxyCapability();
                cap.deserializeNBT(provider, list.getCompound(i));
                proxyCapabilities.add(cap);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        writeSyncedFields(tag, provider);
        return tag;
    }

    private void writeSyncedFields(CompoundTag tag, HolderLookup.Provider provider) {
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

        if (!proxyCapabilities.isEmpty()) {
            var list = new ListTag();
            for (var cap : proxyCapabilities) {
                list.add(cap.serializeNBT(provider));
            }
            tag.put("proxyCapabilities", list);
        }
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

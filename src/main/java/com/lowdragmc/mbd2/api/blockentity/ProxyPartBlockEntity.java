package com.lowdragmc.mbd2.api.blockentity;

import lombok.Getter;
import lombok.Setter;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.capability.IAnimationSource;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author KilaBash
 * @implNote It is used to replace the non mbd blocks that do not need to be rendered after forming in the multiblock structure,
 * and to restore the original blocks when the structure invalid.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProxyPartBlockEntity extends BlockEntity implements IAnimationSource {
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
    private StateMachine<MachineState> fallbackProxyStateMachine;
    @Getter
    private int proxyPredicateId = -1;
    @Getter
    private boolean restoringOriginalBlock;
    @Getter
    private final Map<IRenderer, Object> animatableCache = new HashMap<>();

    public List<ConfigPartSettings.ProxyCapability> getProxyCapabilities() {
        return resolveProxyWhileFormed()
                .map(PatternPredicate.ProxyWhileFormed::getProxyCapabilities)
                .orElse(Collections.emptyList());
    }

    public ProxyPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE(), pPos, pBlockState);
    }

    public void setControllerData(BlockPos controllerPos) {
        setProxyData(controllerPos, proxyPredicateId);
    }

    public void setOriginalData(BlockState originalState, CompoundTag originalData, BlockPos controllerPos) {
        setOriginalData(originalState, originalData, controllerPos, proxyPredicateId);
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        setOriginalData(originalState, originalData, controllerPos, -1);
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine, List<ConfigPartSettings.ProxyCapability> proxyCapabilities) {
        setOriginalData(originalState, originalData, controllerPos, -1);
    }

    public void setOriginalData(BlockState originalState, @Nullable CompoundTag originalData, BlockPos controllerPos, int proxyPredicateId) {
        if (!Objects.equals(this.originalState, originalState) ||
                !Objects.equals(this.originalData, originalData) ||
                !Objects.equals(this.controllerPos, controllerPos) ||
                this.proxyPredicateId != proxyPredicateId) {
            this.originalState = originalState;
            this.originalData = originalData;
            this.controllerPos = controllerPos;
            this.proxyPredicateId = proxyPredicateId;
            sync();
        }
    }

    public void setProxyData(BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine) {
        setProxyData(controllerPos, -1);
    }

    public void setProxyData(BlockPos controllerPos, StateMachine<MachineState> proxyStateMachine, List<ConfigPartSettings.ProxyCapability> proxyCapabilities) {
        setProxyData(controllerPos, -1);
    }

    public void setProxyData(BlockPos controllerPos, int proxyPredicateId) {
        if (!Objects.equals(this.controllerPos, controllerPos) ||
                this.proxyPredicateId != proxyPredicateId) {
            this.controllerPos = controllerPos;
            this.proxyPredicateId = proxyPredicateId;
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
        return resolveProxyWhileFormed()
                .map(PatternPredicate.ProxyWhileFormed::getStateMachine)
                .orElseGet(this::getFallbackProxyStateMachine);
    }

    private StateMachine<MachineState> getFallbackProxyStateMachine() {
        if (fallbackProxyStateMachine == null) {
            fallbackProxyStateMachine = PatternPredicate.ProxyWhileFormed.createDefaultStateMachine();
        }
        return fallbackProxyStateMachine;
    }

    private java.util.Optional<PatternPredicate.ProxyWhileFormed> resolveProxyWhileFormed() {
        if (level == null || controllerPos == null || proxyPredicateId < 0) {
            return java.util.Optional.empty();
        }
        return IMachine.ofMachine(level, controllerPos)
                .filter(MBDMultiblockMachine.class::isInstance)
                .map(MBDMultiblockMachine.class::cast)
                .map(MBDMultiblockMachine::getPattern)
                .map(pattern -> pattern.getPredicate(proxyPredicateId))
                .map(predicate -> predicate.proxyWhileFormed)
                .filter(PatternPredicate.ProxyWhileFormed::isEnable);
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

    // The port is not a machine, so it animates off the predicate's proxy state and the controller's
    // facing — the same two things it already borrows for its shape and model.

    @Override
    public BlockPos getAnimationPos() {
        return getBlockPos();
    }

    @Override
    public Direction getAnimationFacing() {
        return getProxyFacing();
    }

    @Override
    public String getAnimationState() {
        return getProxyState().name();
    }

    @Nullable
    @Override
    public MBDMachine getAnimationEventTarget() {
        if (level == null || controllerPos == null) {
            return null;
        }
        return IMachine.ofMachine(level, controllerPos)
                .filter(MBDMachine.class::isInstance)
                .map(MBDMachine.class::cast)
                .orElse(null);
    }

    @Nullable
    public AABB getProxyRenderBoundingBox() {
        var aabb = getProxyState().getRenderingBox(getProxyFacing());
        return aabb == null ? null : aabb.move(getBlockPos());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeSyncedFields(tag);
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

        if (tag.contains("proxyPredicateId", Tag.TAG_INT)) {
            proxyPredicateId = tag.getInt("proxyPredicateId");
        } else {
            proxyPredicateId = -1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        writeUpdateFields(tag);
        return tag;
    }

    private void writeSyncedFields(CompoundTag tag) {
        if (originalState != null) {
            tag.put("originalState", NbtUtils.writeBlockState(originalState));
        }

        if (originalData != null) {
            tag.put("originalData", originalData);
        }

        if (controllerPos != null) {
            tag.put("controllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        if (proxyPredicateId >= 0) {
            tag.putInt("proxyPredicateId", proxyPredicateId);
        }
    }

    private void writeUpdateFields(CompoundTag tag) {
        if (originalState != null) {
            tag.put("originalState", NbtUtils.writeBlockState(originalState));
        }

        if (controllerPos != null) {
            tag.put("controllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        if (proxyPredicateId >= 0) {
            tag.putInt("proxyPredicateId", proxyPredicateId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 11);
            // the proxied controller/predicate changed, so the capabilities this block exposes may have
            // changed too: invalidate so adjacent pipes re-resolve instead of keeping a stale cache.
            invalidateCapabilities();
        }
    }

}

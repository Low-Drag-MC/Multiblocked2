package com.lowdragmc.mbd2.api.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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

    public ProxyPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE(), pPos, pBlockState);
    }

    public void setControllerData(BlockPos controllerPos) {
        if (this.controllerPos != controllerPos) {
            this.controllerPos = controllerPos;
            sync();
        }
    }

    public void setOriginalData(BlockState originalState, CompoundTag originalData, BlockPos controllerPos) {
        if (this.originalState != originalState || this.originalData != originalData || this.controllerPos != controllerPos) {
            this.originalState = originalState;
            this.originalData = originalData;
            this.controllerPos = controllerPos;
            sync();
        }
    }

    /**
     * Place the original block back to the world. and restore the original block entity data.
     */
    public void restoreOriginalBlock() {
        if (originalState != null) {
            level.setBlockAndUpdate(getBlockPos(), originalState);
            if (originalData != null) {
                var blockEntity = level.getBlockEntity(worldPosition);
                if (blockEntity != null) {
                    blockEntity.loadWithComponents(originalData, level.registryAccess());
                }
            }
        }
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

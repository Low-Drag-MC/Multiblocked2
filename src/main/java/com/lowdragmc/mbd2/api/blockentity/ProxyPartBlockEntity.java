package com.lowdragmc.mbd2.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.DummyWorld;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @implNote It is used to replace the non mbd blocks that do not need to be rendered after forming in the multiblock structure,
 * and to restore the original blocks when the structure invalid.
 */
public class ProxyPartBlockEntity extends BlockEntity implements IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity, IManaged {
    @Getter
    private final FieldManagedStorage rootStorage = new FieldManagedStorage(this);

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ProxyPartBlockEntity.class);

    @Getter
    @Setter
    private boolean isAsyncSyncing = false;

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return rootStorage;
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    public static RegistryObject<BlockEntityType<ProxyPartBlockEntity>> TYPE;
    public static BlockEntityType<?> TYPE() {
        return TYPE.get();
    }

    @Nullable
    @Persisted
    @Getter
    private BlockState originalState;
    @Nullable
    @Persisted
    @Getter
    private CompoundTag originalData;
    @Nullable
    @DescSynced
    @Persisted
    @Getter
    private BlockPos controllerPos;

    public ProxyPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE(), pPos, pBlockState);
    }

    public void setControllerData(BlockPos controllerPos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> {
                this.controllerPos = controllerPos;
            }));
        }
    }

    public void setOriginalData(BlockState originalState, CompoundTag originalData, BlockPos controllerPos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> {
                this.originalState = originalState;
                this.originalData = originalData;
                this.controllerPos = controllerPos;
            }));
        } else if (level instanceof DummyWorld) {
            this.originalState = originalState;
            this.originalData = originalData;
            this.controllerPos = controllerPos;
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
                    blockEntity.load(originalData);
                }
            }
        }
    }

}

package com.lowdragmc.mbd2.common.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.storage.MultiManagedStorage;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * A simple compound Interface for a BlockEntity which is holding a Machine feature.
 * <br>
 * Its using async system to sync data.
 */
public class MachineBlockEntity extends BlockEntity implements IMachineBlockEntity {
    @Getter
    public final MultiManagedStorage rootStorage = new MultiManagedStorage();
    @Getter
    private final long offset = IMachineBlockEntity.randomTickOffset();
    @Getter
    private IMachine metaMachine;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, Function<IMachineBlockEntity, IMachine> machineFactory) {
        super(type, pos, blockState);
        this.setMachine(machineFactory.apply(this));
    }

    public void setMachine(IMachine newMachine) {
        if (metaMachine == newMachine) return;
        if (metaMachine != null && level != null && !level.isClientSide) {
            metaMachine.onUnload();
        }
        if (metaMachine instanceof MBDMachine machine) {
            machine.detach();
        }
        metaMachine = newMachine;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        getMetaMachine().onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        getMetaMachine().onUnload();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        getMetaMachine().onLoad();
    }

}

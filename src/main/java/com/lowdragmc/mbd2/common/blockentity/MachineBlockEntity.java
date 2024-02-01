package com.lowdragmc.mbd2.common.blockentity;

import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple compound Interface for a BlockEntity which is holding a Machine feature.
 * <br>
 * Its using async system to sync data.
 */
public class MachineBlockEntity extends BlockEntity implements IMachineBlockEntity {
    @Getter
    public final MultiManagedStorage rootStorage = new MultiManagedStorage();
    @Getter
    private final long offset = MBD2.RND.nextLong();
    @Getter
    private IMachine metaMachine;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        metaMachine = new MBDMachine(this, ((MBDMachineBlock)blockState.getBlock()).getDefinition());
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

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MBDCapabilities.CAPABILITY_MACHINE) {
            return MBDCapabilities.CAPABILITY_MACHINE.orEmpty(cap, LazyOptional.of(this::getMetaMachine));
        }
        return super.getCapability(cap, side);
    }

}

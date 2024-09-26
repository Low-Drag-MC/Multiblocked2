package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MBDKineticMachineBlock extends MBDMachineBlock implements IRotate {

    public MBDKineticMachineBlock(Properties properties, CreateKineticMachineDefinition definition) {
        super(properties, definition);
    }

    @Override
    public CreateKineticMachineDefinition getDefinition() {
        return (CreateKineticMachineDefinition)super.getDefinition();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return getDefinition().kineticMachineSettings().hasShaftTowards(face, getFrontFacing(state).orElse(Direction.NORTH));
    }

    public Direction getRotationFacing(BlockState state) {
        return getDefinition().kineticMachineSettings().getRotationFacing(getFrontFacing(state).orElse(Direction.NORTH));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return getRotationFacing(state).getAxis();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // onBlockAdded is useless for init, as sometimes the TE gets re-instantiated

        // however, if a block change occurs that does not change kinetic connections,
        // we can prevent a major re-propagation here

        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof KineticBlockEntity kineticTileEntity) {
            kineticTileEntity.preventSpeedUpdate = 0;

            if (oldState.getBlock() != state.getBlock())
                return;
            if (state.hasBlockEntity() != oldState.hasBlockEntity())
                return;
            if (!areStatesKineticallyEquivalent(oldState, state))
                return;

            kineticTileEntity.preventSpeedUpdate = 2;
        }
    }


    public boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        if (oldState.getBlock() != newState.getBlock())
            return false;
        return getRotationAxis(newState) == getRotationAxis(oldState);
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState stateIn, LevelAccessor worldIn, BlockPos pos, int flags,
                                              int count) {
        if (worldIn.isClientSide())
            return;

        BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        if (!(tileEntity instanceof KineticBlockEntity kte))
            return;

        if (kte.preventSpeedUpdate > 0)
            return;

        // Remove previous information when block is added
        kte.warnOfMovement();
        kte.clearKineticInformation();
        kte.updateSpeed = true;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType == getDefinition().blockEntityType()) {
            return (world, pos, state1, blockEntity) -> {
                IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).ifPresent(machine -> {
                    if (world.isClientSide) {
                        machine.clientTick();
                    } else {
                        machine.serverTick();
                    }
                });
                if (blockEntity instanceof KineticBlockEntity kineticBlockEntity) {
                    kineticBlockEntity.tick();
                }
            };
        }
        return null;
    }
}

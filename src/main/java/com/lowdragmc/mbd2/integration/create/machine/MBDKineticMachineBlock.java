package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MBDKineticMachineBlock extends MBDMachineBlock implements IRotate, ICogWheel {

    public MBDKineticMachineBlock(Properties properties, CreateKineticMachineDefinition definition) {
        super(properties, definition);
    }

    @Override
    public CreateKineticMachineDefinition getDefinition() {
        return (CreateKineticMachineDefinition) super.getDefinition();
    }

    public Direction getRotationFacing(BlockState state) {
        return getDefinition().kineticMachineSettings().getRotationFacing(getFrontFacing(state).orElse(Direction.NORTH));
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return getDefinition().kineticMachineSettings().hasShaftTowards(face, getRotationFacing(state));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return getRotationFacing(state).getAxis();
    }

    @Override
    public boolean isSmallCog() {
        return getDefinition().kineticMachineSettings().isSmallCog();
    }

    @Override
    public boolean isLargeCog() {
        return getDefinition().kineticMachineSettings().isLargeCog();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof KineticBlockEntity kineticBE) {
            kineticBE.preventSpeedUpdate = 0;
            if (oldState.getBlock() != state.getBlock()) return;
            if (state.hasBlockEntity() != oldState.hasBlockEntity()) return;
            if (!areStatesKineticallyEquivalent(oldState, state)) return;
            kineticBE.preventSpeedUpdate = 2;
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        if (state == null) return null;
        var preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(context);
        if (preferredAxis != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())) {
            if (preferredAxis == getRotationAxis(state)) return state;
            var rotationState = getRotationState();
            if (rotationState.property.isPresent()) {
                for (var dir : Direction.values()) {
                    if (rotationState.test(dir)) {
                        var newState = state.setValue(rotationState.property.get(), dir);
                        if (getRotationAxis(newState) == preferredAxis) return newState;
                    }
                }
            }
        }
        return state;
    }

    public boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        if (oldState.getBlock() != newState.getBlock()) return false;
        return getRotationAxis(newState) == getRotationAxis(oldState);
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState stateIn, LevelAccessor worldIn, BlockPos pos, int flags, int count) {
        if (worldIn.isClientSide()) return;
        BlockEntity be = worldIn.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kte)) return;
        if (kte.preventSpeedUpdate > 0) return;
        kte.warnOfMovement();
        kte.clearKineticInformation();
        kte.updateSpeed = true;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType == getDefinition().blockEntityType()) {
            return (world, pos, state1, blockEntity) -> {
                IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).ifPresent(machine -> {
                    if (world.isClientSide) machine.clientTick();
                    else machine.serverTick();
                });
                if (blockEntity instanceof KineticBlockEntity kineticBE) kineticBE.tick();
            };
        }
        return null;
    }
}

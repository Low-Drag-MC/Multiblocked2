package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class MBDKineticMachineBlock extends MBDMachineBlock implements IRotate, ICogWheel, TransformableBlock {
    /**
     * The same property Create's own cogwheels carry ({@code RotatedPillarKineticBlock.AXIS}).
     *
     * <p>{@code ICogWheel} does not require it, but Create's large-cogwheel maths reads it straight
     * off the state the moment {@code isLargeCog()} answers true —
     * {@code RotationPropagator#isLargeToLargeGear}, {@code #isLargeToSmallCog} and
     * {@code #isLargeCogToSpeedController} all do. Without it those reads threw
     * {@code IllegalArgumentException: Cannot get property axis ...} on the server thread and took the
     * world down as soon as a cogwheel propagated rotation past the machine.</p>
     *
     * <p>It is redundant with the machine's own rotation facing — it is always
     * {@link #getRotationAxis(BlockState)} — so every write funnels through
     * {@link #withRotationAxis(BlockState)}, and {@link MBDKineticMachineBlockEntity} re-derives it on
     * load and on a lazy tick, which is what keeps already-placed machines correct across a
     * {@code /mbd2 reload_machine_projects} that changes the rotation facing.</p>
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public MBDKineticMachineBlock(Properties properties, CreateKineticMachineDefinition definition) {
        super(properties, definition);
        registerDefaultState(withRotationAxis(defaultBlockState()));
    }

    @Override
    public CreateKineticMachineDefinition getDefinition() {
        return (CreateKineticMachineDefinition) super.getDefinition();
    }

    /**
     * Registered on every kinetic machine, not just the ones currently configured as large cogs.
     *
     * <p>A block's state definition is fixed when the block is built, at mod construction, but
     * {@code kineticMachineSettings} is not: {@code /mbd2 reload_machine_projects} re-reads it into the
     * live definition (see {@link CreateKineticMachineDefinition#loadProductiveTag}). Gating the
     * property on {@code isLargeCog()} would therefore mean switching a machine to
     * {@code LARGE_COGWHEEL} quietly did nothing until the game was restarted — the property could
     * never appear. Three extra states per machine is the cheaper side of that trade, and MBD machines
     * render through {@code IBlockRendererProvider} rather than per-state models, so the extra states
     * cost nothing to load.</p>
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    /** Re-derive {@link #AXIS} from the state's current rotation facing; a no-op when absent. */
    public BlockState withRotationAxis(BlockState state) {
        return state.hasProperty(AXIS) ? state.setValue(AXIS, getRotationAxis(state)) : state;
    }

    @Override
    public BlockState withFrontFacing(BlockState state, Direction facing) {
        return withRotationAxis(super.withFrontFacing(state, facing));
    }

    /**
     * Rotate the machine, not its derived axis.
     *
     * <p>{@link IRotate} extends {@code IWrenchable}, so Create's wrench turns these machines, and its
     * default implementation picks whichever property it recognises first — {@link #AXIS} outranks
     * {@code FACING}. Left alone it would spin the axis while the machine stayed put: the wrench looks
     * like it does nothing, and the cog meshes against a lie until the next lazy tick undoes it. An MBD
     * machine's orientation is its front facing, so rotate that and let the axis follow.</p>
     */
    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        var rotationState = getRotationState();
        if (rotationState.property.isEmpty()) return originalState;
        var facing = originalState.getValue(rotationState.property.get());
        // Up to a full turn: when the rotation state forbids the next step — NON_Y_AXIS turned around
        // a horizontal axis, say — skip on to the next facing it does allow rather than do nothing.
        for (int i = 0; i < 4; i++) {
            facing = facing.getClockWise(targetedFace.getAxis());
            if (rotationState.test(facing)) {
                return withFrontFacing(originalState, facing);
            }
        }
        return originalState;
    }

    /**
     * Same story for a machine riding a contraption: {@code StructureTransform} also checks
     * {@link #AXIS} before it would fall through to anything that rotates a facing, so a machine
     * mounted on a bearing would ride round without turning. {@code TransformableBlock} is the hook
     * Create provides to take that decision back.
     */
    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        var rotationState = getRotationState();
        if (rotationState.property.isEmpty()) return withRotationAxis(state);
        var facing = transform.mirrorFacing(state.getValue(rotationState.property.get()));
        if (transform.rotation != null && transform.rotationAxis != null) {
            facing = transform.rotateFacing(facing);
        }
        return rotationState.test(facing) ? withFrontFacing(state, facing) : withRotationAxis(state);
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

    /**
     * Claiming to be a large cog without {@link #AXIS} on the state is what crashed Create's
     * propagator, so the property is part of the answer rather than something callers have to
     * remember. If it is ever missing the machine simply stops meshing as a large cog — a dead
     * drivetrain beats a dead server.
     */
    @Override
    public boolean isLargeCog() {
        return getDefinition().kineticMachineSettings().isLargeCog() && defaultBlockState().hasProperty(AXIS);
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
            if (preferredAxis == getRotationAxis(state)) return withRotationAxis(state);
            var rotationState = getRotationState();
            if (rotationState.property.isPresent()) {
                for (var dir : Direction.values()) {
                    if (rotationState.test(dir)) {
                        var newState = withFrontFacing(state, dir);
                        if (getRotationAxis(newState) == preferredAxis) return newState;
                    }
                }
            }
        }
        return withRotationAxis(state);
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

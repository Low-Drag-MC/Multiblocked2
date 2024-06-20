package com.lowdragmc.mbd2.common.block;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDMachineBlock extends Block implements EntityBlock, IBlockRendererProvider {

    private final MBDMachineDefinition definition;
    private final RotationState rotationState;

    public MBDMachineBlock(Properties properties, MBDMachineDefinition definition) {
        super(properties);
        this.definition = definition;
        this.rotationState = RotationState.get();
        rotationState.property.ifPresent(property -> registerDefaultState(defaultBlockState().setValue(property, rotationState.defaultDirection)));
    }

    @Override
    public MachineBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return getDefinition().blockEntityType().create(pos, state);
    }

    @Nullable
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
            };
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        RotationState rotationState = RotationState.get();
        rotationState.property.ifPresent(builder::add);
    }

    @Nullable
    @Override
    public IRenderer getRenderer(BlockState state) {
        return definition.blockRenderer();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ModelState getModelState(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        return ModelFactory.getRotation(getRotationState().property.map(state::getValue).orElse(Direction.NORTH));
    }

    public Optional<MBDMachine> getMachine(BlockGetter level, BlockPos pos) {
        return IMachine.ofMachine(level, pos).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getMachine(pLevel, pPos).map(machine -> machine.getShape(pContext)).orElse(Shapes.block());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        getMachine(level, pos).ifPresent(machine -> machine.animateTick(random));
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity player, ItemStack pStack) {
        if (!pLevel.isClientSide) {
            getMachine(pLevel, pPos).ifPresent(machine -> machine.onMachinePlaced(player, pStack));
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        RotationState rotationState = getRotationState();
        var player = context.getPlayer();
        var blockPos = context.getClickedPos();
        var state = defaultBlockState();
        return player == null ? state : rotationState.property.map(property -> {
            Vec3 pos = player.position();
            if (Math.abs(pos.x - (double) ((float) blockPos.getX() + 0.5F)) < 2.0D && Math.abs(pos.z - (double) ((float) blockPos.getZ() + 0.5F)) < 2.0D) {
                double d0 = pos.y + (double) player.getEyeHeight();
                if (d0 - (double) blockPos.getY() > 2.0D && rotationState.test(Direction.UP)) {
                    return state.setValue(property, Direction.UP);
                }
                if ((double) blockPos.getY() - d0 > 0.0D && rotationState.test(Direction.DOWN)) {
                    return state.setValue(property, Direction.DOWN);
                }
            }
            if (rotationState == RotationState.Y_AXIS) {
                return state.setValue(property, Direction.UP);
            } else {
                return state.setValue(property, player.getDirection().getOpposite());
            }
        }).orElse(state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        definition.appendHoverText(stack, tooltip);
    }

    @Override
    public boolean triggerEvent(BlockState pState, Level pLevel, BlockPos pPos, int pId, int pParam) {
        BlockEntity tile = pLevel.getBlockEntity(pPos);
        if (tile != null) {
            return tile.triggerEvent(pId, pParam);
        }
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return rotationState.property.map(property -> state.setValue(property, rotation.rotate(state.getValue(property)))).orElse(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var context = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        BlockEntity tileEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        var drops = super.getDrops(state, builder);
        IMachine.ofMachine(tileEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).ifPresent(machine -> machine.onDrops(entity, drops));
        return drops;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.hasBlockEntity()) {
            if (!pState.is(pNewState.getBlock())) { // new block
                getMachine(pLevel, pPos).ifPresent(MBDMachine::onMachineRemoved);
                pLevel.updateNeighbourForOutputSignal(pPos, this);
                pLevel.removeBlockEntity(pPos);
            } else if (rotationState.property.isPresent()){ // old block different facing
                var oldFacing = pState.getValue(rotationState.property.get());
                var newFacing = pNewState.getValue(rotationState.property.get());
                if (newFacing != oldFacing) {
                    getMachine(pLevel, pPos).ifPresent(machine -> machine.onRotated(oldFacing, newFacing));
                }
            }
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return getMachine(level, pos).map(machine -> machine.getMachineState().getLightLevel()).orElse(0);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var machine = getMachine(world, pos).orElse(null);
        if (machine == null) return InteractionResult.PASS;
        ItemStack itemStack = player.getItemInHand(hand);

        // TODO use
//        Set<GTToolType> types = ToolHelper.getToolTypes(itemStack);
//        if (machine != null && !types.isEmpty() && ToolHelper.canUse(itemStack)) {
//            var result = machine.onToolClick(types, itemStack, new UseOnContext(player, hand, hit));
//            if (result.getSecond() == InteractionResult.CONSUME && player instanceof ServerPlayer serverPlayer) {
//                ToolHelper.playToolSound(result.getFirst(), serverPlayer);
//
//                if (!serverPlayer.isCreative()) {
//                    ToolHelper.damageItem(itemStack, serverPlayer, 1);
//                }
//            }
//            if (result.getSecond() != InteractionResult.PASS) return result.getSecond();
//        }
//
//        if (machine instanceof IInteractedMachine interactedMachine) {
//            var result = interactedMachine.onUse(state, world, pos, player, hand, hit);
//            if (result != InteractionResult.PASS) return result;
//        }
        if (machine.shouldOpenUI(hand, hit)) {
            return machine.openUI(player);
        }
        return InteractionResult.PASS;
    }

    // TODO redstone signal
//    @Override
//    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
//        return getMachine(level, pos).getOutputSignal(direction);
//    }
//
//    @Override
//    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
//        return getMachine(level, pos).getOutputDirectSignal(direction);
//    }
//
//    @Override
//    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
//        return getMachine(level, pos).getAnalogOutputSignal();
//    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        getMachine(level, pos).ifPresent(machine -> machine.onNeighborChanged(block, fromPos, isMoving));
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        return getMachine(level, pos).map(machine -> machine.getAppearance(state, side, queryState, queryPos)).orElse(state);
    }

}

package com.lowdragmc.mbd2.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.client.renderer.ProxyPartRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * @author KilaBash
 * @implNote It is used to replace the non mbd blocks that do not need to be rendered after forming in the multiblock structure,
 * and to restore the original blocks when the structure invalid.
 */
public class ProxyPartBlock extends Block implements EntityBlock, IBlockRendererProvider {
    public static final ProxyPartBlock BLOCK = new ProxyPartBlock();

    public ProxyPartBlock() {
        super(Properties.of().dynamicShape().noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProxyPartBlockEntity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // drop the original block's drops
        var context = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        if (context.getParamOrNull(LootContextParams.BLOCK_ENTITY) instanceof ProxyPartBlockEntity blockEntity &&
                blockEntity.getOriginalState() != null) {
            return blockEntity.getOriginalState().getDrops(builder);
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.hasBlockEntity()) {
            if (!pState.is(pNewState.getBlock())) { // new block
                pLevel.updateNeighbourForOutputSignal(pPos, this);
                pLevel.removeBlockEntity(pPos);
                // restore original block
                if (!pLevel.isClientSide && pLevel.getBlockEntity(pPos) instanceof ProxyPartBlockEntity blockEntity) {
                    blockEntity.restoreOriginalBlock();
                }
            }
        }
    }

    @Override
    public float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof ProxyPartBlockEntity blockEntity && blockEntity.getOriginalState() != null) {
            return blockEntity.getOriginalState().getDestroyProgress(pPlayer, pLevel, pPos);
        }
        return 0;
    }

    /**
     * replace the original block and block entity data.
     */
    public static void replaceOriginalBlock(BlockPos controllerPos, Level level, BlockPos pos, Block block) {
        var originalState = level.getBlockState(pos);
        var originalBlockEntity = level.getBlockEntity(pos);
        var originalData = Optional.ofNullable(originalBlockEntity).map(BlockEntity::saveWithFullMetadata).orElse(null);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ProxyPartBlockEntity blockEntity) {
            blockEntity.setOriginalData(originalState, originalData, controllerPos);
        }
    }

    @Nullable
    @Override
    public IRenderer getRenderer(BlockState state) {
        return ProxyPartRenderer.INSTANCE;
    }
}

package com.lowdragmc.mbd2.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.client.renderer.ProxyPartRenderer;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProxyPartBlockEntity(pos, state);
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

    /**
     * replace the original block and block entity data.
     */
    public static void replaceOriginalBlock(BlockPos controllerPos, Level level, BlockPos pos) {
        var originalState = level.getBlockState(pos);
        var originalBlockEntity = level.getBlockEntity(pos);
        var originalData = Optional.ofNullable(originalBlockEntity).map(BlockEntity::saveWithFullMetadata).orElse(null);
        level.setBlockAndUpdate(pos, BLOCK.defaultBlockState());
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

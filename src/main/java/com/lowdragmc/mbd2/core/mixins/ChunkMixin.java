package com.lowdragmc.mbd2.core.mixins;

import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class ChunkMixin {
    @Final @Shadow Level level;

    // We want to be as quick as possible here
    @Inject(method = "setBlockState", at =@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z",
            ordinal = 1))
    private void mbd2$setBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        MinecraftServer server = level.getServer();
        if (server == null || !(level instanceof ServerLevel serverLevel)) return;
        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
        // Existing structural-invalidation path: notify formed multiblocks whose cache contains this pos.
        for (var structure : mwsd.getControllerInPos(pos)) {
            if (structure.isPosInCache(pos)) {
                server.executeBlocking(() -> structure.onBlockStateChanged(pos, state));
            }
        }
        // Snapshot-tracking path: mark every snapshot covering this pos for re-capture.
        mwsd.markPositionDirty(pos);
    }

}

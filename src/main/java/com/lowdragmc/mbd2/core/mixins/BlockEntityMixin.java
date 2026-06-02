package com.lowdragmc.mbd2.core.mixins;

import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notify the per-level {@link MultiblockWorldSavedData} that a BlockEntity changed, so any
 * snapshot covering this position with an NBT-restricting predicate re-captures the BE NBT on
 * the next main-thread snapshot tick.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

    @Shadow
    protected Level level;

    @Final
    @Shadow
    protected BlockPos worldPosition;

    @Inject(method = "setChanged()V", at = @At("RETURN"))
    private void mbd2$onSetChanged(CallbackInfo ci) {
        if (this.level instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).markBlockEntityChanged(this.worldPosition);
        }
    }
}

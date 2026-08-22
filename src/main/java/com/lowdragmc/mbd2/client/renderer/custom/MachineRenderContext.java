package com.lowdragmc.mbd2.client.renderer.custom;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * The arguments handed to a {@link CustomRenderer#render} call.
 * <p>
 * Fields are public and final so that scripting languages can read them as plain properties
 * ({@code ctx.poseStack}, {@code ctx.partialTick}, ...). New information can be added here without
 * breaking existing scripts, which is why the callback takes a context instead of a positional argument list.
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRenderContext {
    /**
     * The block entity being rendered. Note that it is not necessarily a machine, e.g. it can also be a proxy part.
     */
    public final BlockEntity blockEntity;
    /**
     * The extra data configured on the renderer instance in the machine editor. Never null, possibly empty.
     */
    public final CompoundTag data;
    public final float partialTick;
    public final PoseStack poseStack;
    public final MultiBufferSource bufferSource;
    public final int packedLight;
    public final int packedOverlay;

    public MachineRenderContext(BlockEntity blockEntity, CompoundTag data, float partialTick, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        this.blockEntity = blockEntity;
        this.data = data;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }

    public BlockPos getPos() {
        return blockEntity.getBlockPos();
    }

    public BlockState getBlockState() {
        return blockEntity.getBlockState();
    }

    @Nullable
    public Level getLevel() {
        return blockEntity.getLevel();
    }

    /**
     * @return the machine at this position, or null if the block entity is not an MBD machine.
     */
    @Nullable
    public MBDMachine getMachine() {
        return IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).orElse(null);
    }

    /**
     * @return the game time of the level, or 0 if the block entity is not in a level yet.
     */
    public long getGameTime() {
        var level = getLevel();
        return level == null ? 0L : level.getGameTime();
    }
}

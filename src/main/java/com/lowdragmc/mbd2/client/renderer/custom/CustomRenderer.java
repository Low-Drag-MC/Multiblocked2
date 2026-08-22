package com.lowdragmc.mbd2.client.renderer.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A named piece of block entity render logic that can be attached to any machine through the
 * {@link com.lowdragmc.mbd2.client.renderer.CustomScriptRenderer custom script renderer}.
 * <p>
 * Implementations are registered into {@link CustomRendererRegistry} under a {@link net.minecraft.resources.ResourceLocation}
 * and are looked up lazily every frame, so they survive script reloads.
 *
 * @see CustomRendererBuilder for the script facing way of creating one.
 */
@OnlyIn(Dist.CLIENT)
public interface CustomRenderer {

    /**
     * Draw the block entity. Called on the render thread every frame while the machine is visible.
     * <p>
     * The caller already saved and restores the {@link com.mojang.blaze3d.vertex.PoseStack}, and catches any
     * exception thrown here, so there is no need to be defensive about either.
     */
    void render(MachineRenderContext context);

    /**
     * The visible scope of the renderer. Anything drawn outside of this box is culled away as soon as the box
     * leaves the frustum, so tall or wide effects have to grow it.
     *
     * @see #shouldRenderOffScreen() to skip frustum culling entirely instead.
     */
    default AABB getRenderBoundingBox(BlockEntity blockEntity, CompoundTag data) {
        return new AABB(blockEntity.getBlockPos());
    }

    /**
     * How far away from the camera the renderer keeps drawing, in blocks. Vanilla block entities use 64.
     */
    default int getViewDistance() {
        return 64;
    }

    /**
     * Whether the machine should be rendered even when its {@link #getRenderBoundingBox} is off screen.
     * Comes at a cost, prefer growing the bounding box when the effect has a known size.
     */
    default boolean shouldRenderOffScreen() {
        return false;
    }

    @FunctionalInterface
    interface RenderCallback {
        void render(MachineRenderContext context);
    }

    @FunctionalInterface
    interface BoundingBoxProvider {
        AABB getRenderBoundingBox(BlockEntity blockEntity, CompoundTag data);
    }
}

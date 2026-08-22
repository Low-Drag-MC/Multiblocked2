package com.lowdragmc.mbd2.client.renderer.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * The script facing builder of a {@link CustomRenderer}. Everything but the render callback is optional:
 * <pre>{@code
 * MBDClientEvents.registerCustomRenderers(event => {
 *     event.register('mypack:beam', ctx => {
 *         // draw something using ctx.poseStack / ctx.bufferSource ...
 *     }).viewDistance(256).boundingBoxInflate(0, 256, 0)
 * })
 * }</pre>
 */
@OnlyIn(Dist.CLIENT)
public class CustomRendererBuilder implements CustomRenderer {
    @Nullable
    private RenderCallback renderCallback;
    @Nullable
    private BoundingBoxProvider boundingBoxProvider;
    private int viewDistance = 64;
    private boolean renderOffScreen = false;

    /**
     * Set the render logic. Required, a renderer without it draws nothing.
     */
    public CustomRendererBuilder onRender(RenderCallback renderCallback) {
        this.renderCallback = renderCallback;
        return this;
    }

    /**
     * How far away from the camera the renderer keeps drawing, in blocks. Defaults to the vanilla block entity value of 64.
     */
    public CustomRendererBuilder viewDistance(int viewDistance) {
        this.viewDistance = Math.max(1, viewDistance);
        return this;
    }

    /**
     * Render the machine even when its bounding box is off screen. Prefer {@link #boundingBox} when the effect has a known size.
     */
    public CustomRendererBuilder renderOffScreen(boolean renderOffScreen) {
        this.renderOffScreen = renderOffScreen;
        return this;
    }

    /**
     * Compute the visible scope of the renderer per block entity, in world space.
     */
    public CustomRendererBuilder boundingBox(BoundingBoxProvider boundingBoxProvider) {
        this.boundingBoxProvider = boundingBoxProvider;
        return this;
    }

    /**
     * Grow the default one block bounding box by the given amount on each axis.
     */
    public CustomRendererBuilder boundingBoxInflate(double x, double y, double z) {
        return boundingBox((blockEntity, data) -> new AABB(blockEntity.getBlockPos()).inflate(x, y, z));
    }

    /**
     * Grow the default one block bounding box by the given amount on every axis.
     */
    public CustomRendererBuilder boundingBoxInflate(double amount) {
        return boundingBoxInflate(amount, amount, amount);
    }

    /**
     * Never cull the renderer against the frustum. Only use it for effects that can show up anywhere on screen,
     * it forces the machine into the global block entity list.
     */
    public CustomRendererBuilder infiniteBoundingBox() {
        return boundingBox((blockEntity, data) -> AABB.INFINITE).renderOffScreen(true);
    }

    @Override
    public void render(MachineRenderContext context) {
        if (renderCallback != null) {
            renderCallback.render(context);
        }
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity, CompoundTag data) {
        return boundingBoxProvider == null
                ? CustomRenderer.super.getRenderBoundingBox(blockEntity, data)
                : boundingBoxProvider.getRenderBoundingBox(blockEntity, data);
    }

    @Override
    public int getViewDistance() {
        return viewDistance;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return renderOffScreen;
    }
}

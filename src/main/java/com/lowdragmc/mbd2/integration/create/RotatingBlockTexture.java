package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.function.Supplier;

/**
 * {@link IGuiTexture} that renders a vanilla/Create item-model spinning around the Y axis,
 * always animating from the world tick. Used as the background of a small UIElement
 * (aspectRatio:1, height:100%) in {@link CreateRotationElement}.
 *
 * <p>The element's draw bounds {@code (x, y, width, height)} are honored: the model is
 * centered at {@code (x + width/2, y + height/2)} and scaled so it spans the smaller of
 * width/height. No {@code handleCameraTransforms} is called — that would clobber the
 * parent's UI pose and force the model to render at the canvas origin.
 */
@OnlyIn(Dist.CLIENT)
public class RotatingBlockTexture implements IGuiTexture {
    private final Supplier<ItemStack> stackSupplier;
    private final Supplier<Float> rpmSupplier;

    public RotatingBlockTexture(Supplier<ItemStack> stackSupplier, Supplier<Float> rpmSupplier) {
        this.stackSupplier = stackSupplier;
        this.rpmSupplier = rpmSupplier;
    }

    @Override
    public IGuiTexture copy() {
        return new RotatingBlockTexture(stackSupplier, rpmSupplier);
    }

    @Override
    public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        var stack = stackSupplier.get();
        if (stack == null || stack.isEmpty()) return;

        var mc = Minecraft.getInstance();
        float time = mc.level == null ? 0 : AnimationTickHolder.getRenderTime(mc.level);
        Float rpm = rpmSupplier == null ? null : rpmSupplier.get();
        float speed = rpm == null ? 0f : rpm;
        // angle = time * rpm * (3/10) degrees per render-tick — same scaling Create uses in its
        // animated JEI widgets. Always animates as long as a rate is provided.
        float angle = (time * speed * 3f / 10f) % 360f * Mth.DEG_TO_RAD;

        float scale = Math.min(width, height) * 0.7f;
        var pose = graphics.pose();

        graphics.flush();
        pose.pushPose();
        // Center the model in the element bounds. +100z keeps it above background sprites.
        pose.translate(x + width / 2f, y + height / 2f, 100f);
        // Tip the cube so we look at it isometric-ish, then spin around Y.
        pose.mulPose(new Quaternionf().rotationX(Mth.DEG_TO_RAD * 60));
        pose.mulPose(new Quaternionf().rotationZ(Mth.DEG_TO_RAD * 45));
        pose.mulPose(new Quaternionf().rotationY(angle));
        // Flip Y so vanilla item models orient correctly when drawn through the UI pipeline.
        pose.scale(scale, -scale, scale);
        pose.translate(-0.5f, -0.5f, -0.5f);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        var buffers = graphics.bufferSource();
        var itemRenderer = mc.getItemRenderer();
        var model = itemRenderer.getModel(stack, null, null, 0);
        try {
            itemRenderer.renderModelLists(model, stack, 15728880, OverlayTexture.NO_OVERLAY, pose,
                    buffers.getBuffer(RenderType.solid()));
            buffers.endBatch();
        } catch (Throwable ignored) {
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        pose.popPose();
    }
}

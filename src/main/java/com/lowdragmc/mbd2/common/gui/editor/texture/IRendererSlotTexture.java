package com.lowdragmc.mbd2.common.gui.editor.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class IRendererSlotTexture implements IGuiTexture {
    @Getter @Setter
    private Supplier<IRenderer> rendererSupplier;

    public IRendererSlotTexture(Supplier<IRenderer> rendererSupplier) {
        this.rendererSupplier = rendererSupplier;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        var itemW = width * 16f / 18;
        var itemH = height * 16f / 18;
        var itemX = x + (width - itemW) / 2;
        var itemY = y + (height - itemH) / 2;

        var renderer = rendererSupplier.get();
        var pose = graphics.pose();

        pose.pushPose();
        pose.scale(itemW / 16.0F, (float)itemH / 16.0F, 1.0F);
        pose.translate(itemX * 16.0F / itemW, itemY * 16.0F / (float)itemH, -200.0F);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 232.0F);

        pose.pushPose();
        pose.translate(8, 8, (float)(150));

        try {
            pose.mulPose((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
            pose.scale(16.0F, 16.0F, 16.0F);
            boolean flag = !renderer.useBlockLight(ItemStack.EMPTY);
            if (flag) {
                Lighting.setupForFlatItems();
            }
            var buffers = graphics.bufferSource();

            renderer.renderItem(
                    Items.RED_STAINED_GLASS.getDefaultInstance(), ItemDisplayContext.GUI, false, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY,
                    Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(LDLib2.id("block/renderer_model"))));
            // flush
            RenderSystem.disableDepthTest();
            buffers.endBatch();
            RenderSystem.enableDepthTest();

            if (flag) {
                Lighting.setupFor3DItems();
            }
        } catch (Throwable ignored) {}

        pose.popPose();

        pose.popPose();
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        pose.popPose();
    }

}

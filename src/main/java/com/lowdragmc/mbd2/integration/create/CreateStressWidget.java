package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@LDLRegister(name = "create_stress", group = "widget.container", modID = "create")
public class CreateStressWidget extends Widget implements IConfigurableWidget {
    @Getter
    @Setter
    @Accessors(chain = true)
    public float stress;

    public CreateStressWidget() {
        super(0, 0, 100, 16);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var x = getPositionX();
        var y = getPositionY();
        drawWheel(graphics, x, y, 16, 16);
        graphics.drawString(Minecraft.getInstance().font,
                LocalizationUtils.format("recipe.capability.create_stress.stress.unit", stress),
                x + 16, y + 4, 0xFFFFFF, true);
    }

    @OnlyIn(Dist.CLIENT)
    public void drawWheel(GuiGraphics graphics, float x, float y, int width, int height) {
        var itemW = width * 16f / 16;
        var itemH = height * 16f / 16;
        var itemX = x + (width - itemW) / 2;
        var itemY = y + (height - itemH) / 2;

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
            pose.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
            pose.scale(16.0F, 16.0F, 16.0F);

            var buffers = graphics.bufferSource();

            var itemRenderer = mc.getItemRenderer();
            var stack = AllBlocks.COGWHEEL.asStack();
            var model = itemRenderer.getModel(stack, null, null, 0);

            var angle = AnimationTickHolder.getRenderTime(mc.level) * stress / 4f * 3.0F / 10.0F % 360.0F;
            angle = angle / 180.0F * 3.1415927F;

            ForgeHooksClient.handleCameraTransforms(pose, model, ItemDisplayContext.GUI, false);
            pose.mulPose(new Quaternionf().rotateX(Mth.HALF_PI));
            pose.mulPose(new Quaternionf().rotateY(angle));
            pose.translate(-0.5, -0.5, -0.5);
            itemRenderer.renderModelLists(model, stack, 15728880, OverlayTexture.NO_OVERLAY, pose,
                    buffers.getBuffer(RenderType.solid()));

            // flush
            RenderSystem.disableDepthTest();
            buffers.endBatch();
            RenderSystem.enableDepthTest();

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

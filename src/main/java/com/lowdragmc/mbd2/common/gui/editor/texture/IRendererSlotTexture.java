package com.lowdragmc.mbd2.common.gui.editor.texture;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class IRendererSlotTexture implements IGuiTexture {
    @Nullable
    public static MBDMachineDefinition CURRENT_MACHINE_DEFINITION;

    @Getter @Setter
    private Supplier<IRenderer> rendererSupplier;
    @Setter
    private IGuiTexture slotTexture = new ResourceTexture("ldlib:textures/gui/slot.png");

    public IRendererSlotTexture(Supplier<IRenderer> rendererSupplier) {
        this.rendererSupplier = rendererSupplier;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        slotTexture.draw(graphics, mouseX, mouseY, x, y, width, height);
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
            pose.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
            pose.scale(16.0F, 16.0F, 16.0F);
            boolean flag = !renderer.useBlockLight(ItemStack.EMPTY);
            if (flag) {
                Lighting.setupForFlatItems();
            }
            var buffers = graphics.bufferSource();

            if (Editor.INSTANCE instanceof MachineEditor editor && editor.getCurrentProject() instanceof MachineProject project) {
                CURRENT_MACHINE_DEFINITION = project.getDefinition();
            }
            renderer.renderItem(
                    Items.RED_STAINED_GLASS.getDefaultInstance(), ItemDisplayContext.GUI, false, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY,
                    Minecraft.getInstance().getModelManager().getModel(LDLib.location("block/renderer_model")));
            CURRENT_MACHINE_DEFINITION = null;
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

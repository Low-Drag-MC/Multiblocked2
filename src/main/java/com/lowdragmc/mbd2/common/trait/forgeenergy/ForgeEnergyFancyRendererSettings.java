package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.FancyRendererSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public class ForgeEnergyFancyRendererSettings extends FancyRendererSettings {
    private final ForgeEnergyCapabilityTraitDefinition definition;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.color", tips = "config.definition.trait.fancy_renderer.color.tooltip")
    @ConfigColor
    private int color = 0xaaaa0011;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.percent_height", tips = "config.definition.trait.fancy_renderer.percent_height.tooltip")
    private boolean percentHeight = false;

    public ForgeEnergyFancyRendererSettings(ForgeEnergyCapabilityTraitDefinition definition) {
        this.definition = definition;
    }

    public IRenderer createFancyRenderer() {
        return new ForgeEnergyFancyRendererSettings.Renderer();
    }

    private class Renderer implements IRenderer {
        @Override
        @OnlyIn(Dist.CLIENT)
        public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
            if (blockEntity instanceof IMachineBlockEntity machineBlockEntity
                    && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                if (machine.getTraitByDefinition(definition) instanceof ForgeEnergyCapabilityTrait trait) {
                    var storage = trait.storage;
                    if (storage.getEnergyStored() == 0 || storage.getMaxEnergyStored() == 0) return;

                    poseStack.pushPose();

                    // rotate orientation
                    if (rotateOrientation) {
                        poseStack.translate(0.5D, 0.5d, 0.5D);
                        poseStack.mulPose(ModelFactory.getQuaternion(machine.getFrontFacing().orElse(Direction.NORTH)));
                        poseStack.translate(-0.5D, -0.5d, -0.5D);
                    }

                    // transform
                    poseStack.translate(position.x, position.y, position.z);
                    poseStack.translate(0.5D, 0.5d, 0.5D);
                    // rotation
                    poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
                    // scale
                    poseStack.scale(scale.x, scale.y, scale.z);
                    poseStack.translate(-0.5D, -0.5d, -0.5D);

                    RenderSystem.enableBlend();
                    RenderSystem.enableDepthTest();
                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                    // todo render type
                    var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);

                    RenderBufferUtils.drawCubeFace(poseStack, buffer,
                            0, 0, 0, 1,
                            percentHeight ? storage.getEnergyStored() * 1f / storage.getMaxEnergyStored() : 1, 1,
                            ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color),
                            true);

                    BufferUploader.drawWithShader(buffer.buildOrThrow());
                    poseStack.popPose();
                }
            }
        }

    }
}

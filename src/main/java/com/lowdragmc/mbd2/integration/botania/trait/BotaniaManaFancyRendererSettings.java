package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.FancyRendererSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import vazkii.botania.client.core.helper.RenderHelper;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class BotaniaManaFancyRendererSettings extends FancyRendererSettings {
    private final BotaniaManaCapabilityTraitDefinition definition;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.percent_height", tips = "config.definition.trait.fancy_renderer.percent_height.tooltip")
    private boolean percentHeight = false;

    public BotaniaManaFancyRendererSettings(BotaniaManaCapabilityTraitDefinition definition) {
        this.definition = definition;
    }

    public IRenderer createFancyRenderer() {
        return new Renderer();
    }

    private class Renderer implements IRenderer {
        @Override
        @OnlyIn(Dist.CLIENT)
        public boolean hasTESR(BlockEntity blockEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
            var optional = blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve();
            if (optional.isPresent() && optional.get() instanceof MBDMachine machine) {
                if (machine.getTraitByDefinition(definition) instanceof BotaniaManaCapabilityTrait trait) {
                    var storage = trait.storage;
                    if (storage.getCurrentMana() == 0 || storage.getMaxMana() == 0) return;

                    int mana = storage.getCurrentMana();
                    int maxMana =storage.getMaxMana();

                    float manaLevel = mana * 1f / maxMana;

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

                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableBlend();

                    var texture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(prefix("block/mana_water"));

                    var buffer = bufferSource.getBuffer(RenderHelper.MANA_POOL_WATER);

                    renderCubeFace(poseStack, buffer, 0, 0, 0, 1,
                            percentHeight ? manaLevel : 1, 1, 0xFFFFFFFF,
                            combinedLight, texture.getU0(), texture.getV0(), texture.getU1(), texture.getV1());
                    poseStack.popPose();

                }
            }
        }

        public static void renderCubeFace(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color, int combinedLight,
                                          float u0, float v0, float u1, float v1) {
            Matrix4f mat = poseStack.last().pose();

            buffer.vertex(mat, minX, minY, minZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, minY, maxZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, maxY, maxZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, maxY, minZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();

            buffer.vertex(mat, maxX, minY, minZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, minZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, maxZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, minY, maxZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();


            buffer.vertex(mat, minX, minY, minZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, minY, minZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, minY, maxZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, minY, maxZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();


            buffer.vertex(mat, minX, maxY, minZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, maxY, maxZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, maxZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, minZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();

            buffer.vertex(mat, minX, minY, minZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, maxY, minZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, minZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, minY, minZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();

            buffer.vertex(mat, minX, minY, maxZ).color(color).uv(u0, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, minY, maxZ).color(color).uv(u1, v1).uv2(combinedLight).endVertex();
            buffer.vertex(mat, maxX, maxY, maxZ).color(color).uv(u1, v0).uv2(combinedLight).endVertex();
            buffer.vertex(mat, minX, maxY, maxZ).color(color).uv(u0, v0).uv2(combinedLight).endVertex();
        }

    }
}

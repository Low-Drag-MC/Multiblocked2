package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.FancyRendererSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

public class ChemicalFancyRendererSettings extends FancyRendererSettings {
    private final ChemicalTankCapabilityTraitDefinition definition;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.percent_height", tips = "config.definition.trait.fancy_renderer.percent_height.tooltip")
    private boolean percentHeight = false;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.tank_index", tips = "config.definition.trait.fancy_renderer.tank_index.tooltip")
    private int tankIndex = 0;

    public ChemicalFancyRendererSettings(ChemicalTankCapabilityTraitDefinition definition) {
        this.definition = definition;
    }

    public IRenderer createFancyRenderer() {
        return new Renderer();
    }

    private class Renderer implements IRenderer {
        @Override
        @OnlyIn(Dist.CLIENT)
        public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
            if (!(blockEntity instanceof IMachineBlockEntity machineBlockEntity)) return;
            if (!(machineBlockEntity.getMetaMachine() instanceof MBDMachine machine)) return;
            if (!(machine.getTraitByDefinition(definition) instanceof ChemicalTankCapabilityTrait trait)) return;
            if (tankIndex < 0 || tankIndex >= trait.storages.length) return;
            var storage = trait.storages[tankIndex];
            var stack = storage.getStack();
            if (stack.isEmpty() || storage.getCapacity() == 0) return;

            var sprite = MekanismRenderer.getChemicalTexture(stack);
            if (sprite == null) return;

            poseStack.pushPose();

            if (rotateOrientation) {
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.mulPose(ModelFactory.getQuaternion(machine.getFrontFacing().orElse(Direction.NORTH)));
                poseStack.translate(-0.5D, -0.5D, -0.5D);
            }

            poseStack.translate(position.x, position.y, position.z);
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(rotation.x),
                    (float) Math.toRadians(rotation.y),
                    (float) Math.toRadians(rotation.z)));
            poseStack.scale(scale.x, scale.y, scale.z);
            poseStack.translate(-0.5D, -0.5D, -0.5D);

            float fillHeight = percentHeight
                    ? (float) stack.getAmount() / storage.getCapacity()
                    : 1f;
            int color = stack.getChemicalColorRepresentation() | 0xff000000;

            VertexConsumer builder = buffer.getBuffer(Sheets.translucentCullBlockSheet());
            RenderBufferUtils.renderCubeFace(poseStack, builder, 0, 0, 0, 1, fillHeight, 1, color, combinedLight, sprite);

            poseStack.popPose();
        }
    }
}

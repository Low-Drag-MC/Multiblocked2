package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.FancyRendererSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import vazkii.botania.client.core.helper.RenderHelper;

import static vazkii.botania.api.BotaniaAPI.botaniaRL;

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
        public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
            if (blockEntity instanceof IMachineBlockEntity machineBlockEntity
                    && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine
                    && machine.getTraitByDefinition(definition) instanceof BotaniaManaCapabilityTrait trait) {
                var storage = trait.storage;
                if (storage.getCurrentMana() == 0 || storage.getMaxMana() == 0) return;
                var manaLevel = storage.getCurrentMana() * 1f / storage.getMaxMana();
                poseStack.pushPose();
                if (rotateOrientation) {
                    poseStack.translate(0.5D, 0.5d, 0.5D);
                    poseStack.mulPose(ModelFactory.getQuaternion(machine.getFrontFacing().orElse(Direction.NORTH)));
                    poseStack.translate(-0.5D, -0.5d, -0.5D);
                }
                poseStack.translate(position.x, position.y, position.z);
                poseStack.translate(0.5D, 0.5d, 0.5D);
                poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
                poseStack.scale(scale.x, scale.y, scale.z);
                poseStack.translate(-0.5D, -0.5d, -0.5D);
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableBlend();
                var texture = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(botaniaRL("block/mana_water"));
                var buffer = bufferSource.getBuffer(RenderHelper.MANA_POOL_WATER);
                RenderBufferUtils.renderCubeFace(poseStack, buffer, 0, 0, 0, 1, percentHeight ? manaLevel : 1, 1, 0xFFFFFFFF, combinedLight, texture);
                poseStack.popPose();
            }
        }
    }
}

package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FluidFancyRendererSettings implements IToggleConfigurable {
    private final FluidTankCapabilityTraitDefinition definition;
    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.position", tips = "config.definition.trait.fancy_renderer.position.tooltip")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private Vector3f position = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.rotation", tips = "config.definition.trait.fancy_renderer.rotation.tooltip")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private Vector3f rotation = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.scale", tips = "config.definition.trait.fancy_renderer.scale.tooltip")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private Vector3f scale = new Vector3f(1, 1, 1);
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.rotate_orientation", tips = "config.definition.trait.fancy_renderer.rotate_orientation.tooltip")
    private boolean rotateOrientation = true;

    // run-time;
    private IRenderer renderer;

    public FluidFancyRendererSettings(FluidTankCapabilityTraitDefinition definition) {
        this.definition = definition;
    }

    public IRenderer createRenderer() {
        if (isEnable()) {
            return renderer == null ? (renderer = new FluidFancyRendererSettings.Renderer()) : renderer;
        } else return IRenderer.EMPTY;
    }

    private class Renderer implements IRenderer {
        @Override
        @OnlyIn(Dist.CLIENT)
        public boolean hasTESR(BlockEntity blockEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
            var optional = blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve();
            if (optional.isPresent() && optional.get() instanceof MBDMachine machine) {
                if (machine.getTraitByDefinition(definition) instanceof FluidTankCapabilityTrait trait) {
                    FluidStack fluid = trait.storages[0].getFluid();
                    if (fluid.isEmpty()) return;

                    var fluidTexture = FluidHelper.getStillTexture(fluid);
                    if (fluidTexture == null) {
                        fluidTexture = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
                    }
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

                    VertexConsumer builder = buffer.getBuffer(Sheets.translucentCullBlockSheet());
                    RenderBufferUtils.renderCubeFace(poseStack, builder, 0, 0, 0, 1, 1, 1, FluidHelper.getColor(fluid) | 0xff000000, combinedLight, fluidTexture);
                    poseStack.popPose();
                }
            }
        }

    }
}

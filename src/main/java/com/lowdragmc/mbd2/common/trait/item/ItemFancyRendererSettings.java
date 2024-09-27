package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.FancyRendererSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemFancyRendererSettings extends FancyRendererSettings {
    private final ItemSlotCapabilityTraitDefinition definition;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.spin", tips = "config.definition.trait.fancy_renderer.spin.tooltip")
    @NumberRange(range = {0, Float.MAX_VALUE})
    private float spin = 0;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fancy_renderer.render_stack", tips = "config.definition.trait.fancy_renderer.render_stack.tooltip")
    private boolean renderStack = true;

    public ItemFancyRendererSettings(ItemSlotCapabilityTraitDefinition definition) {
        this.definition = definition;
    }

    @Override
    public IRenderer createFancyRenderer() {
        return new Renderer();
    }

    private class Renderer implements IRenderer {
        private final static RandomSource RANDOM = RandomSource.create();

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
                if (machine.getTraitByDefinition(definition) instanceof ItemSlotCapabilityTrait trait) {
                    var itemRenderer = Minecraft.getInstance().getItemRenderer();
                    float tick = blockEntity.getLevel().getGameTime() + partialTicks;
                    for (int i = 0; i < trait.storage.getSlots(); i++) {
                        var itemStack = trait.storage.getStackInSlot(i);
                        if (itemStack.isEmpty()) continue;

                        var bakedmodel = itemRenderer.getModel(itemStack, Minecraft.getInstance().level, null, Item.getId(itemStack.getItem()) + itemStack.getDamageValue());
                        var isGui3d = bakedmodel.isGui3d();
                        var renderAmount = renderStack ? getRenderAmount(itemStack) : 1;
                        RANDOM.setSeed(itemStack.isEmpty() ? 187 : Item.getId(itemStack.getItem()) + itemStack.getDamageValue());

                        poseStack.pushPose();

                        // rotate orientation
                        if (rotateOrientation) {
                            poseStack.translate(0.5D, 0.5d, 0.5D);
                            poseStack.mulPose(ModelFactory.getQuaternion(machine.getFrontFacing().orElse(Direction.NORTH)));
                            poseStack.translate(-0.5D, -0.5d, -0.5D);
                        }

                        // transform
                        poseStack.translate(position.x, position.y, position.z);
                        // self-rotation
                        if (spin != 0) {
                            poseStack.mulPose(new Quaternionf().rotateAxis(spin * tick * Mth.TWO_PI / 80, 0, 1, 0));
                        }
                        poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
                        // scale
                        poseStack.scale(scale.x, scale.y, scale.z);

                        for (int j = 0; j < renderAmount; j++) {
                            poseStack.pushPose();
                            // offset
                            if (isGui3d) { // e.g. blocks
                                float rx = 0, ry = 0, rz = 0;
                                if (renderAmount > 1) {
                                    rx = (RANDOM.nextFloat() * 2.0f - 1.0f) * 0.06f;
                                    ry = (RANDOM.nextFloat() * 2.0f - 1.0f) * 0.06f;
                                    rz = (RANDOM.nextFloat() * 2.0f - 1.0f) * 0.06f;
                                }
                                poseStack.translate(rx, ry, rz);
                            } else { // e.g. ingots
                                var offset = j / 10f - (renderAmount - 1) / 20f;
                                poseStack.translate(offset, offset, offset);
                            }

                            itemRenderer.render(itemStack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, combinedLight, combinedOverlay, bakedmodel);
                            poseStack.popPose();
                        }
                        poseStack.popPose();
                    }
                }
            }
        }

        private int getRenderAmount(ItemStack stack) {
            int i = 1;
            if (stack.getCount() > 16) {
                i = 3;
            } else if (stack.getCount() > 1) {
                i = 2;
            }
            return i;
        }
    }
}

package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@AllArgsConstructor
public class MBDItemRenderer implements IRenderer {
    protected final BooleanSupplier useBlockLight;
    protected final BooleanSupplier isGui3d;
    protected final Supplier<IRenderer> renderer;

    @Override
    public boolean useBlockLight(ItemStack stack) {
        return useBlockLight.getAsBoolean();
    }

    @Override
    public boolean isGui3d() {
        return isGui3d.getAsBoolean();
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        renderer.get().renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }
}

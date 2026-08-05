package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.HashMap;

@LDLRegisterClient(
    name = "custom_script",
    registry = "ldlib2:renderer"
)
@Getter
@Setter
@OnlyIn(Dist.CLIENT)
public class KubeJSRenderer implements IRenderer {
    public static final HashMap<ResourceLocation, RenderSupplier> renderFunctions = new HashMap<>();

    @Configurable
    public ResourceLocation rendererName = ResourceLocation.parse("mbd2:beacon");

    @Configurable
    public CompoundTag data;

    @Configurable
    public boolean renderOffScreen = false;

    public KubeJSRenderer() {
        data = new CompoundTag();
        data.putInt("color", -1);
    }

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if(renderFunctions.containsKey(rendererName)) {
            stack.pushPose();
            try {
                renderFunctions.get(rendererName).render(blockEntity, partialTick, stack, buffer, combinedLight, combinedOverlay, data);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
            stack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return renderOffScreen;
    }

    public interface RenderSupplier {
        void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, CompoundTag data);
    }
}

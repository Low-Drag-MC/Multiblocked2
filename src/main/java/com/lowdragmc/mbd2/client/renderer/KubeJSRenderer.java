package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.KubeJSContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public class KubeJSRenderer implements IRenderer {
    public static final HashMap<ResourceLocation, RenderSupplier> renderFunctions = new HashMap<>();

    public ResourceLocation rendererName;

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if(renderFunctions.containsKey(rendererName)) {
            try {
                renderFunctions.get(rendererName).render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
            } catch(Exception e) {
                KubeJSContext.reportRuntimeError(e.getLocalizedMessage(), KubeJS.getClientScriptManager().contextFactory.enter());
            }
        }
    }

    public interface RenderSupplier {
        void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay);
    }
}

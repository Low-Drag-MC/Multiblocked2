package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.HashMap;

@LDLRegisterClient(name = "kubejs", group = "renderer")
@Getter
@Setter
@OnlyIn(Dist.CLIENT)
public class KubeJSRenderer implements ISerializableRenderer {
    public static final HashMap<ResourceLocation, RenderSupplier> renderFunctions = new HashMap<>();

    @Configurable
    public ResourceLocation rendererName = ResourceLocation.parse("mbd2:beacon");

    @Configurable
    public CompoundTag data;

    @Configurable
    public boolean renderOffScreen = false;

    @Configurable
    @NumberRange(range = {1, Integer.MAX_VALUE})
    @Getter
    public int maxRenderDistance = 64;

    public KubeJSRenderer() {
        data = new CompoundTag();
        data.putInt("color", -1);
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
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
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return renderOffScreen;
    }

    public interface RenderSupplier {
        void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, CompoundTag data);
    }
}

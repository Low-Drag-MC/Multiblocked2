package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.MBD2;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@LDLRegisterClient(name = "custom", group = "renderer")
@Getter
@Setter
@OnlyIn(Dist.CLIENT)
public class CustomRenderer implements ISerializableRenderer {
    public static final HashMap<ResourceLocation, RenderSupplier> javaRenderFunctions = new HashMap<>();
    public static final HashMap<ResourceLocation, RenderSupplier> javaScriptRenderFunctions = new HashMap<>();
    private static final Set<ResourceLocation> ERRORED_RENDERERS = new HashSet<>();

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

    public CustomRenderer() {
        data = new CompoundTag();
        data.putInt("color", -1);
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        // renderers registered by KubeJS take priority over the java ones with the same name.
        var renderFunction = javaScriptRenderFunctions.get(rendererName);
        if (renderFunction == null) {
            renderFunction = javaRenderFunctions.get(rendererName);
        }
        if (renderFunction == null) return;
        stack.pushPose();
        try {
            renderFunction.render(blockEntity, partialTick, stack, buffer, combinedLight, combinedOverlay, data);
        } catch (Exception e) {
            if (ERRORED_RENDERERS.add(rendererName)) {
                MBD2.LOGGER.error("Error while rendering the custom renderer {}", rendererName, e);
            }
        } finally {
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

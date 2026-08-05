package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.client.renderer.KubeJSRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.awt.*;

import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;

public class CustomRendererEvent extends Event implements KubeEvent {
    public CustomRendererEvent() {
        KubeJSRenderer.renderFunctions.clear();
        createExampleRenderFunction();
    }

    public void addRenderer(ResourceLocation name, KubeJSRenderer.RenderSupplier renderer) {
        KubeJSRenderer.renderFunctions.put(name, renderer);
    }
    
    private void createExampleRenderFunction() {
        KubeJSRenderer.renderFunctions.put(ResourceLocation.parse("mbd2:beacon"), (be, partialTick, stack, bufferSource, combinedLight, combinedOverlay, data) -> {
            if(be.getLevel() != null) BeaconRenderer.renderBeaconBeam(stack, bufferSource, data.contains("texture") ? ResourceLocation.parse(data.getString("texture")) : BEAM_LOCATION, partialTick, 1, be.getLevel().getGameTime(), data.contains("y_offset") ? data.getInt("y_offset") : 0, data.contains("height") ? data.getInt("height") : 1000, data.contains("color") ? data.getInt("color") : Color.WHITE.getRGB(), data.contains("radius") ? data.getFloat("radius") : 0.2f, data.contains("glow_radius") ? data.getInt("glow_radius") : 0.25f);
        });
    }
}

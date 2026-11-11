package com.lowdragmc.mbd2.common.event;

import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.mbd2.client.renderer.KubeJSRenderer;
import mezz.jei.library.color.ColorHelper;
import mezz.jei.library.color.ColorUtil;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.awt.*;

import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;

@SuppressWarnings("unused")
public class CustomRendererEvent extends Event {
    public CustomRendererEvent() {
        KubeJSRenderer.renderFunctions.clear();
        createExampleRenderFunction();
    }

    public void addRenderer(ResourceLocation name, KubeJSRenderer.RenderSupplier renderer) {
        KubeJSRenderer.renderFunctions.put(name, renderer);
    }
    
    private void createExampleRenderFunction() {
        KubeJSRenderer.renderFunctions.put(ResourceLocation.parse("mbd2:beacon"), (be, partialTick, stack, bufferSource, combinedLight, combinedOverlay, data) -> {
            if(be.getLevel() != null) BeaconRenderer.renderBeaconBeam(stack, bufferSource, data.contains("texture") ? ResourceLocation.parse(data.getString("texture")) : BEAM_LOCATION, partialTick, 1, be.getLevel().getGameTime(), data.contains("y_offset") ? data.getInt("y_offset") : 0, data.contains("height") ? data.getInt("height") : 1000, intColorToFloatArray(data.contains("color") ? data.getInt("color") : Color.WHITE.getRGB()), data.contains("radius") ? data.getFloat("radius") : 0.2f, data.contains("glow_radius") ? data.getInt("glow_radius") : 0.25f);
        });
    }

    private float[] intColorToFloatArray(int color) {
        return new float[]{ColorUtils.red(color),ColorUtils.green(color),ColorUtils.blue(color)};
    }
}

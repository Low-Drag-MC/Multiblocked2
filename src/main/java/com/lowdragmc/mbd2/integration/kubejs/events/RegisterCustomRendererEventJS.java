package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.client.renderer.CustomRenderer;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class RegisterCustomRendererEventJS extends EventJS {
    public RegisterCustomRendererEventJS() {
        CustomRenderer.javaScriptRenderFunctions.clear();
    }

    /**
     * Client only. Renderers registered here take priority over those registered in {@link com.lowdragmc.mbd2.common.event.RegisterCustomRendererEvent}.
     */
    public static void postEvent() {
        MBDClientEvents.CUSTOM_RENDER.post(new RegisterCustomRendererEventJS());
    }

    public void addRenderer(ResourceLocation name, CustomRenderer.RenderSupplier renderer) {
        CustomRenderer.javaScriptRenderFunctions.put(name, renderer);
    }
}

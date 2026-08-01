package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.client.renderer.KubeJSRenderer;
import dev.latvian.mods.kubejs.event.KubeEvent;
import javafx.event.Event;
import javafx.event.EventType;
import net.minecraft.resources.ResourceLocation;

public class CustomRendererEvent extends Event implements KubeEvent {
    public CustomRendererEvent(EventType<? extends Event> eventType) {
        super(eventType);
        KubeJSRenderer.renderFunctions.clear();
    }

    public void addRenderer(ResourceLocation name, KubeJSRenderer.RenderSupplier renderer) {
        KubeJSRenderer.renderFunctions.put(name, renderer);
    }
}

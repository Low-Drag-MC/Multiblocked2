package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.client.renderer.custom.CustomRenderer;
import com.lowdragmc.mbd2.client.renderer.custom.CustomRendererBuilder;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Fired on the client after client scripts loaded, so that scripts can contribute named block entity render
 * logic that machines can then point at through the {@code custom_script} renderer.
 * <pre>{@code
 * MBDClientEvents.registerCustomRenderers(event => {
 *     event.register('mypack:beam', ctx => {
 *         BeaconRenderer.renderBeaconBeam(ctx.poseStack, ctx.bufferSource, BEAM_LOCATION,
 *             ctx.partialTick, 1, ctx.gameTime, 0, 256, ctx.data.getInt('color'), 0.2, 0.25)
 *     }).viewDistance(256).boundingBoxInflate(0, 256, 0)
 * })
 * }</pre>
 */
@OnlyIn(Dist.CLIENT)
public class CustomRendererRegistryEventJS implements KubeEvent {
    private final Map<ResourceLocation, CustomRendererBuilder> renderers = new HashMap<>();

    /**
     * Register a renderer under the given name and configure it further through the returned builder.
     */
    public CustomRendererBuilder register(ResourceLocation name) {
        var builder = new CustomRendererBuilder();
        renderers.put(name, builder);
        return builder;
    }

    /**
     * Shorthand for {@code register(name).onRender(callback)}.
     */
    public CustomRendererBuilder register(ResourceLocation name, CustomRenderer.RenderCallback callback) {
        return register(name).onRender(callback);
    }

    public Map<ResourceLocation, CustomRendererBuilder> getRenderers() {
        return renderers;
    }
}

package com.lowdragmc.mbd2.utils;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import org.jetbrains.annotations.Nullable;

public class RendererUtils {

    /**
     * Resolve the renderer that actually draws. Anything picked in the editor's resource panel is
     * stored as a {@link UIResourceRenderer} pointing at the resource, so callers that need the
     * concrete renderer type - e.g. to reach a geckolib animation controller - have to unwrap it.
     */
    public static IRenderer resolve(@Nullable IRenderer renderer) {
        while (renderer instanceof UIResourceRenderer uiResourceRenderer) {
            renderer = uiResourceRenderer.getInternalRenderer();
        }
        return renderer == null ? IRenderer.EMPTY : renderer;
    }
}

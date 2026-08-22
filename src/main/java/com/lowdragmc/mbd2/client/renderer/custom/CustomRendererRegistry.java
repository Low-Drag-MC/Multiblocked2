package com.lowdragmc.mbd2.client.renderer.custom;

import com.lowdragmc.mbd2.MBD2;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side registry of the named {@link CustomRenderer}s that
 * {@link com.lowdragmc.mbd2.client.renderer.CustomScriptRenderer} can point at.
 * <p>
 * The whole content is replaced every time client scripts reload, so a pack author can iterate on the render
 * logic with F3 + T without restarting the game. Renderer instances resolve their entry lazily, per frame,
 * which is what makes that hot reload work.
 * <p>
 * Everything here runs on the render thread except {@link #reload}, which runs on the script reload thread,
 * hence the volatile immutable map swap.
 */
@OnlyIn(Dist.CLIENT)
public class CustomRendererRegistry {
    private static volatile Map<ResourceLocation, CustomRenderer> renderers = Map.of();
    /**
     * Renderers that threw while drawing. They are skipped until the next script reload so that a broken script
     * reports once instead of crashing the game, or spamming the log sixty times a second.
     */
    private static final Set<ResourceLocation> broken = ConcurrentHashMap.newKeySet();

    private CustomRendererRegistry() {
    }

    /**
     * Replace every registered renderer. Called after client scripts finished loading.
     */
    public static void reload(Map<ResourceLocation, ? extends CustomRenderer> newRenderers) {
        renderers = Map.copyOf(newRenderers);
        broken.clear();
    }

    /**
     * @return the renderer registered under the given name, or null if there is none or it has been disabled
     *         after throwing.
     */
    @Nullable
    public static CustomRenderer get(@Nullable ResourceLocation name) {
        return name == null || broken.contains(name) ? null : renderers.get(name);
    }

    /**
     * Disable a renderer for the rest of the session, or until scripts reload, and report why.
     */
    public static void markBroken(ResourceLocation name, Throwable throwable) {
        if (broken.add(name)) {
            MBD2.LOGGER.error("Custom renderer '{}' threw while rendering and has been disabled. "
                    + "Fix the script and reload client scripts to re-enable it.", name, throwable);
        }
    }

    /**
     * @return the currently registered names, sorted, for display in the machine editor.
     */
    public static List<ResourceLocation> getNames() {
        return renderers.keySet().stream().sorted(ResourceLocation::compareTo).toList();
    }
}

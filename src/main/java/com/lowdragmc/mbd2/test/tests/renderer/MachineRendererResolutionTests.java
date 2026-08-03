package com.lowdragmc.mbd2.test.tests.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.utils.RendererUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Issue #226: anything picked in the editor's resource panel is stored as a
 * {@link UIResourceRenderer} pointing at the resource, so code that tests the machine renderer
 * with {@code instanceof} (e.g. {@code triggerGeckolibAnim}) has to resolve the wrapper first.
 */
@GameTestHolder(MBD2.MOD_ID)
public class MachineRendererResolutionTests {

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void resource_backed_renderer_is_unwrapped(GameTestHelper h) {
        var wrapped = new UIResourceRenderer(new BuiltinPath("built-in:empty"));
        var resolved = RendererUtils.resolve(wrapped);
        if (resolved instanceof UIResourceRenderer) {
            h.fail("resolve() handed back the UIResourceRenderer wrapper instead of the renderer behind it");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void plain_renderer_is_returned_unchanged(GameTestHelper h) {
        var renderer = new IModelRenderer(MBD2.id("block/machine"));
        if (RendererUtils.resolve(renderer) != renderer) {
            h.fail("resolve() must return a non-wrapped renderer untouched");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void null_renderer_resolves_to_empty(GameTestHelper h) {
        if (RendererUtils.resolve(null) != IRenderer.EMPTY) {
            h.fail("resolve(null) should fall back to IRenderer.EMPTY");
            return;
        }
        h.succeed();
    }
}

package com.lowdragmc.mbd2.test.uitest.wiki;

import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Wiki video of the recipe logic settings in the machine editor. */
@LDLRegisterClient(name = "wiki_mbd2_recipe_logic_editor", group = "mbd2_wiki", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WikiRecipeLogicEditorRecording implements UIRecording {
    private static final String SETTINGS = "settings";
    private static final String GROUP = "config.definition.recipe_logic_settings";
    private static final String LIST = "config.recipe_logic_settings.additional_recipe_logics";

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("wiki", "mbd2").requiresWorld(true).guiScale(4).scenarioTimeoutMs(600_000);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("the machine editor", WikiRecipeLogicEditorRecording::editor)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(10);
        toTop(s, "Has Recipe Logic", ctx -> group(ctx, GROUP).lineContainer)
                .step("tag the threads field", ctx -> field(group(ctx, GROUP), "config.recipe_logic_settings.threads").setId("wiki_threads"))
                .take("recipe-logic-settings", o -> o.size(1920, 1080).fps(30).poster(6_000), t -> {
                    say(t, "A machine's recipe logic is set up under Has Recipe Logic, in Basic Settings.",
                            "机器的配方逻辑在 Basic Settings 的 Has Recipe Logic 里设置。");
                    t.zoomTo("Has Recipe Logic", ctx -> around(ctx, group(ctx, GROUP).lineContainer), 700).hold(3000);
                    say(t, "Name is what slot traits, pattern predicates and blueprints call this logic.",
                            "Name 是槽位 Trait、样式谓词和蓝图称呼这个逻辑用的名字。");
                    t.hold(3000);
                    say(t, "Threads runs that many copies of it side by side, each with a recipe of its own.",
                            "Threads 让它的若干份副本并排运行，每份各自运行一条配方。");
                    t.typeInto("#wiki_threads", "3").hold(2500);
                    say(t, "At the bottom, Additional Recipe Logics adds logics with their own name, recipe type and settings.",
                            "最下方的 Additional Recipe Logics 可以添加更多逻辑，各有自己的名字、配方类型和设置。");
                    toTop(t, "Additional Recipe Logics", ctx -> group(ctx, LIST).lineContainer)
                            .zoomTo("Additional Recipe Logics", ctx -> around(ctx, group(ctx, LIST).lineContainer), 600)
                            .step("tag the list", ctx -> {
                                var list = (ArrayConfiguratorGroup<?>) group(ctx, LIST);
                                list.lineContainer.setId("wiki_list_title");
                                list.addButton.setId("wiki_list_add");
                            })
                            .click("#wiki_list_title")
                            .hold(600)
                            .click("#wiki_list_add")
                            .waitUntil("a logic was added", ctx -> settings(ctx).additionalRecipeLogics().size() == 1)
                            .frames(4);
                    toTop(t, "Additional Recipe Logics", ctx -> group(ctx, LIST).lineContainer)
                            .zoomTo("the new entry", ctx -> around(ctx, group(ctx, LIST).lineContainer), 500)
                            .step("tag the new entry's name", ctx ->
                                    field(entry(group(ctx, LIST)), "config.recipe_logic_settings.name").setId("wiki_entry_name"))
                            .hold(2000);
                    say(t, "Name it, here smelt. Its own checkbox switches it off without deleting it.",
                            "给它起个名字，这里叫 smelt。条目自己的勾选框可以关掉它而不删除。");
                    t.typeInto("#wiki_entry_name", "smelt").hold(3000);
                    say(t, "Switch Has Recipe Logic off and the machine has no recipe logic at all, additional ones included.",
                            "关闭 Has Recipe Logic 后，机器就完全没有配方逻辑了，额外逻辑也一样。");
                    toTop(t, "Has Recipe Logic", ctx -> group(ctx, GROUP).lineContainer)
                            .zoomTo("Has Recipe Logic", ctx -> around(ctx, group(ctx, GROUP).lineContainer), 600)
                            .step("tag the switch", ctx -> descendant(group(ctx, GROUP).lineContainer, Toggle.class).setId("wiki_logic_toggle"))
                            .click("#wiki_logic_toggle")
                            .frames(4)
                            .zoomTo("the folded group", ctx -> around(ctx, group(ctx, GROUP).lineContainer), 600)
                            .hold(2500)
                            .step("tag the switch again", ctx -> descendant(group(ctx, GROUP).lineContainer, Toggle.class).setId("wiki_logic_toggle"))
                            .click("#wiki_logic_toggle")
                            .hold(1500);
                    say(t, "", "");
                    t.zoomOut(500).hold(300);
                })
                .check("threads, the added logic and the switch ended where the video says", ctx -> {
                    var settings = settings(ctx);
                    return settings.threads() == 3 && settings.isEnable()
                            && settings.additionalRecipeLogics().size() == 1
                            && "smelt".equals(settings.additionalRecipeLogics().getFirst().name());
                })
                .closeScreen();
    }

    /** Scrolls the inspector until {@code element} sits at its top, or as near as the scroll range allows. */
    private static ScenarioBuilder toTop(ScenarioBuilder s, String description, Function<TestContext, UIElement> element) {
        return s.waitUntil(description + " at the top of the inspector", ctx -> {
            var scroller = inspector(ctx);
            var dy = ElementBounds.of(element.apply(ctx)).y() - ElementBounds.of(scroller.viewPort).y() - 4;
            var range = scroller.getContainerHeight() - scroller.viewPort.getContentHeight();
            if (Math.abs(dy) <= 1.5f || range <= 0) return true;
            var value = scroller.verticalScroller.getValue();
            var next = Math.clamp(value + dy / range, 0f, 1f);
            if (next == value) return true;
            scroller.verticalScroller.setValue(next);
            return false;
        });
    }

    private static ModularUI editor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        ctx.put(SETTINGS, project.getDefinition().recipeLogicSettings());
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static ConfigRecipeLogicSettings settings(TestContext ctx) {
        return ctx.get(SETTINGS);
    }

    private static void say(ScenarioBuilder t, String en, String zh) {
        t.caption("en", en).caption("zh", zh);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    /** The configurator group titled {@code key}. */
    private static ConfiguratorGroup group(TestContext ctx, String key) {
        var title = text(key);
        return ctx.query().type(ConfiguratorGroup.class).list().stream()
                .map(ref -> ref.as(ConfiguratorGroup.class))
                .filter(group -> title.equals(group.label.getText().getString()))
                .findFirst().orElseThrow(() -> new IllegalStateException("no group titled " + title));
    }

    /** The first entry of a list. */
    private static ConfiguratorGroup entry(ConfiguratorGroup list) {
        var entries = new ArrayList<ConfiguratorGroup>();
        collect(list.configuratorContainer, ConfiguratorGroup.class, entries);
        if (entries.isEmpty()) throw new IllegalStateException("the list has no entry");
        return entries.getFirst();
    }

    /** The text field of the row labelled {@code key} directly in {@code group}. */
    private static TextField field(ConfiguratorGroup group, String key) {
        var label = text(key);
        var rows = new ArrayList<Configurator>();
        collect(group.configuratorContainer, Configurator.class, rows);
        for (var row : rows) {
            if (row instanceof ConfiguratorGroup || !label.equals(row.label.getText().getString())) continue;
            if (nearestGroup(row) != group) continue;
            return descendant(row.inlineContainer, TextField.class);
        }
        throw new IllegalStateException("no " + label + " row in " + group.label.getText().getString());
    }

    private static ConfiguratorGroup nearestGroup(UIElement element) {
        for (var e = element.getParent(); e != null; e = e.getParent()) {
            if (e instanceof ConfiguratorGroup group) return group;
        }
        return null;
    }

    private static <T extends UIElement> T descendant(UIElement root, Class<T> type) {
        var found = new ArrayList<T>();
        collect(root, type, found);
        if (found.isEmpty()) throw new IllegalStateException("no " + type.getSimpleName() + " under " + root);
        return found.getFirst();
    }

    private static <T extends UIElement> void collect(UIElement element, Class<T> type, List<T> into) {
        for (var child : element.getChildren()) {
            if (type.isInstance(child)) into.add(type.cast(child));
            collect(child, type, into);
        }
    }

    /** A 16:9 piece of the screen at the inspector's right edge, from just above {@code element} down. */
    private static float[] around(TestContext ctx, UIElement element) {
        var viewPort = ElementBounds.of(inspector(ctx).viewPort);
        var width = viewPort.width() * 1.9f;
        var height = width * 9 / 16;
        var y = Math.clamp(ElementBounds.of(element).y() - height * 0.12f, viewPort.y(), viewPort.y() + viewPort.height() - height);
        return new float[]{viewPort.x() + viewPort.width() - width, y, width, height};
    }

    /** The tallest scroller in the inspector: the one holding the inspected settings. */
    private static ScrollerView inspector(TestContext ctx) {
        var editor = ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
        var scrollers = new ArrayList<ScrollerView>();
        collect(editor.inspectorView, ScrollerView.class, scrollers);
        return scrollers.stream().max((a, b) -> Float.compare(a.getSizeHeight(), b.getSizeHeight())).orElseThrow();
    }
}

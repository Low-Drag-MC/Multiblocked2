package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Renders the editor panels behind several recipe logics: the primary's name and threads with its list of additional
 * logics, and a trait's recipe logic scope.
 *
 * <p>All three are generated from {@code @Configurable} fields, the list through a {@code @ConfigList} whose
 * entries are toggle groups. Whether that renders, row by row, has no headless equivalent.</p>
 */
@LDLRegisterClient(name = "mbd2_multi_recipe_logic_settings", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MultiRecipeLogicSettingsScenario implements UIScenario {

    private static final String LIST = "config.recipe_logic_settings.additional_recipe_logics";
    private static final String PRIMARY = "primary";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "recipe_logic", "visual").requiresWorld(true).guiScale(1);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 multi recipe logic settings", ctx -> createUI(ctx))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .frames(3)
                .check("the additional logic list is labelled", ctx -> resolves(LIST))
                .check("a logic's name is labelled", ctx -> resolves("config.recipe_logic_settings.name"))
                .check("a logic's threads are labelled", ctx -> resolves("config.recipe_logic_settings.threads"))
                .check("a trait's logic scope is labelled", ctx -> resolves("config.definition.trait.recipe_logics"))
                .step("expand every group", ctx -> ctx.query().type(ConfiguratorGroup.class).list()
                        .forEach(ref -> ref.as(ConfiguratorGroup.class).setCollapse(false)))
                .frames(4)
                // the primary plus both list entries each show a name row and a threads row
                .check("every logic shows its name row", ctx ->
                        ctx.query().withText(text("config.recipe_logic_settings.name")).visible().count() >= 3)
                .check("every logic shows its threads row", ctx ->
                        ctx.query().withText(text("config.recipe_logic_settings.threads")).visible().count() >= 3)
                .check("an entry shows its recipe type row", ctx ->
                        ctx.query().withText(text("editor.machine.recipe_type")).visible().count() >= 3)
                .check("the list shows once, inside the primary's group", ctx -> {
                    var lists = shownLists(ctx);
                    if (lists.size() != 1 || !inPrimaryGroup(ctx, lists.get(0))) {
                        throw new AssertionError("shown list labels: " + lists);
                    }
                    return true;
                })
                .check("the trait shows its logic scope row", ctx ->
                        ctx.query().withText(text("config.definition.trait.recipe_logics")).visible().count() >= 1)
                .screenshotElement("settings-top", "#root")
                .step("scroll to the middle", ctx -> {
                    var scroller = ctx.query("#root").one().as(ScrollerView.class).verticalScroller;
                    scroller.setValue(Float.MAX_VALUE);
                    scroller.setValue(scroller.getValue() * 0.42f);
                })
                .frames(4)
                .screenshotElement("settings-middle", "#root")
                .step("scroll to the bottom", ctx -> ctx.query("#root").one().as(ScrollerView.class)
                        .verticalScroller.setValue(Float.MAX_VALUE))
                .frames(4)
                .screenshotElement("settings-bottom", "#root")
                .step("switch Has Recipe Logic off", ctx -> ctx.<ConfigRecipeLogicSettings>get(PRIMARY).setEnable(false))
                .frames(4)
                .check("the list is hidden with the rest of the group", ctx -> shownLists(ctx).isEmpty())
                .closeScreen();
    }

    private static boolean resolves(String key) {
        return !key.equals(Component.translatable(key).getString());
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    /**
     * The list labels on screen, innermost only: a row's container carries its label's text too, and
     * {@code visible()} looks at the element alone while a collapsed group hides its content.
     */
    private static List<UIElement> shownLists(TestContext ctx) {
        var matches = ctx.query().withText(text(LIST)).list().stream().map(ref -> ref.as(UIElement.class)).toList();
        return matches.stream()
                .filter(element -> matches.stream().noneMatch(other -> other != element && isAncestor(element, other)))
                .filter(element -> {
                    for (var e = element; e != null; e = e.getParent()) {
                        if (!e.isVisible() || !e.isDisplayed()) return false;
                    }
                    return true;
                })
                .toList();
    }

    private static boolean isAncestor(UIElement ancestor, UIElement element) {
        for (var e = element.getParent(); e != null; e = e.getParent()) {
            if (e == ancestor) return true;
        }
        return false;
    }

    private static boolean inPrimaryGroup(TestContext ctx, UIElement element) {
        return isAncestor(ctx.query("#" + PRIMARY).one().as(UIElement.class), element);
    }

    private ModularUI createUI(TestContext ctx) {
        var root = new ScrollerView();
        root.layout(layout -> {
            layout.width(300);
            layout.height(980);
        }).setId("root");

        var column = new UIElement();

        var primary = ConfigRecipeLogicSettings.builder().name("main").threads(2).build();
        primary.additionalRecipeLogics().add(ConfigRecipeLogicSettings.builder().name("smelt").keepWhenDisabled(true).build());
        primary.additionalRecipeLogics().add(ConfigRecipeLogicSettings.builder().name("alloy").threads(3).keepWhenDisabled(true).build());
        ctx.put(PRIMARY, primary);
        var primaryGroup = new ConfiguratorGroup("config.definition.recipe_logic_settings");
        primaryGroup.setId(PRIMARY);
        primaryGroup.setCollapse(false);
        primary.buildConfigurator(primaryGroup);
        column.addChild(primaryGroup);

        var trait = new ItemSlotCapabilityTraitDefinition();
        trait.setRecipeLogics(List.of("smelt", "alloy#1"));
        var traitGroup = new ConfiguratorGroup("trait");
        traitGroup.setCollapse(false);
        trait.buildConfigurator(traitGroup);
        column.addChild(traitGroup);

        return new ModularUI(UI.of(root.addScrollViewChild(column)), ctx.requirePlayer());
    }
}

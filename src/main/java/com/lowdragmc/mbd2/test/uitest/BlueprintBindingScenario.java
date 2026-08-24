package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintConfigurator;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;

/**
 * Renders the machine settings panel with blueprints attached.
 *
 * <p>This is the half of "does using a blueprint work" that lives entirely in the editor: the
 * {@code @ConfigList} row, the blueprint picker, and — the part with no headless equivalent — the
 * parameter rows {@code MachineBlueprintBinding} generates from the referenced graph's INPUT
 * variables. Those rows are built by reflecting over a {@code TypeHandle} into a
 * {@code ConfiguratorAccessor}, so "the variable exists" and "a row for it renders" are genuinely
 * different claims.</p>
 *
 * <p>Two bindings, because they exercise different halves: the first is inlined and carries the
 * parameters (and so needs nothing on disk), the second is a plain path reference and is what the
 * picker row is checked against.</p>
 */
@LDLRegisterClient(name = "mbd2_blueprint_binding", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class BlueprintBindingScenario implements UIScenario {

    /** The exposed parameters the fixture blueprint declares, and the label each row must carry. */
    private static final String INTERVAL = "interval";
    private static final String TARGET_STATE = "targetState";
    private static final String ENABLED = "enabled";
    /** A path that deliberately resolves to nothing, so the picker's "missing" state is rendered. */
    private static final String DANGLING = new FilePath("no_such_blueprint").getPathWithType();

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "blueprint", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 blueprint binding", ctx -> createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .frames(3)
                .check("the blueprint list is labelled", ctx ->
                        resolves("config.machine_settings.blueprints"))
                .check("the picker row is labelled", ctx ->
                        resolves("config.machine_blueprint.blueprint"))
                .check("the parameters group is labelled", ctx ->
                        resolves("config.machine_blueprint.parameters"))
                .step("expand every group", ctx -> ctx.query().type(ConfiguratorGroup.class).list()
                        .forEach(ref -> ref.as(ConfiguratorGroup.class).setCollapse(false)))
                .frames(4)
                // The picker is a drag-and-drop / click-to-pick row like the renderer and texture ones,
                // not a dropdown. Asserting the type is what stops it quietly regressing to a selector.
                .check("the picker is a drag-and-drop resource row", ctx ->
                        ctx.query().type(MachineBlueprintConfigurator.class).count() == 2)
                .check("an empty picker reads as such", ctx ->
                        ctx.query().withText(text("config.machine_blueprint.blueprint.none")).count() > 0)
                .check("a dangling reference reads as missing", ctx ->
                        ctx.query().withTextContaining("no_such_blueprint").count() > 0)
                // Visible, not merely present: a collapsed group still holds its children, so
                // asserting existence would pass for rows nobody can see or edit.
                .check("the int parameter row is visible", ctx ->
                        ctx.query().withText(INTERVAL).visible().count() > 0)
                .check("the string parameter row is visible", ctx ->
                        ctx.query().withText(TARGET_STATE).visible().count() > 0)
                .check("the boolean parameter row is visible", ctx ->
                        ctx.query().withText(ENABLED).visible().count() > 0)
                .check("a LOCAL variable gets no parameter row", ctx ->
                        ctx.query().withText("scratch").count() == 0)
                .screenshotElement("blueprint-binding-settings", "#root")
                .closeScreen();
    }

    private static boolean resolves(String key) {
        return !key.equals(Component.translatable(key).getString());
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private ModularUI createUI(Player player) {
        var root = new ScrollerView();
        root.layout(layout -> {
            layout.width(280);
            layout.height(420);
        }).setId("root");

        var settings = ConfigMachineSettings.builder().build();
        settings.blueprints().add(MachineBlueprintBinding.ofInline(
                parameterisedBlueprint().graphModel.serializeNBT(Platform.getFrozenRegistry())));

        // A reference binding: one with nothing chosen yet, pointed at a dangling path afterwards so
        // both of the picker's non-resolved states are on screen at once.
        var reference = new MachineBlueprintBinding();
        reference.setBlueprintPath(DANGLING);
        settings.blueprints().add(reference);

        var group = new ConfiguratorGroup("config.definition.machine_settings");
        group.setCollapse(false);
        settings.buildConfigurator(group);

        return new ModularUI(UI.of(root.addScrollViewChild(group)), player);
    }

    /**
     * A blueprint exposing one parameter of each shape a row has to handle: a number, a string and a
     * boolean. Three rather than one because they go through three different configurator accessors,
     * and a bug in the generation usually only shows for some types.
     */
    private static MachineBlueprintGraph parameterisedBlueprint() {
        var graph = new MachineBlueprintGraph();
        var model = graph.graphModel;
        KGGameTestHelpers.dataVar(model, INTERVAL, int.class, 20, VariableKind.INPUT);
        KGGameTestHelpers.dataVar(model, TARGET_STATE, String.class, "working", VariableKind.INPUT);
        KGGameTestHelpers.dataVar(model, ENABLED, boolean.class, true, VariableKind.INPUT);
        // A local variable too: it must NOT get a row, since only INPUT variables are parameters.
        KGGameTestHelpers.dataVar(model, "scratch", int.class, 0, VariableKind.LOCAL);

        var tick = model.createNodeModel(new TickEventNode(), new Vector2f(0, 0));
        var setState = model.createNodeModel(new MachineActionNodes.SetState(), new Vector2f(220, 0));
        model.createWire(setState.getInputsById().get("in"), tick.getOutputsById().get("next"));
        return graph;
    }
}

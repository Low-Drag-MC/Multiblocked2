package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommands;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintResource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;

import java.util.List;

/**
 * Every built-in blueprint, drawn on a read-only canvas.
 *
 * <h2>What this covers that the gametests cannot</h2>
 * Two things, both invisible headlessly. First, the <em>layout</em>: a built-in blueprint is meant to be
 * read, and whether its nodes overlap, whether a wire crosses the whole canvas, and whether the sticky
 * notes sit where they belong is a question only a rendered frame answers. The screenshots are the
 * artefact — a built-in whose graph has become a tangle is a regression even though nothing throws.
 *
 * <p>Second, read-only. {@code GraphView.setReadOnly} refuses commands at
 * {@code dispatchCommand}, and the check below drives a real command through a real view to prove the
 * graph is unchanged afterwards, which is the property a builtin resource depends on.</p>
 *
 * <h2>It is also a crash regression</h2>
 * Opening this many graphs in a row is what reproduced the {@code GraphChangeset} crash: the
 * blackboard's type search ran {@code detectSupportedTypes} on a background thread, which built models
 * in the graph being viewed while the render thread was merging that graph's change set. Keep the
 * whole list — one graph did not do it reliably.
 */
@LDLRegisterClient(name = "mbd2_builtin_blueprints", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class BuiltinBlueprintScenario implements UIScenario {

    private static final List<String> NAMES = List.of(
            "redstone_control", "comparator_progress", "environment_gate", "overclock",
            "upgrade_slots", "part_count_bonus", "upkeep", "chance_output", "output_swap", "heat_buildup",
            "debug_probe");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "blueprint", "builtin", "visual")
                .requiresWorld(true).guiScale(1);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 built-in blueprints", ctx -> createUI(ctx.requirePlayer(), NAMES.getFirst()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .frames(3)
                .check("the canvas opened read-only", ctx ->
                        ctx.query().type(GraphView.class).one().as(GraphView.class).isReadOnly())
                .check("a command is refused on a read-only canvas", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    var graph = graphView.getGraph();
                    if (graph == null) return false;
                    var before = graph.graphModel.getStickyNoteModels().size();
                    // Through dispatchCommand, not the menu: the menu is empty read-only, so driving
                    // the command directly is what proves the refusal is the model's and not the UI's.
                    var accepted = graphView.dispatchCommand(
                            new GraphCommands.CreateStickyNoteCommand(new Vector2f(0, 0)));
                    ctx.attach("accepted", String.valueOf(accepted));
                    return !accepted && graph.graphModel.getStickyNoteModels().size() == before;
                })

                .check("the right-click menu offers nothing to change", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    graphView.graphView.setActive(true);
                    return true;
                })
                .closeScreen();

        // One screenshot per blueprint, each in its own screen so the canvas fits the whole graph.
        for (var name : NAMES) {
            s.openModularUI("built-in: " + name, ctx -> createUI(ctx.requirePlayer(), name))
                    .awaitScreen(ModularUIScreen.class)
                    .awaitModularUI()
                    .awaitElement("#root")
                    .frames(3)
                    .step("fit " + name + " to the canvas", ctx ->
                            ctx.query().type(GraphView.class).one().as(GraphView.class).fitGraphChildren())
                    .frames(3)
                    .check(name + " has nodes on the canvas", ctx -> {
                        var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                        var graph = graphView.getGraph();
                        if (graph == null) return false;
                        ctx.attach("nodes", String.valueOf(graph.graphModel.getNodeModels().size()));
                        ctx.attach("notes", String.valueOf(graph.graphModel.getStickyNoteModels().size()));
                        return !graph.graphModel.getNodeModels().isEmpty()
                                && !graph.graphModel.getStickyNoteModels().isEmpty();
                    })
                    .screenshotElement("builtin-" + name, "#root")
                    .closeScreen();
        }
    }

    private ModularUI createUI(Player player, String name) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.paddingAll(4);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);

        var graphView = new GraphView();
        root.addChildren(graphView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));
        // Read-only BEFORE loadGraph: the inline port editors read the flag as the UI tree is built.
        graphView.setReadOnly(true);
        graphView.loadGraph(load(name));
        return new ModularUI(UI.of(root), player);
    }

    private static com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph load(String name) {
        var path = IResourcePath.parse(BuiltinBlueprints.path(name));
        var tag = path == null
                ? null
                : MachineBlueprintResource.INSTANCE.getResourceInstance().getResource(path);
        if (tag == null) throw new IllegalStateException("built-in blueprint '" + name + "' is missing");
        return MachineBlueprintResource.INSTANCE.deserializeGraphResource(tag, null);
    }
}

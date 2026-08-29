package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.test.tests.blueprint.BlueprintFixtures;
import net.minecraft.world.entity.player.Player;

/**
 * A blueprint that names a Photon node, drawn on a real canvas — with Photon and without.
 *
 * <h2>Why a client test as well as a gametest</h2>
 * {@code BlueprintPhotonNodeTests} proves the graph <em>loads</em> without Photon: LDLib2 keeps the
 * node model as a placeholder, logs {@code Could not find node class} and carries on. What it cannot
 * reach is the editor, and a placeholder model is precisely the shape the canvas has never been given
 * — a node UI built for a node that is not there. Opening a blueprint is the first thing anyone does
 * to a project they cannot run, so it has to survive.
 *
 * <pre>
 *   gradlew runClient -PldTest=tag:photon-optional             # Photon present
 *   gradlew runClient -PldTest=tag:photon-optional -PnoPhoton  # Photon off the runtime classpath
 * </pre>
 *
 * @see com.lowdragmc.mbd2.test.tests.blueprint.BlueprintFixtures#photonBlueprintGraph the recorded graph
 */
@LDLRegisterClient(name = "mbd2_photon_optional_blueprint", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class PhotonOptionalBlueprintScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "blueprint", "photon-optional", "visual")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 blueprint with a Photon node", ctx -> createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .frames(3)
                .check("the graph is on the canvas", ctx -> {
                    var count = nodeModels(ctx).size();
                    ctx.attach("photon", String.valueOf(MBD2.isPhotonLoaded()));
                    ctx.attach("nodes", String.valueOf(count));
                    return count == BlueprintFixtures.PHOTON_BLUEPRINT_NODE_COUNT;
                })
                // The placeholder is the whole point: without Photon one node model has no node behind
                // it, and the canvas still has to lay it out and draw it rather than throw.
                .check("the Photon node is a placeholder exactly when Photon is missing", ctx -> {
                    int unresolved = 0;
                    for (var model : nodeModels(ctx)) {
                        if (model instanceof ICustomNodeModel custom && custom.getNode() == null) unresolved++;
                    }
                    ctx.attach("unresolved", String.valueOf(unresolved));
                    return unresolved == (MBD2.isPhotonLoaded() ? 0 : 1);
                })
                .screenshotElement("blueprint-with-photon-node", "#root")
                .closeScreen();
    }

    /** The node models the canvas is actually holding, read back off the view rather than the builder. */
    private static java.util.List<AbstractNodeModel> nodeModels(TestContext ctx) {
        var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
        if (!(graphView.getGraph() instanceof MachineBlueprintGraph graph)) return java.util.List.of();
        return graph.graphModel.getNodeModels();
    }

    private ModularUI createUI(Player player) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.widthPercent(90);
            layout.heightPercent(100);
            layout.paddingAll(4);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);

        var graphView = new GraphView();
        root.addChildren(graphView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));

        // The recorded bytes, not a freshly built graph: without Photon the node cannot be spawned at
        // all, so building one here would quietly turn this into a test of a graph with nothing in it.
        var graph = new MachineBlueprintGraph();
        var golden = BlueprintFixtures.photonBlueprintTag();
        if (golden != null) {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), golden);
        }
        graphView.loadGraph(graph);
        return new ModularUI(UI.of(root), player);
    }
}

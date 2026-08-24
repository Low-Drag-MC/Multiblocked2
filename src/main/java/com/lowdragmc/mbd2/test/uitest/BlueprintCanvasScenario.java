package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.CancelEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoNode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;

import java.util.Set;

/**
 * Renders a real machine blueprint on a real graph canvas.
 *
 * <p>What this covers that the gametests cannot: the nodes as the editor actually draws them. A node
 * whose ports resolve fine headlessly can still render as a raw lang key, lay out wrong, or fail to
 * connect a block to its context — none of which a serialize/spawn test can see.</p>
 *
 * <p>The lang checks are the point of running this on a client at all. The gametest reads
 * {@code en_us.json} off the classpath, which proves the file has the keys; only a client with its
 * language loaded proves they actually resolve.</p>
 */
@LDLRegisterClient(name = "mbd2_blueprint_canvas", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class BlueprintCanvasScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "blueprint", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 blueprint canvas", ctx -> createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .frames(3)
                .check("a node display name resolves", ctx -> {
                    var name = Component.translatable("mbd2_event_tick").getString();
                    ctx.attach("mbd2_event_tick", name);
                    return "Machine Tick".equals(name);
                })
                .check("a node tooltip resolves", ctx ->
                        resolves("kg.node.mbd2_event_tick.tooltip"))
                .check("a node description resolves", ctx ->
                        resolves("kg.node.mbd2_event_tick.desc.1"))
                .check("a port description resolves", ctx ->
                        resolves("kg.node.mbd2_machine_set_state.in.state"))
                // Groups nest under mbd2/ since KilaGraph 21.1.0.11 moved its own under mc/ and ui/,
                // and the library looks up one key per path segment rather than the whole path.
                .check("the group root resolves", ctx -> {
                    var root = Component.translatable("mbd2").getString();
                    ctx.attach("mbd2", root);
                    return "MBD2".equals(root);
                })
                .check("a group leaf resolves", ctx ->
                        "Action".equals(Component.translatable("action").getString()))
                .screenshotElement("blueprint-canvas", "#root")
                .step("open the item library", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    graphView.itemLibrary.setDescriptionWidth(200).show(120, 70, ignored -> {});
                })
                .frames(3)
                .check("MBD2 nodes reach the item library", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    return find(graphView.itemLibrary.nodeTree.getRoot(), "mbd2_event_tick") != null;
                })
                .check("KilaGraph nodes are offered alongside them", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    return find(graphView.itemLibrary.nodeTree.getRoot(), "exec_branch") != null
                            || find(graphView.itemLibrary.nodeTree.getRoot(), "math_add") != null;
                })
                .screenshotElement("blueprint-item-library", "#root")
                .step("select Machine Tick in the library", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    var tree = graphView.itemLibrary.nodeTree;
                    var node = find(tree.getRoot(), "mbd2_event_tick");
                    if (node == null) throw new IllegalStateException("mbd2_event_tick is not in the library");
                    tree.expandNodeAlongPath(node);
                    tree.setSelected(Set.of(node), true);
                })
                .frames(3)
                .screenshotElement("blueprint-node-description", "#root")
                .closeScreen();
    }

    /** Whether a lang key has a translation, as opposed to falling through to the key itself. */
    private static boolean resolves(String key) {
        return !key.equals(Component.translatable(key).getString());
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
        graphView.loadGraph(createBlueprint());
        return new ModularUI(UI.of(root), player);
    }

    /**
     * A blueprint that reads like one a user would write: on tick, if the machine is working, put it in
     * a state; otherwise cancel the tick.
     *
     * <p>Deliberately built from both node shapes — plain nodes and a context node with property blocks
     * inside — because the block-inside-context layout is the part with no headless equivalent.</p>
     */
    private static MachineBlueprintGraph createBlueprint() {
        var graph = new MachineBlueprintGraph();
        var model = graph.graphModel;

        var tick = spawn(graph, TickEventNode.class, 0, 60);
        var logicInfo = spawn(graph, RecipeLogicInfoNode.class, 0, 190);
        var isWorking = KGGameTestHelpers.addBlock(graph, logicInfo, RecipeLogicInfoBlocks.IsWorking.class);

        var branch = spawn(graph, BranchNode.class, 230, 70);
        var setState = spawn(graph, MachineActionNodes.SetState.class, 400, 20);
        var cancel = spawn(graph, CancelEventNode.class, 400, 150);

        var machineInfo = spawn(graph, MachineInfoNode.class, 230, 250);
        KGGameTestHelpers.addBlock(graph, machineInfo, MachineInfoBlocks.MachineStateName.class);
        KGGameTestHelpers.addBlock(graph, machineInfo, MachineInfoBlocks.MachineTier.class);

        KGGameTestHelpers.setInputConstant(setState, "state", "working");
        model.createWire(branch.getInputsById().get("cond"), isWorking.getOutputsById().get("value"));
        model.createWire(branch.getInputsById().get("in"), tick.getOutputsById().get("next"));
        model.createWire(setState.getInputsById().get("in"), branch.getOutputsById().get("trueExec"));
        model.createWire(cancel.getInputsById().get("in"), branch.getOutputsById().get("falseExec"));
        return graph;
    }

    /**
     * Spawn a node at a position, letting the graph model pick the model type.
     *
     * <p>Not {@code createNodeModel(new SomeNode(), pos)}: that always builds a plain node model, so a
     * context node comes back without the container its blocks attach to and inserting one throws.
     * Going through {@code createNodeFromData} is what the editor itself does.</p>
     */
    private static NodeModel spawn(MachineBlueprintGraph graph, Class<? extends Node> nodeClass,
                                   float x, float y) {
        var data = new GraphNodeCreationData(graph.graphModel, new Vector2f(x, y), SpawnFlags.DEFAULT, null);
        return (NodeModel) CustomGraphModelImpl.createNodeFromData(data, nodeClass);
    }

    private static TreeNode<ItemLibraryItem, Void> find(TreeNode<ItemLibraryItem, Void> node, String registryName) {
        if (node == null) return null;
        if (registryName.equals(node.getKey().getSearchableName())) return node;
        for (var child : node.getChildren()) {
            var found = find(child, registryName);
            if (found != null) return found;
        }
        return null;
    }
}

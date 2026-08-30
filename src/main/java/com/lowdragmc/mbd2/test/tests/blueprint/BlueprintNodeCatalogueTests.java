package com.lowdragmc.mbd2.test.tests.blueprint;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeContentNodes;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Whole-catalogue tests: every node MBD2 registers, checked at once.
 *
 * <h2>Why these exist</h2>
 * There are far more nodes than anyone will write a behaviour test for, and the ways a node breaks are
 * mostly not behavioural. A field whose type has no {@code TypeHandle}, a port that fails to define, a
 * value that does not survive NBT, a display name that renders as its own lang key — none of those need
 * a machine to reproduce, and all of them make a node unusable. Testing them across the whole registry
 * costs one test each and cannot fall behind the node set, because it enumerates the registry rather
 * than a list someone has to remember to extend.
 *
 * <p>The behaviour tests in {@link BlueprintTests} then cover the paths that really do need a machine.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class BlueprintNodeCatalogueTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintFixtures.PLAIN_MACHINE_ID; }

    /** Node classes MBD2 itself registers — KilaGraph's own are its project's to test. */
    private static List<Class<? extends Node>> mbdNodes() {
        return MachineBlueprintGraph.NODE_REGISTRY.getNodeClasses();
    }

    /**
     * Every node can be spawned into a graph and defines its ports.
     *
     * <p>Spawning is what runs the annotation scan, resolves every field's {@code TypeHandle} and builds
     * each port's embedded constant — so a node declaring a field of a type nothing can carry fails
     * here rather than the first time someone drags it onto a canvas.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyNodeSpawns(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        int spawned = 0;
        for (var nodeClass : mbdNodes()) {
            try {
                var graph = new MachineBlueprintGraph();
                spawnInto(graph, nodeClass);
                spawned++;
            } catch (Throwable t) {
                failures.add(nodeClass.getSimpleName() + ": " + t);
            }
        }
        if (!failures.isEmpty()) {
            helper.fail(failures.size() + " node(s) failed to spawn: " + String.join(" | ", failures));
            return;
        }
        if (spawned == 0) {
            helper.fail("no MBD2 blueprint nodes registered at all — the registry did not scan");
            return;
        }
        helper.succeed();
    }

    /**
     * A graph holding every node survives a serialize/deserialize round trip.
     *
     * <p>This is the property a machine definition depends on: a blueprint is stored as NBT on the
     * definition or in a resource file, so a node whose constant cannot be written silently loses its
     * value between the editor and the world. One graph with everything in it rather than one per node,
     * because the failure mode is per node type and the cost is not.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyNodeSurvivesNbtRoundTrip(GameTestHelper helper) {
        var graph = new MachineBlueprintGraph();
        int expected = 0;
        for (var nodeClass : mbdNodes()) {
            // Context nodes come back through their blocks; counting them twice would make the
            // assertion below wrong rather than stricter.
            if (BlockNode.class.isAssignableFrom(nodeClass)) continue;
            try {
                spawnInto(graph, nodeClass);
                expected++;
            } catch (Throwable t) {
                helper.fail("could not spawn " + nodeClass.getSimpleName() + " into the shared graph: " + t);
                return;
            }
        }

        var provider = Platform.getFrozenRegistry();
        var tag = graph.graphModel.serializeNBT(provider);
        var restored = new MachineBlueprintGraph();
        try {
            restored.graphModel.deserializeNBT(provider, tag);
        } catch (Throwable t) {
            helper.fail("blueprint graph failed to deserialize: " + t);
            return;
        }
        int actual = restored.graphModel.getNodeModels().size();
        if (actual != expected) {
            helper.fail("round trip lost nodes: wrote " + expected + ", read back " + actual);
            return;
        }
        helper.succeed();
    }

    /**
     * Every node has a display name, a tooltip, a description and a line for each of its ports.
     *
     * <p>Read straight out of the shipped {@code en_us.json} rather than through {@code Language},
     * because a dedicated server never loads a mod's lang file — asking the game would pass by
     * answering "nothing is translated" uniformly.</p>
     *
     * <p>The port list comes from spawning each node, so a pin added without a description is caught
     * the same day it is added.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyNodeIsDocumented(GameTestHelper helper) {
        JsonObject lang;
        try (var stream = BlueprintNodeCatalogueTests.class
                .getResourceAsStream("/assets/mbd2/lang/en_us.json")) {
            if (stream == null) {
                helper.fail("en_us.json is not on the classpath");
                return;
            }
            lang = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            helper.fail("could not read en_us.json: " + e);
            return;
        }

        var missing = new ArrayList<String>();
        var groups = new LinkedHashMap<String, Boolean>();
        for (var nodeClass : mbdNodes()) {
            var attribute = nodeClass.getAnnotation(NodeAttribute.class);
            if (attribute == null) continue;
            String name = attribute.name();
            // A group is a path — the item library splits it on "/" and looks each segment up as its
            // own key — so checking the whole string would pass for nothing and fail for everything.
            for (String segment : attribute.group().split("/")) {
                if (!segment.isEmpty()) groups.put(segment, lang.has(segment));
            }

            if (!lang.has(name)) missing.add(name + " (display name)");
            if (!lang.has("kg.node." + name + ".tooltip")) missing.add(name + " (tooltip)");
            if (!lang.has("kg.node." + name + ".desc.1")) missing.add(name + " (desc)");

            for (var entry : portsOf(nodeClass).entrySet()) {
                String key = "kg.node." + name + "." + entry.getValue() + "." + entry.getKey();
                if (!lang.has(key)) missing.add(key);
            }
        }
        for (var entry : groups.entrySet()) {
            if (!entry.getValue()) missing.add(entry.getKey() + " (group name)");
        }

        if (!missing.isEmpty()) {
            helper.fail(missing.size() + " missing lang key(s): " + String.join(", ", missing));
            return;
        }
        helper.succeed();
    }

    /** Port id -> {@code "in"} or {@code "out"}, by spawning the node and reading its model. */
    private static Map<String, String> portsOf(Class<? extends Node> nodeClass) {
        var ports = new LinkedHashMap<String, String>();
        var graph = new MachineBlueprintGraph();
        NodeModel model;
        try {
            model = spawnInto(graph, nodeClass);
        } catch (Throwable t) {
            return ports; // everyNodeSpawns reports this; do not fail twice for the same cause
        }
        if (model == null) return ports;
        model.getInputsById().keySet().forEach(id -> ports.put(id, "in"));
        model.getOutputsById().keySet().forEach(id -> ports.put(id, "out"));
        return ports;
    }

    /**
     * Spawn one node, putting a block inside a context of the type its {@code @UseWithContext} names.
     *
     * <p>A block has no independent existence — its ports are defined against its parent, and the
     * parent is how it reaches the target it reads — so spawning one on its own would test a shape that
     * never occurs.</p>
     */
    private static NodeModel spawnInto(MachineBlueprintGraph graph, Class<? extends Node> nodeClass) {
        if (!BlockNode.class.isAssignableFrom(nodeClass)) {
            return KGGameTestHelpers.addRegisteredNode(graph, nodeClass);
        }
        @SuppressWarnings("unchecked")
        var blockClass = (Class<? extends BlockNode>) nodeClass;
        var contextClass = contextFor(blockClass);
        if (contextClass == null) {
            throw new IllegalStateException("block node has no @UseWithContext, so it can never be placed");
        }
        var context = KGGameTestHelpers.addRegisteredNode(graph, contextClass);
        return KGGameTestHelpers.addBlock(graph, context, blockClass);
    }

    private static Class<? extends ContextNode> contextFor(Class<? extends BlockNode> blockClass) {
        var annotation = blockClass.getAnnotation(UseWithContext.class);
        if (annotation == null) return null;
        for (var candidate : annotation.value()) {
            if (ContextNode.class.isAssignableFrom(candidate) && !Modifier.isAbstract(candidate.getModifiers())) {
                @SuppressWarnings("unchecked")
                var context = (Class<? extends ContextNode>) candidate;
                return context;
            }
        }
        return null;
    }

    /**
     * Content Value's output port takes its type from the capability the node is set to.
     *
     * <p>Everything else about that node works the same whether the port is properly typed or left
     * untyped - the value flows either way, so no behaviour test can tell the difference. What a
     * wrong type costs is in the editor: an untyped port connects to anything and defers the mistake
     * to runtime, where a recipe silently stops producing. Asserting the port retypes when the
     * dropdown moves is the only place that is visible.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void contentValuePortFollowsItsCapability(GameTestHelper helper) {
        var graph = new MachineBlueprintGraph();
        var node = KGGameTestHelpers.addRegisteredNode(graph, RecipeContentNodes.ContentValue.class);

        var asItem = node.getOutputsById().get("value").getPortDataType();
        if (asItem != SizedIngredient.class) {
            helper.fail("expected the default (item) capability to type the port as SizedIngredient, was " + asItem);
            return;
        }

        KGGameTestHelpers.setOption(node, "capability", ForgeEnergyRecipeCapability.CAP.name);
        var asEnergy = node.getOutputsById().get("value").getPortDataType();
        if (asEnergy != Integer.class) {
            helper.fail("expected the energy capability to retype the port as Integer, was " + asEnergy);
            return;
        }

        // And back, because a retype that only works in one direction is a retype that leaks.
        KGGameTestHelpers.setOption(node, "capability", ItemRecipeCapability.CAP.name);
        if (node.getOutputsById().get("value").getPortDataType() != SizedIngredient.class) {
            helper.fail("the port did not go back to SizedIngredient");
            return;
        }
        helper.succeed();
    }
}

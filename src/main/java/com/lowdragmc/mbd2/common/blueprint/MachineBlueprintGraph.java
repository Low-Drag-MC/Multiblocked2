package com.lowdragmc.mbd2.common.blueprint;

import com.lowdragmc.kilagraph.blueprint.BlueprintGraph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphNodeRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.mbd2.MBD2;

import java.util.ArrayList;
import java.util.List;

/**
 * The graph type a machine blueprint is authored in.
 *
 * <h2>Why a subclass rather than reusing {@link BlueprintGraph}</h2>
 * {@code @NodeAttribute.graphTypes} is matched by <b>class identity</b>, not assignability — so a node
 * bound to {@code MachineBlueprintGraph} does not appear in KilaGraph's own generic blueprint editor,
 * which is what we want: every machine node needs a {@link MachineEnvironment} to resolve its machine
 * from, and there is none over there.
 *
 * <p>The reverse direction is not a restriction, because {@link #getSupportNodes()} is just a list:
 * we return KilaGraph's whole registry <em>plus</em> ours, so a machine blueprint offers all ~300
 * generic nodes (math, logic, string, list, map, exec, {@code mc.*}) alongside the MBD2 ones.</p>
 *
 * <p>Extending {@code BlueprintGraph} rather than {@code Graph} also inherits its
 * {@code KGGraphModel} and its type-set bootstrap, so KilaGraph's Minecraft handles stay available.</p>
 */
public class MachineBlueprintGraph extends BlueprintGraph {

    static {
        // Must run before NODE_REGISTRY below: creating a GraphNodeRegistry scans annotations and
        // instantiates every matching Node to harvest its declared port types, so a handle minted
        // after that point would never be seen. Static initialisers run in textual order, so the
        // ordering is enforced here rather than depending on some caller doing it first.
        MBDTypeHandles.init();
    }

    public static final GraphNodeRegistry NODE_REGISTRY =
            GraphNodeRegistry.create(MBD2.id("machine_blueprint"), MachineBlueprintGraph.class);

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        var nodes = new ArrayList<Class<? extends Node>>();
        nodes.addAll(BlueprintGraph.NODE_REGISTRY.getNodeClasses());
        nodes.addAll(NODE_REGISTRY.getNodeClasses());
        return List.copyOf(nodes);
    }

    @Override
    public List<TypeHandle> getSupportTypes() {
        var types = new ArrayList<>(super.getSupportTypes());
        types.addAll(MBDTypeHandles.ALL);
        return List.copyOf(types);
    }

    @Override
    public List<TypeHandle> getLibrarySupportTypes() {
        var types = new ArrayList<>(super.getLibrarySupportTypes());
        types.addAll(MBDTypeHandles.LIBRARY_TYPES);
        return List.copyOf(types);
    }

    /**
     * Accept any KilaGraph blueprint as a subgraph, not just another machine blueprint.
     *
     * <p>That makes a pure-logic graph authored in KilaGraph's own editor reusable here as a function
     * library. It is safe in the one direction that matters: such a graph contains only generic nodes,
     * so nothing inside it can reach for a machine that isn't there.</p>
     */
    @Override
    public boolean acceptsSubgraphGraph(Graph other) {
        return other instanceof BlueprintGraph;
    }
}

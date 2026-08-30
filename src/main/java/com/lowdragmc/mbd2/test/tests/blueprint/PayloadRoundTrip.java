package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeContentNodes;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One graph shape, reused per recipe capability, that answers "do this capability's payload nodes
 * actually work".
 *
 * <h2>The shape</h2>
 * <pre>
 * Tick ────────────────────────────────────────────────────────────► Set Analog Signal
 *   maker ─► Content Of ─► Content To Nbt ─► Content From Nbt ─► Content Value ─► reader ─┘
 * </pre>
 *
 * <p>Every capability supplies a pair — the node that builds its payload and the node that reads one
 * back — and this drives that pair through the whole generic pipeline in between. A machine is
 * involved only because a blueprint needs an event to hang off; there is no recipe, no trait and no
 * mod machinery, so a failure is about the nodes and nothing else.</p>
 *
 * <h2>Why it goes through NBT</h2>
 * Without that leg the test would be nearly free to pass: {@code Content Of} hands the payload
 * straight to {@code capability.of}, which returns it unchanged when it is already the right type,
 * and {@code Content Value} hands the same object back — the same reference, never inspected.
 * Routing through {@code Content To Nbt} and {@code Content From Nbt} forces the capability's codec
 * to encode and re-parse it, so a field the maker set but the codec drops comes out as a different
 * number rather than as the same object arriving intact.
 *
 * <h2>Why an analog signal</h2>
 * It is the one machine-observable value a blueprint can write with no trait at all, and it survives
 * into {@code MBDMachine.getAnalogOutputSignal()} where a test can read it. Each fixture picks a
 * number nothing else in its graph produces, so "the signal is 11" cannot be a coincidence — but the
 * converse is weaker than it looks: zero is what both a broken chain and a blueprint that never ran
 * produce, and only the other tests in {@link BlueprintBehaviourTests}, which drive a signal off the
 * same tick event, rule the second one out.
 */
public final class PayloadRoundTrip {

    private PayloadRoundTrip() {}

    /** The graph, and the two nodes a caller may still need to wire something extra onto. */
    public record Built(MachineBlueprintGraph graph, NodeModel maker, NodeModel reader) {}

    /** {@link #build} for the callers that only want the graph. */
    public static MachineBlueprintGraph graph(String capability,
                                              Class<? extends Node> makerClass, String makerOutput,
                                              Consumer<NodeModel> configureMaker,
                                              Class<? extends Node> readerClass, String readerInput,
                                              String readerOutput, @Nullable String readerGate) {
        return build(capability, makerClass, makerOutput, configureMaker,
                readerClass, readerInput, readerOutput, readerGate).graph();
    }

    /**
     * The whole shape from the class javadoc.
     *
     * @param capability     the registry name of the capability under test
     * @param makerClass     the node that builds this capability's payload
     * @param makerOutput    its output port
     * @param configureMaker sets the maker's constants — the values the round trip has to preserve
     * @param readerClass    the node that reads the payload back
     * @param readerInput    its input port
     * @param readerOutput   the port whose value becomes the analog signal
     * @param readerGate     an optional boolean port of the reader; when given, the signal is only
     *                       emitted if it came back true, which is how a flag with no number of its
     *                       own gets asserted
     */
    public static Built build(String capability,
                              Class<? extends Node> makerClass, String makerOutput,
                              Consumer<NodeModel> configureMaker,
                              Class<? extends Node> readerClass, String readerInput,
                              String readerOutput, @Nullable String readerGate) {
        var built = dataChain(capability, makerClass, makerOutput, configureMaker, readerClass, readerInput);
        var graph = built.graph();
        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("signal"),
                built.reader().getOutputsById().get(readerOutput));

        if (readerGate == null) {
            KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), tick.getOutputsById().get("next"));
        } else {
            var branch = KGGameTestHelpers.addRegisteredNode(graph, BranchNode.class);
            KGGameTestHelpers.wire(graph, branch.getInputsById().get("in"), tick.getOutputsById().get("next"));
            KGGameTestHelpers.wire(graph, branch.getInputsById().get("cond"),
                    built.reader().getOutputsById().get(readerGate));
            KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), branch.getOutputsById().get("trueExec"));
        }
        return new Built(graph, built.maker(), built.reader());
    }

    /**
     * Only the data half — maker through the pipeline to reader, with no tick and no signal.
     *
     * <p>For the capability whose reading cannot be a number: the caller owns the exec leg so it can
     * put a comparison in front of the signal.</p>
     */
    public static Built dataChain(String capability,
                                  Class<? extends Node> makerClass, String makerOutput,
                                  Consumer<NodeModel> configureMaker,
                                  Class<? extends Node> readerClass, String readerInput) {
        var graph = new MachineBlueprintGraph();
        var maker = KGGameTestHelpers.addRegisteredNode(graph, makerClass);
        configureMaker.accept(maker);

        // Options before wires: setting one redefines the node's ports, and Content Value's output
        // port is typed from the capability - wiring it first would connect the item-typed default
        // and then have that wire parked as a type conflict when the option moved.
        var of = capabilityNode(graph, RecipeContentNodes.ContentOf.class, capability);
        var toNbt = capabilityNode(graph, RecipeContentNodes.ContentToNbt.class, capability);
        var fromNbt = capabilityNode(graph, RecipeContentNodes.ContentFromNbt.class, capability);
        var value = capabilityNode(graph, RecipeContentNodes.ContentValue.class, capability);
        var reader = KGGameTestHelpers.addRegisteredNode(graph, readerClass);

        KGGameTestHelpers.wire(graph, of.getInputsById().get("value"), maker.getOutputsById().get(makerOutput));
        KGGameTestHelpers.wire(graph, toNbt.getInputsById().get("content"), of.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, fromNbt.getInputsById().get("nbt"), toNbt.getOutputsById().get("nbt"));
        KGGameTestHelpers.wire(graph, value.getInputsById().get("content"), fromNbt.getOutputsById().get("content"));
        KGGameTestHelpers.wire(graph, reader.getInputsById().get(readerInput), value.getOutputsById().get("value"));
        return new Built(graph, maker, reader);
    }

    private static NodeModel capabilityNode(MachineBlueprintGraph graph, Class<? extends Node> nodeClass,
                                            String capability) {
        var node = KGGameTestHelpers.addRegisteredNode(graph, nodeClass);
        KGGameTestHelpers.setOption(node, "capability", capability);
        return node;
    }
}

package com.lowdragmc.mbd2.test.tests.blueprint.create;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.EqualsNode;
import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.create.CreateRecipeContentNodes;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.tests.blueprint.PayloadRoundTrip;
import net.minecraft.resources.ResourceLocation;

/**
 * Two machines for Create's rotation payload, because it has three fields and one redstone signal
 * cannot carry all of them.
 *
 * <p>The first covers the amount and the torque override; the second covers the mode, which is the
 * field that decides what the amount even means and so the one whose loss would be silent.</p>
 */
public class CreatePayloadFixtures implements TestFixtureProvider {

    public static final ResourceLocation VALUE_MACHINE_ID = MBD2.id("blueprint_create_payload_round_trip");
    public static final ResourceLocation MODE_MACHINE_ID = MBD2.id("blueprint_create_payload_mode");

    /** The rotation amount asked for; readable as a redstone signal and not a default. */
    public static final int VALUE = 7;
    /** The torque asked for. Distinct from {@link #VALUE} so a graph reading the wrong one shows it. */
    public static final int TORQUE = 9;
    /** What the mode machine emits when the two modes came back telling each other apart. */
    public static final int MODE_MATCHED = 13;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(VALUE_MACHINE_ID).withBlueprint(valueRoundTrip()).register(event);
        TestMachineBuilder.simple(MODE_MACHINE_ID).withBlueprint(modeRoundTrip()).register(event);
    }

    /** The amount, gated on the torque override having survived — see {@link PayloadRoundTrip}. */
    private static MachineBlueprintGraph valueRoundTrip() {
        return PayloadRoundTrip.graph(CreateRotationRecipeCapability.CAP.name,
                CreateRecipeContentNodes.RotationOf.class, "rotation",
                maker -> {
                    KGGameTestHelpers.setInputConstant(maker, "value", (float) VALUE);
                    KGGameTestHelpers.setInputConstant(maker, "mode", CreateRotation.Mode.RPM);
                    KGGameTestHelpers.setInputConstant(maker, "overrideTorque", true);
                    KGGameTestHelpers.setInputConstant(maker, "torque", (float) TORQUE);
                },
                CreateRecipeContentNodes.RotationInfo.class, "rotation", "value", "overridesTorque");
    }

    /**
     * The mode, asserted by comparison rather than by number.
     *
     * <p>A mode is an enum: it cannot be emitted as a redstone signal, and there is no way to put one
     * on the {@code Object} port of an equality node as a constant. So the graph builds a rotation
     * twice — one {@code RPM} through the whole Content/NBT pipeline, one {@code STRESS} straight into
     * a reader — and emits only when the two modes come back <em>different</em>.</p>
     *
     * <p>Different rather than the same, which is the version that survives being attacked from both
     * sides. Requiring agreement would catch a codec that dropped the mode — the round-tripped one
     * would fall back to {@code STRESS} while the direct one stayed {@code RPM} — but a reader that
     * ignored the field and always answered {@code STRESS} would satisfy it just as happily, since
     * both sides would then be wrong in the same direction. Requiring disagreement catches both: a
     * reader stuck on one answer can never produce two.</p>
     */
    private static MachineBlueprintGraph modeRoundTrip() {
        var built = PayloadRoundTrip.dataChain(CreateRotationRecipeCapability.CAP.name,
                CreateRecipeContentNodes.RotationOf.class, "rotation",
                maker -> KGGameTestHelpers.setInputConstant(maker, "mode", CreateRotation.Mode.RPM),
                CreateRecipeContentNodes.RotationInfo.class, "rotation");
        var graph = built.graph();

        var direct = KGGameTestHelpers.addRegisteredNode(graph, CreateRecipeContentNodes.RotationOf.class);
        KGGameTestHelpers.setInputConstant(direct, "mode", CreateRotation.Mode.STRESS);
        var directInfo = KGGameTestHelpers.addRegisteredNode(graph, CreateRecipeContentNodes.RotationInfo.class);
        KGGameTestHelpers.wire(graph, directInfo.getInputsById().get("rotation"), direct.getOutputsById().get("rotation"));

        var equals = KGGameTestHelpers.addRegisteredNode(graph, EqualsNode.class);
        KGGameTestHelpers.wire(graph, equals.getInputsById().get("in1"), built.reader().getOutputsById().get("mode"));
        KGGameTestHelpers.wire(graph, equals.getInputsById().get("in2"), directInfo.getOutputsById().get("mode"));

        var tick = KGGameTestHelpers.addRegisteredNode(graph, TickEventNode.class);
        var branch = KGGameTestHelpers.addRegisteredNode(graph, BranchNode.class);
        var signal = KGGameTestHelpers.addRegisteredNode(graph, MachineRedstoneNodes.SetAnalogSignal.class);
        KGGameTestHelpers.setInputConstant(signal, "signal", MODE_MATCHED);
        KGGameTestHelpers.wire(graph, branch.getInputsById().get("in"), tick.getOutputsById().get("next"));
        KGGameTestHelpers.wire(graph, branch.getInputsById().get("cond"), equals.getOutputsById().get("out"));
        // falseExec: the modes must disagree, so the signal hangs off "not equal".
        KGGameTestHelpers.wire(graph, signal.getInputsById().get("in"), branch.getOutputsById().get("falseExec"));
        return graph;
    }
}

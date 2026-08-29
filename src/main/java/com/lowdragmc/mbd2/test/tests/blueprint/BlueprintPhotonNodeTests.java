package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.integration.photon.PhotonFXNodes;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A blueprint holding Photon nodes, loaded with and without Photon installed.
 *
 * <h2>Why this cannot be reasoned about</h2>
 * MBD2 declares Photon optional and keeps every Photon type behind a nested class reached only past
 * an {@code isPhotonLoaded()} check, so in principle nothing resolves when the jar is missing. But a
 * blueprint is the one place a project can refer to Photon by a name MBD2 never wrote itself: the
 * graph stores {@code nodeClass} strings, and {@code GraphNodeRegistry} filters {@code modID="photon"}
 * nodes out of the registry entirely when Photon is absent — so every one of those names resolves to
 * nothing on load. Whether that is a warning or a crash is a fact about LDLib2's deserializer, and the
 * only way to know it is to take the jar away and load the file.
 *
 * <p>Run both ways:
 * <pre>
 *   gradlew runGameTestServer            # Photon present
 *   gradlew runGameTestServer -PnoPhoton # Photon off the runtime classpath
 * </pre>
 * Every assertion below holds in both, which is the point — the tests read
 * {@link MBD2#isPhotonLoaded()} only where the two runs legitimately differ.</p>
 *
 * @see BlueprintFixtures#PHOTON_BLUEPRINT_MACHINE_ID for the machine that actually runs it
 */
@GameTestHolder(MBD2.MOD_ID)
public class BlueprintPhotonNodeTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintFixtures.PHOTON_BLUEPRINT_MACHINE_ID; }

    /**
     * The recorded graph deserializes, and the Photon node resolves exactly when Photon is there.
     *
     * <p>The node <em>model</em> survives either way — LDLib2 keeps the placeholder and logs
     * {@code Could not find node class} — which is what lets the wires around it still resolve. What
     * changes is whether the model has a {@link ICustomNodeModel#getNode() node} behind it.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprint_with_photon_nodes_loads(GameTestHelper h) {
        var golden = BlueprintFixtures.photonBlueprintTag();
        if (golden == null) {
            h.fail("photon_blueprint_golden.nbt is not on the classpath");
            return;
        }
        var graph = new MachineBlueprintGraph();
        try {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), golden);
        } catch (Throwable t) {
            h.fail("a blueprint holding Photon nodes failed to load: " + t);
            return;
        }

        var unresolved = new ArrayList<String>();
        int total = 0;
        for (var model : graph.graphModel.getNodeModels()) {
            if (!(model instanceof ICustomNodeModel custom)) continue;
            total++;
            if (custom.getNode() == null) unresolved.add(model.getClass().getSimpleName());
        }
        if (total != BlueprintFixtures.PHOTON_BLUEPRINT_NODE_COUNT) {
            h.fail("the graph lost nodes on load: expected "
                    + BlueprintFixtures.PHOTON_BLUEPRINT_NODE_COUNT + ", got " + total);
            return;
        }
        // One Photon node in the fixture; everything else must resolve whichever way this runs.
        int expectedUnresolved = MBD2.isPhotonLoaded() ? 0 : 1;
        if (unresolved.size() != expectedUnresolved) {
            h.fail("photon loaded=" + MBD2.isPhotonLoaded() + " so " + expectedUnresolved
                    + " node(s) should be unresolved, but " + unresolved.size() + " are: " + unresolved);
            return;
        }
        h.succeed();
    }

    /**
     * A machine whose blueprint contains a Photon node ticks without Photon.
     *
     * <p>The assertion is the item, not the absence of a stack trace: the executor logs and carries on
     * rather than propagating, so "nothing was thrown" would pass for a blueprint that silently stopped
     * dead. {@code Insert} sits <em>before</em> the Photon node in the exec chain precisely so that its
     * output means the same thing in both runs — whether flow continues <em>past</em> a node that could
     * not be resolved is LDLib2's business, not a promise MBD2 can keep.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void machine_with_a_photon_blueprint_still_runs(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(BlueprintFixtures.PHOTON_BLUEPRINT_MACHINE_ID, new BlockPos(1, 1, 1))
                .runTicks(10);
        int produced = scenario.getItem(0).getCount();
        if (produced <= 0) {
            h.fail("the blueprint did not run: nothing was inserted in 10 ticks (photon loaded="
                    + MBD2.isPhotonLoaded() + ")");
            return;
        }
        h.succeed();
    }

    /**
     * The recorded file still matches the graph the fixture describes.
     *
     * <p>A golden file is only evidence while it says what its author thinks it says. This rebuilds
     * the graph from source and compares the node classes, so renaming or dropping a Photon node turns
     * into a failure here rather than into a test that quietly stops covering anything.</p>
     *
     * <p>Only meaningful with Photon: without it the node cannot be spawned to compare against.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void photon_blueprint_golden_is_current(GameTestHelper h) {
        if (!MBD2.isPhotonLoaded()) {
            h.succeed();
            return;
        }
        var golden = BlueprintFixtures.photonBlueprintTag();
        if (golden == null) {
            h.fail("photon_blueprint_golden.nbt is not on the classpath");
            return;
        }
        var recorded = nodeClassesOf(golden);
        var current = nodeClassesOf(BlueprintFixtures.photonBlueprintGraph()
                .graphModel.serializeNBT(Platform.getFrozenRegistry()));
        if (!recorded.equals(current)) {
            h.fail("photon_blueprint_golden.nbt is stale - recorded " + recorded + ", fixture builds "
                    + current + ". Rebuild it, or this test covers a graph that no longer exists.");
            return;
        }
        if (!recorded.contains(PhotonFXNodes.PlayFX.class.getName())) {
            h.fail("the golden graph no longer contains a Photon node, so it proves nothing");
            return;
        }
        h.succeed();
    }

    /**
     * Every {@code nodeClass} a serialized graph names, sorted so order cannot matter.
     *
     * <p>Collected by walking the whole tag rather than reading a fixed path: a graph model writes its
     * node list inside {@code serializeAdditionalNBT}, so where it lands is the persistence layer's
     * business, and local subgraphs nest another one inside that. Searching for the key is both
     * simpler than tracking that shape and correct if it changes.</p>
     */
    private static List<String> nodeClassesOf(CompoundTag tag) {
        var names = new ArrayList<String>();
        collectNodeClasses(tag, names);
        names.sort(null);
        return names;
    }

    private static void collectNodeClasses(Tag tag, List<String> into) {
        if (tag instanceof CompoundTag compound) {
            var name = compound.getString("nodeClass");
            if (!name.isEmpty()) into.add(name);
            for (var key : compound.getAllKeys()) {
                collectNodeClasses(compound.get(key), into);
            }
        } else if (tag instanceof ListTag list) {
            for (var element : list) {
                collectNodeClasses(element, into);
            }
        }
    }
}

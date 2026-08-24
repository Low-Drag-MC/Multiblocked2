package com.lowdragmc.mbd2.common.blueprint;

import com.lowdragmc.kilagraph.graph.exec.GraphExecutor;
import com.lowdragmc.kilagraph.graph.exec.VariableStore;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One blueprint running on one machine: the executor over its graph, and the index that turns a posted
 * {@link MachineEvent} into the entry nodes to run.
 *
 * <h2>One executor per machine per blueprint</h2>
 * That granularity is what gives graph variables useful semantics: a {@code SetVar} in the blueprint is
 * <em>this machine's</em> state for <em>this</em> blueprint, and it accumulates across ticks. It is also
 * what {@link GraphExecutor} is built for — its structural {@code PreparedGraph}, slot tables and
 * context pools are all reused across runs, so after the first dispatch a run allocates nothing.
 *
 * <p>Sharing one executor per definition would instead make every machine of that type write into the
 * same variables, and building one per dispatch would throw the caches away every tick.</p>
 */
public class MachineBlueprintInstance {

    @Getter
    private final MBDMachine machine;
    @Getter
    private final MachineBlueprintBinding binding;

    @Nullable
    private GraphExecutor executor;
    @Nullable
    private MachineEnvironment environment;
    /**
     * Entry nodes by the exact event class they hook.
     *
     * <p>Keyed by exact class, not by assignability: {@code MachineRecipeModifyEvent.Before} and
     * {@code .After} are subclasses of the same parent, and a blueprint hooking "before" must not also
     * fire on "after". Dispatch therefore looks up {@code event.getClass()} and nothing else, which is
     * also the semantics the 1.20.1 processor had.</p>
     */
    private final Map<Class<?>, List<NodeModel>> entriesByEvent = new HashMap<>();
    /** Set once resolution has been attempted, so a missing blueprint is not retried every tick. */
    private boolean resolved;
    /**
     * Set once this instance has reported a failure.
     *
     * <p>Blueprints hang off per-tick events, so a graph that throws throws every tick. Logging each
     * one would bury the first — and the stack trace of the thousandth is the same as the first.</p>
     */
    private boolean loggedFailure;

    public MachineBlueprintInstance(MBDMachine machine, MachineBlueprintBinding binding) {
        this.machine = machine;
        this.binding = binding;
    }

    /**
     * Load the graph and index its entry nodes.
     *
     * <h2>Why this is lazy</h2>
     * A blueprint's NBT contains constants — item stacks, blocks, fluids — that only decode once the
     * game's registries are frozen. A machine definition is read at mod load, well before that, which
     * is why {@code MBDMachineDefinition.loadProductiveTag} defers its own settings into a post task.
     * Deserializing the graph eagerly alongside the binding would put it on the wrong side of that
     * line and quietly drop every registry-backed constant.
     *
     * <p>Doing it on the first dispatch instead puts it firmly in-world: the earliest event a machine
     * can receive is its own load, which is a tick after its block entity is valid. The binding itself
     * holds nothing but strings and raw tags, so it is safe to deserialize whenever the definition
     * is.</p>
     */
    private void resolve() {
        resolved = true;
        var loaded = binding.loadGraph();
        if (loaded == null) return;

        var values = binding.resolveVariableValues(loaded, Platform.getFrozenRegistry());
        this.environment = new MachineEnvironment(machine, new VariableStore(values));
        this.executor = new GraphExecutor(loaded, environment);

        for (var nodeModel : loaded.graphModel.getNodeModels()) {
            if (nodeModel instanceof ICustomNodeModel custom
                    && custom.getNode() instanceof MachineEventNode<?> eventNode
                    && nodeModel instanceof NodeModel model) {
                entriesByEvent.computeIfAbsent(eventNode.eventClass(), k -> new ArrayList<>()).add(model);
            }
        }
        if (entriesByEvent.isEmpty()) {
            MBD2.LOGGER.warn("Machine blueprint {} on {} has no event entry nodes — it will never run.",
                    binding.describe(), machine.getDefinition().id());
        }
        warnAboutDuplicateEntries();
    }

    /**
     * Point out events hooked more than once.
     *
     * <p>Several entry nodes for one event are allowed — two unrelated reactions to the same event is a
     * reasonable thing to draw, and forbidding it would push authors into one tangled flow. But they run
     * in the order the graph stores its nodes, which is creation order and is <em>not</em> visible on the
     * canvas. So if two of them touch the same machine state, the result depends on something the author
     * cannot see. Saying so once is cheaper than the bug report.</p>
     */
    private void warnAboutDuplicateEntries() {
        for (var entry : entriesByEvent.entrySet()) {
            if (entry.getValue().size() > 1) {
                MBD2.LOGGER.warn("Machine blueprint {} on {} has {} entry nodes for {}; they all run, in "
                                + "the order the nodes were created. Order is not shown on the canvas, so "
                                + "avoid having them write the same thing.",
                        binding.describe(), machine.getDefinition().id(),
                        entry.getValue().size(), entry.getKey().getSimpleName());
            }
        }
    }

    /** Whether this instance can react to {@code eventClass} at all — the fast path for dispatch. */
    public boolean handles(Class<?> eventClass) {
        if (!resolved) resolve();
        return entriesByEvent.containsKey(eventClass);
    }

    /**
     * Run every entry node hooking {@code event}.
     *
     * <p>The event is bound to the environment for the duration and cleared in a {@code finally}, so a
     * node that throws cannot leave a stale event visible to the next dispatch. A throw is logged and
     * swallowed: a broken blueprint must not take the machine's tick — or the server — down with it.</p>
     *
     * <p>Every entry hooking the event runs; see {@link #warnAboutDuplicateEntries}.</p>
     */
    public void post(MachineEvent event) {
        if (!resolved) resolve();
        var entries = entriesByEvent.get(event.getClass());
        if (entries == null || executor == null || environment == null) return;
        environment.withEvent(event);
        try {
            for (var entry : entries) {
                // Before every run, including the first: the executor memoises pulled values for the
                // lifetime of a generation, and the machine has moved on since the last dispatch. A
                // second entry for the same event is likewise its own run, not a continuation.
                executor.clearCache();
                executor.executeFrom(entry);
            }
        } catch (Throwable t) {
            if (!loggedFailure) {
                loggedFailure = true;
                MBD2.LOGGER.error("Machine blueprint {} on {} failed handling {}; further failures from "
                                + "this blueprint will not be logged",
                        binding.describe(), machine.getDefinition().id(),
                        event.getClass().getSimpleName(), t);
            }
        } finally {
            environment.withEvent(null);
        }
    }

    /** Drop the executor and its caches. Called when the machine unloads. */
    public void release() {
        executor = null;
        environment = null;
        entriesByEvent.clear();
        resolved = false;
        loggedFailure = false;
    }
}

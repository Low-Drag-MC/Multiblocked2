package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.ExecOutputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.ExecutionFlow;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;

/**
 * Base class for the entry nodes a machine blueprint hooks its logic onto — the blueprint equivalent
 * of KilaGraph's {@code EntryNode}, one subclass per {@link MachineEvent} type.
 *
 * <h2>How dispatch reaches one</h2>
 * A blueprint is not "the graph for event X". It is one graph that may contain any number of event
 * entry nodes, so a single reusable blueprint can react to several events and share variables and
 * local subgraphs (functions) between them. {@link com.lowdragmc.mbd2.common.blueprint.MachineBlueprintInstance}
 * indexes the graph's entry nodes by {@link #eventClass()} once, and on each dispatch runs
 * {@code executeFrom} for every entry matching the posted event.
 *
 * <p>The event itself arrives through {@link MachineEnvironment#getCurrentEvent()} rather than through
 * a port, because an entry node has no inputs — it <em>is</em> the start of the flow.</p>
 *
 * <h2>Writing a subclass</h2>
 * Declare one {@code @OutputPort} field per event field and fill them in {@link #publish}:
 * <pre>{@code
 * @NodeAttribute(name = "mbd2_event_use_item_on", group = "mbd2/event",
 *                graphTypes = MachineBlueprintGraph.class)
 * public class UseItemOnEventNode extends MachineEventNode<MachineUseItemOnEvent> {
 *     @OutputPort public Player player;
 *     @OutputPort public InteractionHand hand;
 *
 *     @Override public Class<MachineUseItemOnEvent> eventClass() { return MachineUseItemOnEvent.class; }
 *     @Override protected void publish(ExecContext ctx, MachineUseItemOnEvent event) {
 *         ctx.setOutput("player", event.player);
 *         ctx.setOutput("hand", event.hand);
 *     }
 * }
 * }</pre>
 * The {@code machine} and {@code next} pins come from here, and the field scan emits exec pins before
 * data pins, so {@code next} sits at the top of the node even though it is declared in this superclass.
 *
 * @param <E> the event type this node hooks
 */
public abstract class MachineEventNode<E extends MachineEvent> extends AnnotatedNode {

    @ExecOutputPort
    public ExecutionFlow next;
    @OutputPort
    public MBDMachine machine;

    /** The event type this entry node hooks. Used both to index the node and to guard the cast. */
    public abstract Class<E> eventClass();

    /**
     * Publish the event's fields onto this node's data outputs. Called only when the dispatched event
     * really is an {@link #eventClass()}; default no-op for events that carry nothing but the machine.
     */
    protected void publish(ExecContext ctx, E event) {
    }

    /**
     * Final: an entry node's job is fixed — publish the machine, publish the event's fields, fire
     * {@code next}. A subclass that needs to do work should put a node downstream of {@code next}
     * rather than overriding this, so the work is visible in the graph.
     */
    @Override
    public final void execute(ExecContext ctx) {
        if (!(ctx.getExecutor().getEnvironment() instanceof MachineEnvironment env)) {
            // Not running under a machine — e.g. someone pulled this graph into KilaGraph's own
            // editor and hit run. Dead-end rather than NPE downstream.
            return;
        }
        ctx.setOutput("machine", env.getMachine());
        var event = env.getCurrentEvent();
        if (eventClass().isInstance(event)) {
            publish(ctx, eventClass().cast(event));
        }
        ctx.flow("next");
    }
}

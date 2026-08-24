package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.ExecInputPort;
import com.lowdragmc.kilagraph.graph.core.ExecOutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.ExecutionFlow;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;

/**
 * Base class for the exec-flow nodes that <em>do</em> something to the machine or the event in flight.
 *
 * <p>Handles the two things every one of them shares: the {@code in}/{@code next} exec pins, and
 * resolving the {@link MachineEnvironment}. {@code next} fires whether or not the action applied, so a
 * node that does not apply — the wrong event type, a null machine — is a no-op rather than a
 * dead-ended flow the author has to debug.</p>
 */
public abstract class MachineActionNode extends AnnotatedNode {

    @ExecInputPort
    public ExecutionFlow in;
    @ExecOutputPort
    public ExecutionFlow next;

    /** Do the work. Only called when the graph really is running for a machine. */
    protected abstract void run(ExecContext ctx, MachineEnvironment env);

    @Override
    public final void execute(ExecContext ctx) {
        if (ctx.getExecutor().getEnvironment() instanceof MachineEnvironment env) {
            run(ctx, env);
        }
        ctx.flow("next");
    }
}

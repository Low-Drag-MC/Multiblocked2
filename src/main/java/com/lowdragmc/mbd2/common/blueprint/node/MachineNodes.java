package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.kilagraph.graph.exec.GraphExecutor;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import org.jetbrains.annotations.Nullable;

/**
 * Resolving "which machine does this node act on".
 *
 * <p>Every machine node takes an optional {@code machine} input and falls back to the machine the
 * blueprint is running for. The fallback is the whole point: a blueprint almost always acts on its own
 * machine, so requiring that wire on every node would put a fan of identical edges across the canvas
 * for no information. Wiring the port explicitly is for the minority case — reaching a neighbouring
 * machine, or a multiblock part.</p>
 */
public final class MachineNodes {

    /** The id every machine node uses for its optional target input. */
    public static final String MACHINE_INPUT = "machine";

    private MachineNodes() {}

    /** The machine {@code inputId} names, or the blueprint's own machine when nothing is wired. */
    @Nullable
    public static MBDMachine resolve(EvalContext ctx, String inputId) {
        var explicit = ctx.getInput(inputId, MBDMachine.class, null);
        return explicit != null ? explicit : ownMachine(ctx.getExecutor());
    }

    /** @see #resolve(EvalContext, String) */
    @Nullable
    public static MBDMachine resolve(ExecContext ctx, String inputId) {
        var explicit = ctx.getInput(inputId, MBDMachine.class, null);
        return explicit != null ? explicit : ownMachine(ctx.getExecutor());
    }

    /**
     * The machine this blueprint belongs to.
     *
     * <p>Read off the executor's environment rather than a port, so it is reachable from a subgraph —
     * {@link MachineEnvironment#createChild} carries it down for exactly this.</p>
     */
    @Nullable
    public static MBDMachine ownMachine(GraphExecutor executor) {
        return executor.getEnvironment() instanceof MachineEnvironment env ? env.getMachine() : null;
    }
}

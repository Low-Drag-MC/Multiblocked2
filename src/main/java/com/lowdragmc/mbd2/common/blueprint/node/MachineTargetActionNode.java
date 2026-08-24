package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

/**
 * A {@link MachineActionNode} that acts on a machine: adds the optional {@code machine} input and
 * skips the action when there is none to act on.
 *
 * <p>Leave {@code machine} unwired to act on the blueprint's own machine — see {@link MachineNodes}.</p>
 */
public abstract class MachineTargetActionNode extends MachineActionNode {

    /**
     * Which side an action is allowed to run on.
     *
     * <p>Machine events fire on both sides — {@code Client Tick} and the UI ones on the client, the
     * rest on the server — so a blueprint's flow reaches a node on whichever side dispatched. Every
     * action therefore has to say which side it means, and the base class enforces it rather than each
     * node remembering an {@code isRemote} guard.</p>
     *
     * <p>{@link #CLIENT} is not a nicety: {@code MBDMachine.playStateSound} is
     * {@code @OnlyIn(Dist.CLIENT)} and its body touches a client-only sound class, so reaching it on a
     * server is a {@code NoClassDefFoundError}. Declaring the side keeps that invoke unreachable
     * structurally instead of relying on a guard inside the node body.</p>
     */
    public enum Side {
        /** World state. The default, because almost every machine mutation is one. */
        SERVER,
        /** Presentation with no server counterpart — sound, particles. */
        CLIENT,
        /** Safe either way, usually because the machine relays it to the other side itself. */
        BOTH;

        boolean allows(boolean remote) {
            return this == BOTH || (remote ? this == CLIENT : this == SERVER);
        }
    }

    @InputPort
    public MBDMachine machine;

    /** Do the work. Only called with a resolved machine, on a side {@link #side()} permits. */
    protected abstract void apply(ExecContext ctx, MBDMachine machine);

    /** Which side this action may run on. Override for anything that is not plain server state. */
    protected Side side() {
        return Side.SERVER;
    }

    @Override
    protected final void run(ExecContext ctx, MachineEnvironment env) {
        MBDMachine target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
        if (target == null || !side().allows(target.isRemote())) return;
        apply(ctx, target);
    }
}

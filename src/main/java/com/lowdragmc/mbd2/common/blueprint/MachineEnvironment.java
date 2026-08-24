package com.lowdragmc.mbd2.common.blueprint;

import com.lowdragmc.kilagraph.graph.exec.EvaluationEnvironment;
import com.lowdragmc.kilagraph.graph.exec.VariableStore;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The {@link EvaluationEnvironment} a machine blueprint runs in: the machine it belongs to, plus the
 * event currently being dispatched.
 *
 * <p>{@code createChild} is overridden because a subgraph gets a fresh variable store but everything
 * else about the environment belongs to the run, not to the graph level. Without this, a machine node
 * placed inside a subgraph would find the machine silently absent and quietly emit null — which is
 * exactly the failure {@link EvaluationEnvironment#createChild} is documented to guard against.</p>
 */
public class MachineEnvironment extends EvaluationEnvironment {

    @Getter
    private final MBDMachine machine;
    /**
     * The event being dispatched, or {@code null} outside a dispatch.
     *
     * <p>Set for the duration of one {@code executeFrom} by {@link MachineBlueprintInstance#post},
     * and read by {@code MachineEventNode} to publish the event's fields onto its output pins. Not
     * final, and deliberately not a graph variable: an event is per-dispatch state on a long-lived
     * executor, and putting it in the variable store would let a {@code SetVar} clobber it.</p>
     */
    @Getter
    @Nullable
    private MachineEvent currentEvent;

    public MachineEnvironment(MBDMachine machine, VariableStore variables) {
        super(variables, java.util.OptionalLong.empty());
        this.machine = Objects.requireNonNull(machine);
    }

    private MachineEnvironment(MachineEnvironment parent, VariableStore childVariables) {
        super(childVariables, parent.seed());
        this.machine = parent.machine;
        this.currentEvent = parent.currentEvent;
    }

    @Override
    public EvaluationEnvironment createChild(VariableStore childVariables) {
        return new MachineEnvironment(this, childVariables);
    }

    /**
     * Bind the event for one dispatch. Returns {@code this} so the caller can chain; always paired
     * with a {@code finally} that clears it, so a throwing node cannot leave a stale event visible to
     * the next dispatch.
     */
    public MachineEnvironment withEvent(@Nullable MachineEvent event) {
        this.currentEvent = event;
        return this;
    }
}

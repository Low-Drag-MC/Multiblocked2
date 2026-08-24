package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.blueprint.node.MachineTargetActionNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Reading and emitting redstone from a machine.
 *
 * <p>Three separate signals, matching what the block exposes: the weak signal a comparator or dust
 * beside the machine sees, the strong ("direct") signal that powers a block through another, and the
 * analog signal a comparator reads. Setting any of them re-notifies the neighbours, so a blueprint does
 * not have to follow up with {@code Notify Block Update}.</p>
 */
public final class MachineRedstoneNodes {

    private static final String GROUP = "mbd2/machine/redstone";

    private MachineRedstoneNodes() {}

    /** The weak signal the machine emits on a side. */
    @NodeAttribute(name = "mbd2_redstone_get_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetSignal extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public Direction side = Direction.NORTH;
        @OutputPort public int signal;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var side = ctx.getInput("side", Direction.class, Direction.NORTH);
            ctx.setOutput("signal", target == null ? 0 : target.getOutputSignal(side));
        }
    }

    /** The strong signal the machine emits on a side. */
    @NodeAttribute(name = "mbd2_redstone_get_direct_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetDirectSignal extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public Direction side = Direction.NORTH;
        @OutputPort public int signal;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var side = ctx.getInput("side", Direction.class, Direction.NORTH);
            ctx.setOutput("signal", target == null ? 0 : target.getOutputDirectSignal(side));
        }
    }

    /** The analog signal a comparator reads off the machine. */
    @NodeAttribute(name = "mbd2_redstone_get_analog_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetAnalogSignal extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public int signal;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            ctx.setOutput("signal", target == null ? 0 : target.getAnalogOutputSignal());
        }
    }

    /** Whether redstone connects to the machine on a side. Follows the definition's signal-connection config. */
    @NodeAttribute(name = "mbd2_redstone_can_connect", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class CanConnect extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public Direction side = Direction.NORTH;
        @OutputPort public boolean value;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var side = ctx.getInput("side", Direction.class, Direction.NORTH);
            ctx.setOutput("value", target != null && target.canConnectRedstone(side));
        }
    }

    /** Emit a weak signal on a side. Clamped to 0-15. */
    @NodeAttribute(name = "mbd2_redstone_set_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetSignal extends MachineTargetActionNode {
        @InputPort public int signal;
        @InputPort public Direction side = Direction.NORTH;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.setOutputSignal(clampSignal(ctx), ctx.getInput("side", Direction.class, Direction.NORTH));
        }
    }

    /** Emit a strong signal on a side. Clamped to 0-15. */
    @NodeAttribute(name = "mbd2_redstone_set_direct_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetDirectSignal extends MachineTargetActionNode {
        @InputPort public int signal;
        @InputPort public Direction side = Direction.NORTH;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.setOutputDirectSignal(clampSignal(ctx), ctx.getInput("side", Direction.class, Direction.NORTH));
        }
    }

    /** Set the analog signal comparators read. Clamped to 0-15. */
    @NodeAttribute(name = "mbd2_redstone_set_analog_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAnalogSignal extends MachineTargetActionNode {
        @InputPort public int signal;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.setAnalogOutputSignal(clampSignal(ctx));
        }
    }

    /** Re-push the machine's signals to its neighbours without changing them. */
    @NodeAttribute(name = "mbd2_redstone_update_signal", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class UpdateSignal extends MachineTargetActionNode {
        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.updateSignal();
        }
    }

    /**
     * Clamped rather than passed through, because the machine stores signals as bytes: an out-of-range
     * value would wrap into a plausible-looking wrong strength rather than fail.
     */
    private static int clampSignal(ExecContext ctx) {
        return Mth.clamp(ctx.getInput("signal", Integer.class, 0), 0, 15);
    }
}

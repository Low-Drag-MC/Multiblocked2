package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.blueprint.node.MachineTargetActionNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.IRuntimeValueHolder;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Overriding, per machine, the values a definition authors — auto IO, capability IO, and any other
 * registered runtime value.
 *
 * <p>A definition is shared by every placed block of its type, so editing one changes them all and
 * persists nowhere. These nodes write the machine's own {@code RuntimeValueStorage} instead: the change
 * is saved with the block entity and reverts to the definition when cleared. Overrides are server-side
 * state and are never sent to clients.</p>
 *
 * <p>Every node takes an optional {@code trait} name. Leave it empty to target the machine itself; give
 * the name shown in the editor's trait list to target one of its traits.</p>
 */
public final class MachineRuntimeValueNodes {

    private static final String ACTION_GROUP = "mbd2/machine/action";
    private static final String INFO_GROUP = "mbd2/machine";

    private MachineRuntimeValueNodes() {}

    /**
     * The runtime value holder a {@code trait} name selects: the machine when empty, otherwise the named
     * trait — or null when there is no such trait, or it predates the runtime value system.
     */
    @Nullable
    private static IRuntimeValueHolder holder(MBDMachine machine, String traitName) {
        if (traitName.isEmpty()) return machine;
        var trait = machine.getTraitByName(traitName);
        return trait instanceof IRuntimeValueHolder valueHolder ? valueHolder : null;
    }

    /** Base for the actions, factoring out the {@code trait} port and the holder lookup. */
    private abstract static class RuntimeValueAction extends MachineTargetActionNode {
        @InputPort public String trait = "";

        @Override
        protected final void apply(ExecContext ctx, MBDMachine machine) {
            var holder = holder(machine, ctx.getInput("trait", String.class, ""));
            if (holder != null) {
                apply(ctx, machine, holder);
            }
        }

        protected abstract void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder);
    }

    /**
     * Write a runtime value by name — {@code auto_io.interval}, {@code capability_io.top},
     * {@code machine_level}, and so on. An unknown name is ignored with a log line.
     *
     * <p>Prefer the dedicated nodes below where one exists; this is the escape hatch for the values that
     * do not have one.</p>
     */
    private abstract static class SetRuntimeValue extends RuntimeValueAction {
        @InputPort public String key = "";

        @Override
        protected final void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            var key = ctx.getInput("key", String.class, "");
            if (key.isEmpty()) return;
            var value = value(ctx);
            if (value == null) return;
            try {
                holder.getRuntimeValues().set(key, value);
            } catch (IllegalArgumentException e) {
                MBD2.LOGGER.warn("Blueprint tried to set an unusable runtime value: {}", e.getMessage());
            }
        }

        @Nullable
        protected abstract Object value(ExecContext ctx);
    }

    /** @see SetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_set_runtime_bool", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeBool extends SetRuntimeValue {
        @InputPort public boolean value;

        @Override
        protected Object value(ExecContext ctx) {
            return ctx.getInput("value", Boolean.class, false);
        }
    }

    /** @see SetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_set_runtime_int", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeInt extends SetRuntimeValue {
        @InputPort public int value;

        @Override
        protected Object value(ExecContext ctx) {
            return ctx.getInput("value", Integer.class, 0);
        }
    }

    /** @see SetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_set_runtime_io", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeIO extends SetRuntimeValue {
        @InputPort public IO value = IO.NONE;

        @Override
        protected Object value(ExecContext ctx) {
            return ctx.getInput("value", IO.class, IO.NONE);
        }
    }

    /** Drop one override, putting that value back on the definition. */
    @NodeAttribute(name = "mbd2_machine_clear_runtime_value", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearRuntimeValue extends RuntimeValueAction {
        @InputPort public String key = "";

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            var key = ctx.getInput("key", String.class, "");
            if (key.isEmpty()) return;
            try {
                holder.getRuntimeValues().clear(key);
            } catch (IllegalArgumentException e) {
                MBD2.LOGGER.warn("Blueprint tried to clear an unknown runtime value: {}", e.getMessage());
            }
        }
    }

    /** Drop every override on the target, putting it fully back on its definition. */
    @NodeAttribute(name = "mbd2_machine_clear_all_runtime_values", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearAllRuntimeValues extends RuntimeValueAction {
        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            holder.getRuntimeValues().clearAll();
        }
    }

    /** Turn a trait's auto IO on or off for this machine only. */
    @NodeAttribute(name = "mbd2_machine_set_auto_io_enabled", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoIOEnabled extends RuntimeValueAction {
        @InputPort public boolean enabled;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            if (holder instanceof IAutoIOTrait autoIOTrait) {
                autoIOTrait.setAutoIOEnabled(ctx.getInput("enabled", Boolean.class, false));
            }
        }
    }

    /** Set which way a trait's auto IO moves things on one side, for this machine only. */
    @NodeAttribute(name = "mbd2_machine_set_auto_io_side", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoIOSide extends RuntimeValueAction {
        @InputPort public Direction side = Direction.NORTH;
        @InputPort public IO io = IO.NONE;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            var side = ctx.getInput("side", Direction.class, null);
            if (side == null || !(holder instanceof IAutoIOTrait autoIOTrait)) return;
            autoIOTrait.setAutoIOSide(side, ctx.getInput("io", IO.class, IO.NONE));
        }
    }

    /** Set how often a trait's auto IO runs, in ticks, for this machine only. */
    @NodeAttribute(name = "mbd2_machine_set_auto_io_interval", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoIOInterval extends RuntimeValueAction {
        @InputPort public int interval = 20;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            if (holder instanceof IAutoIOTrait autoIOTrait) {
                autoIOTrait.setAutoIOInterval(Math.max(1, ctx.getInput("interval", Integer.class, 20)));
            }
        }
    }

    /**
     * Set which way a trait's capability faces on one side, for this machine only — how neighbouring
     * pipes and hoppers may use it.
     */
    @NodeAttribute(name = "mbd2_machine_set_capability_io_side", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetCapabilityIOSide extends RuntimeValueAction {
        @InputPort public Direction side = Direction.NORTH;
        @InputPort public IO io = IO.BOTH;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            var side = ctx.getInput("side", Direction.class, null);
            if (side == null || !(holder instanceof SimpleCapabilityTrait<?, ?> capabilityTrait)) return;
            capabilityTrait.setCapabilityIOSide(side, ctx.getInput("io", IO.class, IO.BOTH));
        }
    }

    /** Put the machine back on the tier its definition configures. @see MachineActionNodes.SetTier */
    @NodeAttribute(name = "mbd2_machine_clear_tier", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ClearTier extends MachineTargetActionNode {
        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.clearMachineLevel();
        }
    }

    /**
     * Whether a value is currently overridden on this machine, as opposed to being read from its
     * definition. For a blueprint that wants to override something only once, or show the difference.
     */
    @NodeAttribute(name = "mbd2_machine_is_runtime_value_overridden", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class IsRuntimeValueOverridden extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @InputPort public String key = "";
        @OutputPort public boolean overridden;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var key = ctx.getInput("key", String.class, "");
            if (target == null || key.isEmpty()) {
                ctx.setOutput("overridden", false);
                return;
            }
            var holder = holder(target, ctx.getInput("trait", String.class, ""));
            ctx.setOutput("overridden", holder != null && holder.getRuntimeValues().isOverridden(key));
        }
    }
}

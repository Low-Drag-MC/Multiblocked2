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
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoWorldIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.IAutoWorldIOTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

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
    /** What the box readers publish when there is no box to read, so their outputs are never stale. */
    private static final AABB EMPTY_BOX = new AABB(0, 0, 0, 0, 0, 0);

    private MachineRuntimeValueNodes() {}

    /**
     * A runtime value as text, for {@link GetRuntimeString}.
     *
     * <p>A list joins on commas rather than using {@code List.toString} so the result is exactly what
     * {@link SetRuntimeString} takes back — {@code "[a, b]"} would not round-trip.</p>
     */
    private static String asText(@Nullable Object value) {
        if (value == null) return "";
        if (value instanceof Enum<?> constant) return constant.name();
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(value);
    }

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

    /**
     * @see SetRuntimeValue
     *
     * <p>{@code Set Runtime Value (Number)} also writes a decimal value — it just rounds on the way in,
     * because an integral slot refuses a fraction. This one is for the slots that keep it.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_runtime_float", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeFloat extends SetRuntimeValue {
        @InputPort public float value;

        @Override
        protected Object value(ExecContext ctx) {
            return ctx.getInput("value", Float.class, 0f);
        }
    }

    /**
     * @see SetRuntimeValue
     *
     * <p>Reaches more than the text slots: a value is coerced to whatever the slot holds, so this also
     * sets an enum by constant name and a list of names from a comma-separated string —
     * {@code "input,catalyst"} into {@code slot_names}.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_runtime_string", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeString extends SetRuntimeValue {
        @InputPort public String value = "";

        @Override
        protected Object value(ExecContext ctx) {
            return ctx.getInput("value", String.class, "");
        }
    }

    /**
     * Write a box-shaped runtime value — {@code area} on an entity handler, {@code auto_world_input.range}
     * and {@code auto_world_output.range} on an item or fluid trait.
     *
     * <p>Six numbers rather than one box input because a graph has no box type to wire, and because the
     * corners are usually computed — a scan area that grows with the machine's tier is
     * {@code -tier, -tier, -tier} to {@code 1 + tier, ...}.</p>
     *
     * <p>The box is machine-relative and unrotated, exactly as the editor authors it; the trait rotates
     * it to the machine's facing when it uses it.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_runtime_box", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetRuntimeBox extends SetRuntimeValue {
        @InputPort public double minX;
        @InputPort public double minY;
        @InputPort public double minZ;
        @InputPort public double maxX = 1;
        @InputPort public double maxY = 1;
        @InputPort public double maxZ = 1;

        @Override
        protected Object value(ExecContext ctx) {
            // AABB's constructor sorts the corners itself, so a graph that wires them the wrong way round
            // gets the box it meant rather than one that contains nothing
            return new AABB(
                    ctx.getInput("minX", Double.class, 0d),
                    ctx.getInput("minY", Double.class, 0d),
                    ctx.getInput("minZ", Double.class, 0d),
                    ctx.getInput("maxX", Double.class, 1d),
                    ctx.getInput("maxY", Double.class, 1d),
                    ctx.getInput("maxZ", Double.class, 1d));
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
     * Whether a trait does auto IO at all, and how it is currently set up.
     *
     * <p>{@code supported} is the question a blueprint has to ask before offering any of this: most
     * traits have no auto IO, and the setter nodes below are silent no-ops on one that does not — fine
     * for a script, useless for a UI that would otherwise draw a panel of controls doing nothing.</p>
     *
     * <p>The values are the machine's own, definition or override alike, which is what a UI wants to
     * show. {@code Is Runtime Value Overridden} answers the other question.</p>
     */
    @NodeAttribute(name = "mbd2_machine_auto_io_info", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AutoIOInfo extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @OutputPort public boolean supported;
        @OutputPort public boolean enabled;
        @OutputPort public int interval;

        @Override
        public void evaluate(EvalContext ctx) {
            var autoIO = autoIO(ctx);
            ctx.setOutput("supported", autoIO != null);
            ctx.setOutput("enabled", autoIO != null && autoIO.enable.get());
            ctx.setOutput("interval", autoIO == null ? 0 : autoIO.intervalTicks());
        }
    }

    /**
     * Which way a trait's auto IO currently moves things on one side.
     *
     * <p>The read half of {@code Set Auto IO Side}, and {@code side} means the same thing there: a
     * world direction, resolved against the machine's facing. {@code Relative Side} converts one of
     * the machine's own faces into it.</p>
     */
    @NodeAttribute(name = "mbd2_machine_get_auto_io_side", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetAutoIOSide extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @InputPort public Direction side = Direction.NORTH;
        @OutputPort public IO io;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var autoIO = autoIO(ctx);
            var side = ctx.getInput("side", Direction.class, null);
            if (target == null || autoIO == null || side == null) {
                ctx.setOutput("io", IO.NONE);
                return;
            }
            ctx.setOutput("io", autoIO.getIO(target.getFrontFacing().orElse(Direction.NORTH), side));
        }
    }

    /** The runtime auto IO of the trait a node names, or null when there is none to speak of. */
    @Nullable
    private static RuntimeAutoIO autoIO(EvalContext ctx) {
        var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
        if (target == null) return null;
        var holder = holder(target, ctx.getInput("trait", String.class, ""));
        return holder instanceof IAutoIOTrait autoIOTrait ? autoIOTrait.getRuntimeAutoIO() : null;
    }

    // ***** auto world IO ***** //

    /**
     * Base for the auto <b>world</b> IO setters — the scan box a trait picks dropped items or fluid
     * blocks up from, as opposed to {@code Set Auto IO ...}, which is about neighbouring block
     * capabilities.
     *
     * <p>{@code io} picks which of the two sets of values to write: {@code IN} for pulling out of the
     * world, {@code OUT} for pushing into it. Anything else is not a choice between them and does
     * nothing.</p>
     */
    private abstract static class AutoWorldIOAction extends RuntimeValueAction {
        @InputPort public IO io = IO.IN;

        @Override
        protected final void apply(ExecContext ctx, MBDMachine machine, IRuntimeValueHolder holder) {
            if (!(holder instanceof IAutoWorldIOTrait worldIOTrait)) return;
            var values = worldIOTrait.getRuntimeAutoWorldIO(ctx.getInput("io", IO.class, IO.IN));
            if (values != null) {
                apply(ctx, values);
            }
        }

        protected abstract void apply(ExecContext ctx, RuntimeAutoWorldIO values);
    }

    /** Turn a trait's world scanning on or off for this machine only. */
    @NodeAttribute(name = "mbd2_machine_set_auto_world_io_enabled", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoWorldIOEnabled extends AutoWorldIOAction {
        @InputPort public boolean enabled;

        @Override
        protected void apply(ExecContext ctx, RuntimeAutoWorldIO values) {
            values.enable.set(ctx.getInput("enabled", Boolean.class, false));
        }
    }

    /** Set how often a trait scans the world, in ticks, for this machine only. */
    @NodeAttribute(name = "mbd2_machine_set_auto_world_io_interval", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoWorldIOInterval extends AutoWorldIOAction {
        @InputPort public int interval = 20;

        @Override
        protected void apply(ExecContext ctx, RuntimeAutoWorldIO values) {
            values.interval.set(Math.max(1, ctx.getInput("interval", Integer.class, 20)));
        }
    }

    /** How much a trait moves per scan — items for an item slot, mB for a tank. */
    @NodeAttribute(name = "mbd2_machine_set_auto_world_io_speed", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoWorldIOSpeed extends AutoWorldIOAction {
        @InputPort public int speed = 1;

        @Override
        protected void apply(ExecContext ctx, RuntimeAutoWorldIO values) {
            values.speed.set(Math.max(0, ctx.getInput("speed", Integer.class, 1)));
        }
    }

    /**
     * Set the box a trait scans, for this machine only.
     *
     * <p>Machine-relative and unrotated, the way the editor authors it — the trait turns it to face the
     * machine when it uses it, so a box wired here does not need rotating first.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_auto_world_io_range", group = ACTION_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetAutoWorldIORange extends AutoWorldIOAction {
        @InputPort public double minX;
        @InputPort public double minY;
        @InputPort public double minZ;
        @InputPort public double maxX = 1;
        @InputPort public double maxY = 1;
        @InputPort public double maxZ = 1;

        @Override
        protected void apply(ExecContext ctx, RuntimeAutoWorldIO values) {
            values.range.set(new AABB(
                    ctx.getInput("minX", Double.class, 0d),
                    ctx.getInput("minY", Double.class, 0d),
                    ctx.getInput("minZ", Double.class, 0d),
                    ctx.getInput("maxX", Double.class, 1d),
                    ctx.getInput("maxY", Double.class, 1d),
                    ctx.getInput("maxZ", Double.class, 1d)));
        }
    }

    /**
     * Whether a trait scans the world at all, and how it is currently set up.
     *
     * <p>{@code supported} is the question to ask before drawing any of the controls above: most traits
     * do not do this, and the setters are silent no-ops on one that does not.</p>
     */
    @NodeAttribute(name = "mbd2_machine_auto_world_io_info", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AutoWorldIOInfo extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @InputPort public IO io = IO.IN;
        @OutputPort public boolean supported;
        @OutputPort public boolean enabled;
        @OutputPort public int interval;
        @OutputPort public int speed;
        @OutputPort public double minX;
        @OutputPort public double minY;
        @OutputPort public double minZ;
        @OutputPort public double maxX;
        @OutputPort public double maxY;
        @OutputPort public double maxZ;

        @Override
        public void evaluate(EvalContext ctx) {
            RuntimeAutoWorldIO values = null;
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            if (target != null
                    && holder(target, ctx.getInput("trait", String.class, "")) instanceof IAutoWorldIOTrait worldIOTrait) {
                values = worldIOTrait.getRuntimeAutoWorldIO(ctx.getInput("io", IO.class, IO.IN));
            }
            ctx.setOutput("supported", values != null);
            ctx.setOutput("enabled", values != null && values.enable.get());
            ctx.setOutput("interval", values == null ? 0 : values.intervalTicks());
            ctx.setOutput("speed", values == null ? 0 : values.speed.get());
            var box = values == null ? EMPTY_BOX : values.range.get();
            ctx.setOutput("minX", box.minX);
            ctx.setOutput("minY", box.minY);
            ctx.setOutput("minZ", box.minZ);
            ctx.setOutput("maxX", box.maxX);
            ctx.setOutput("maxY", box.maxY);
            ctx.setOutput("maxZ", box.maxZ);
        }
    }

    /**
     * Read a runtime value by name — the mirror of {@link SetRuntimeValue}.
     *
     * <p>Without this a graph can write a per-machine value and never read it back, which makes the
     * whole system write-only: a blueprint that stores a mode on a machine cannot then act on it.
     * The value returned is what the machine <em>uses</em> — its override if it has one, its
     * definition's otherwise — which is the question nearly every caller is asking.
     * {@code Is Runtime Value Overridden} answers the other one.</p>
     *
     * <p>{@code found} separates "the value is false/zero" from "there is no such value here",
     * which a typed output cannot express on its own.</p>
     */
    private abstract static class GetRuntimeValue extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @InputPort public String key = "";
        @OutputPort public boolean found;

        @Override
        public final void evaluate(EvalContext ctx) {
            ctx.setOutput("found", false);
            publish(ctx, null);
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var key = ctx.getInput("key", String.class, "");
            if (target == null || key.isEmpty()) return;
            var holder = holder(target, ctx.getInput("trait", String.class, ""));
            if (holder == null || holder.getRuntimeValues().slot(key) == null) return;
            ctx.setOutput("found", true);
            publish(ctx, holder.getRuntimeValues().get(key));
        }

        /** Put {@code value} on this node's typed output, or its zero when there is nothing to put. */
        protected abstract void publish(EvalContext ctx, @Nullable Object value);
    }

    /** @see GetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_get_runtime_bool", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeBool extends GetRuntimeValue {
        @OutputPort public boolean value;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            ctx.setOutput("value", value instanceof Boolean bool && bool);
        }
    }

    /** @see GetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_get_runtime_int", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeInt extends GetRuntimeValue {
        @OutputPort public int value;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            ctx.setOutput("value", value instanceof Number number ? number.intValue() : 0);
        }
    }

    /** @see GetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_get_runtime_io", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeIO extends GetRuntimeValue {
        @OutputPort public IO value;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            ctx.setOutput("value", value instanceof IO io ? io : IO.NONE);
        }
    }

    /** @see GetRuntimeValue */
    @NodeAttribute(name = "mbd2_machine_get_runtime_float", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeFloat extends GetRuntimeValue {
        @OutputPort public float value;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            ctx.setOutput("value", value instanceof Number number ? number.floatValue() : 0f);
        }
    }

    /**
     * @see GetRuntimeValue
     *
     * <p>Every slot has a readable text form, so this answers for all of them: an enum comes back as its
     * constant name and a list of slot names comma-separated, which is the form
     * {@code Set Runtime Value (Text)} takes back.</p>
     */
    @NodeAttribute(name = "mbd2_machine_get_runtime_string", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeString extends GetRuntimeValue {
        @OutputPort public String value;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            ctx.setOutput("value", asText(value));
        }
    }

    /** Read a box-shaped runtime value. @see SetRuntimeBox */
    @NodeAttribute(name = "mbd2_machine_get_runtime_box", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetRuntimeBox extends GetRuntimeValue {
        @OutputPort public double minX;
        @OutputPort public double minY;
        @OutputPort public double minZ;
        @OutputPort public double maxX;
        @OutputPort public double maxY;
        @OutputPort public double maxZ;

        @Override
        protected void publish(EvalContext ctx, @Nullable Object value) {
            var box = value instanceof AABB aabb ? aabb : EMPTY_BOX;
            ctx.setOutput("minX", box.minX);
            ctx.setOutput("minY", box.minY);
            ctx.setOutput("minZ", box.minZ);
            ctx.setOutput("maxX", box.maxX);
            ctx.setOutput("maxY", box.maxY);
            ctx.setOutput("maxZ", box.maxZ);
        }
    }

    /**
     * Every runtime value name available on the target, comma-separated, and how many there are.
     *
     * <p>The rest of these nodes are addressed by name, and until now there was no way to find out what
     * the names <em>are</em> short of reading the source — a typo just logged a warning somewhere.
     * Wire this into a debug label while authoring, or use {@code count} to check a trait supports
     * anything at all before offering a panel of controls for it.</p>
     */
    @NodeAttribute(name = "mbd2_machine_runtime_value_keys", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class RuntimeValueKeys extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @OutputPort public String keys;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("keys", "");
            ctx.setOutput("count", 0);
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            if (target == null) return;
            var holder = holder(target, ctx.getInput("trait", String.class, ""));
            if (holder == null) return;
            var slots = holder.getRuntimeValues().slots();
            ctx.setOutput("keys", slots.stream().map(RuntimeValue::getKey).collect(Collectors.joining(",")));
            ctx.setOutput("count", slots.size());
        }
    }

    /**
     * Which way a trait's capability faces on one side — the mirror of {@code Set Capability IO Side}.
     *
     * <p>{@code side} is a world direction, resolved against the machine's facing, exactly as the
     * setter takes one; {@code Relative Side} converts one of the machine's own faces into it. This is
     * about what neighbouring pipes and hoppers may do, which is a different question from auto IO —
     * that is the machine reaching out, this is what it lets others reach in for.</p>
     */
    @NodeAttribute(name = "mbd2_machine_get_capability_io_side", group = INFO_GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class GetCapabilityIOSide extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String trait = "";
        @InputPort public Direction side = Direction.NORTH;
        @OutputPort public IO io;
        @OutputPort public boolean supported;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("io", IO.NONE);
            ctx.setOutput("supported", false);
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var side = ctx.getInput("side", Direction.class, null);
            if (target == null || side == null) return;
            if (!(holder(target, ctx.getInput("trait", String.class, ""))
                    instanceof SimpleCapabilityTrait<?, ?> capabilityTrait)) {
                return;
            }
            ctx.setOutput("supported", true);
            ctx.setOutput("io", capabilityTrait.capabilityIO.getIO(
                    target.getFrontFacing().orElse(Direction.NORTH), side));
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

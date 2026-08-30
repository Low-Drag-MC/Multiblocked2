package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineTargetActionNode;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * The things a blueprint can do <em>to</em> a machine.
 *
 * <p>Server-side by default — {@link MachineTargetActionNode} refuses to run one on a remote machine,
 * because client-side events do fire ({@code Client Tick}, the UI ones) and a mutation that reached
 * one would desync silently. The presentation actions declare a different {@code Side}; notably
 * {@code Play State Sound} is {@code CLIENT}, since the method it calls does not exist on a server.</p>
 */
public final class MachineActionNodes {

    private static final String GROUP = "mbd2/machine/action";

    private MachineActionNodes() {}

    /**
     * Move the machine to another state, which is what drives its renderer, shape, light level and
     * sound. Unknown state names are ignored.
     *
     * <p>Goes through {@code setMachineState}, so it fires {@code State Changed} — including into this
     * same blueprint. Setting the state the machine is already in does nothing and fires nothing.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_state", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetState extends MachineTargetActionNode {
        @InputPort public String state = "";

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var state = ctx.getInput("state", String.class, "");
            if (!state.isEmpty()) machine.setMachineState(state);
        }
    }

    /** Turn the machine to face a direction. Ignored if the direction is not valid for it. */
    @NodeAttribute(name = "mbd2_machine_set_front_facing", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetFrontFacing extends MachineTargetActionNode {
        @InputPort public Direction facing = Direction.NORTH;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var facing = ctx.getInput("facing", Direction.class, null);
            if (facing != null && machine.isFacingValid(facing)) machine.setFrontFacing(facing);
        }
    }

    /**
     * Override the machine's tier at runtime.
     *
     * <p>A negative value clears the override and returns the machine to the tier its definition
     * configures.</p>
     */
    @NodeAttribute(name = "mbd2_machine_set_tier", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetTier extends MachineTargetActionNode {
        @InputPort public int tier;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.setMachineLevel(ctx.getInput("tier", Integer.class, 0));
        }
    }

    /**
     * Replace the machine's custom data tag.
     *
     * <p>Replaces rather than merges, which is the difference between this and
     * {@code Merge Custom Data} — nothing to do with change tracking. The incoming tag is copied
     * because it belongs to the graph: storing it directly would leave the machine's state aliased to
     * a value a later node is free to mutate.</p>
     *
     * <h2>How the change is noticed</h2>
     * By content, not by identity. {@code customData} is {@code @DescSynced}, and LDLib2 gives a
     * {@code Tag} field a {@code MutableDirectRef} whose mark is a deep {@code Tag::copy} compared with
     * {@code equals} each tick — so an in-place edit is seen, and replacing the tag with an equal one
     * is not. Neither this node nor the setter pushes anything; the ref polls.
     */
    @NodeAttribute(name = "mbd2_machine_set_custom_data", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetCustomData extends MachineTargetActionNode {
        @InputPort public CompoundTag data;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var data = ctx.getInput("data", CompoundTag.class, null);
            machine.setCustomData(data == null ? new CompoundTag() : data.copy());
        }
    }

    /**
     * Merge keys into the machine's custom data, leaving the rest alone.
     *
     * <p>Merged in place. The change is still noticed — see {@link SetCustomData} on why tracking is
     * by content — so the copy-then-replace this used to do bought nothing and cost a deep tag copy
     * every call, which is paid per machine per tick by any blueprint that keeps state.</p>
     *
     * @see SetCustomData
     */
    @NodeAttribute(name = "mbd2_machine_merge_custom_data", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class MergeCustomData extends MachineTargetActionNode {
        @InputPort public CompoundTag data;

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var data = ctx.getInput("data", CompoundTag.class, null);
            if (data == null || data.isEmpty()) return;
            machine.getCustomData().merge(data);
        }
    }

    /** Re-render the machine's chunk. For a renderer whose look depends on something the game cannot see. */
    @NodeAttribute(name = "mbd2_machine_schedule_render_update", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ScheduleRenderUpdate extends MachineTargetActionNode {
        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.scheduleRenderUpdate();
        }
    }

    /** Send the machine's block state to clients and poke its neighbours. */
    @NodeAttribute(name = "mbd2_machine_notify_block_update", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class NotifyBlockUpdate extends MachineTargetActionNode {
        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.notifyBlockUpdate();
        }
    }

    /** Mark the machine's chunk dirty so its data is saved. */
    @NodeAttribute(name = "mbd2_machine_mark_dirty", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class MarkDirty extends MachineTargetActionNode {
        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            machine.markDirty();
        }
    }

    /**
     * Start the looping sound a machine state defines.
     *
     * <p>Client-only, and refused on the server rather than silently doing nothing there: the sound is
     * a client object with no server counterpart, so this belongs downstream of {@code Client Tick} or
     * a client-side {@code State Changed}.</p>
     */
    @NodeAttribute(name = "mbd2_machine_play_state_sound", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class PlayStateSound extends MachineTargetActionNode {
        @InputPort public String state = "";

        @Override
        protected Side side() {
            return Side.CLIENT;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var state = ctx.getInput("state", String.class, "");
            if (!state.isEmpty()) machine.playStateSound(state);
        }
    }

    /**
     * Trigger a Geckolib animation on the machine's renderer.
     *
     * <p>Safe from either side: called on the server it is relayed to every tracking client, called on
     * the client it plays locally. Leave {@code controller} empty for the default controller.</p>
     */
    @NodeAttribute(name = "mbd2_machine_trigger_anim", group = GROUP, modID = "geckolib",
            graphTypes = MachineBlueprintGraph.class)
    public static class TriggerAnimation extends MachineTargetActionNode {
        @InputPort public String controller = "";
        @InputPort public String animation = "";
        @InputPort public float speed = 1f;

        @Override
        protected Side side() {
            return Side.BOTH;
        }

        @Override
        protected void apply(ExecContext ctx, MBDMachine machine) {
            var animation = ctx.getInput("animation", String.class, "");
            if (animation.isEmpty()) return;
            machine.triggerGeckolibAnim(ctx.getInput("controller", String.class, ""), animation,
                    ctx.getInput("speed", Float.class, 1f));
        }
    }
}

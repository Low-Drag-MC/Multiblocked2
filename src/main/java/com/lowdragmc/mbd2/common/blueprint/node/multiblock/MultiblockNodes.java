package com.lowdragmc.mbd2.common.blueprint.node.multiblock;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reading a multiblock: whether it is formed, what its parts are, and what is wrong when it is not.
 *
 * <p>Plain nodes rather than an info context, because a multiblock read is nearly always a single
 * question — "am I formed?", "how many parts?" — rather than the eight-properties-of-one-target shape
 * that makes a context pay for itself.</p>
 *
 * <p>Every node here resolves its target the same way: the wired machine if there is one, otherwise the
 * blueprint's own. A blueprint attached to a controller therefore needs no wires at all, and the same
 * blueprint attached to a plain machine reads {@code isFormed = false} rather than failing.</p>
 */
public final class MultiblockNodes {

    private static final String GROUP = "mbd2/multiblock";

    private MultiblockNodes() {}

    /** Whether the controller's structure is currently formed. False for a machine that is not one. */
    @NodeAttribute(name = "mbd2_multiblock_is_formed", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class IsFormed extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public boolean formed;
        @OutputPort public boolean isController;

        @Override
        public void evaluate(EvalContext ctx) {
            var controller = controllerOf(ctx);
            ctx.setOutput("isController", controller != null);
            ctx.setOutput("formed", controller != null && controller.isFormed());
        }
    }

    /**
     * Whether the structure is formed <em>and</em> allowed to work.
     *
     * <p>Distinct from {@link IsFormed}: a structure can be formed but held invalid — that is the check
     * the recipe logic itself uses, so it is the one a blueprint gating on "is this machine running"
     * wants.</p>
     */
    @NodeAttribute(name = "mbd2_multiblock_is_formed_valid", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class IsFormedValid extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public boolean value;

        @Override
        public void evaluate(EvalContext ctx) {
            var controller = controllerOf(ctx);
            ctx.setOutput("value", controller != null && controller.isFormedValid());
        }
    }

    /** Every part currently belonging to the controller, for iterating with the generic list nodes. */
    @NodeAttribute(name = "mbd2_multiblock_parts", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Parts extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public List<IMultiPart> parts;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            var controller = controllerOf(ctx);
            List<IMultiPart> parts = controller == null ? List.of() : controller.getParts();
            ctx.setOutput("parts", parts);
            ctx.setOutput("count", parts.size());
        }
    }

    /**
     * The positions of the controller's parts.
     *
     * <p>A separate node from {@link Parts} because the positions are what a graph usually wants — to
     * spawn a particle at each, to look for a block under one — and going from a part to its position
     * otherwise needs a cast the graph has no node for.</p>
     */
    @NodeAttribute(name = "mbd2_multiblock_part_positions", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class PartPositions extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public List<BlockPos> positions;

        @Override
        public void evaluate(EvalContext ctx) {
            var controller = controllerOf(ctx);
            if (controller == null) {
                ctx.setOutput("positions", List.of());
                return;
            }
            var positions = new ArrayList<BlockPos>();
            for (var part : controller.getParts()) {
                positions.add(part.getPos());
            }
            ctx.setOutput("positions", positions);
        }
    }

    /** The machine of a multiblock part, so the machine nodes can read it. */
    @NodeAttribute(name = "mbd2_multiblock_part_machine", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class PartMachine extends AnnotatedNode {
        @InputPort public IMultiPart part;
        @OutputPort public MBDMachine machine;

        @Override
        public void evaluate(EvalContext ctx) {
            var part = ctx.getInput("part", IMultiPart.class, null);
            ctx.setOutput("machine", part instanceof MBDMachine partMachine ? partMachine : null);
        }
    }

    /**
     * Why the structure did not form, when it did not.
     *
     * <p>The same text the multiblock preview shows. Absent while the structure is fine.</p>
     */
    @NodeAttribute(name = "mbd2_multiblock_error", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class StructureError extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public boolean hasError;
        @OutputPort public Component error;
        @OutputPort public BlockPos errorPos;

        @Override
        public void evaluate(EvalContext ctx) {
            var controller = controllerOf(ctx);
            if (controller == null) return;
            var state = controller.getMultiblockState();
            if (state == null || !state.hasError()) return;
            ctx.setOutput("hasError", true);
            ctx.setOutput("error", state.error.getErrorInfo());
            ctx.setOutput("errorPos", state.error.getPos());
        }
    }

    /** The controllers a part belongs to. A shared part can serve more than one. */
    @NodeAttribute(name = "mbd2_multiblock_part_controllers", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class PartControllers extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public List<IMultiController> controllers;
        @OutputPort public boolean isPart;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var part = target instanceof MBDPartMachine partMachine ? partMachine : null;
            ctx.setOutput("isPart", part != null);
            ctx.setOutput("controllers", part == null ? List.of() : part.getControllers());
        }
    }

    /**
     * Re-check the controller's pattern now instead of waiting for the periodic scan.
     *
     * <p>What to call after a blueprint changes a block the pattern depends on — the controller only
     * re-checks every few ticks otherwise.</p>
     */
    @NodeAttribute(name = "mbd2_multiblock_check_pattern", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class CheckPattern extends MachineActionNode {
        @InputPort public MBDMachine machine;
        @OutputPort public boolean formed;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            if (!(target instanceof MBDMultiblockMachine controller) || target.isRemote()) {
                ctx.setOutput("formed", false);
                return;
            }
            ctx.setOutput("formed", controller.checkPatternWithTryLock());
        }
    }

    /** The controller this node acts on: the wired machine, or the blueprint's own. */
    @Nullable
    private static MBDMultiblockMachine controllerOf(EvalContext ctx) {
        var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
        return target instanceof MBDMultiblockMachine controller ? controller : null;
    }
}

package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Getting hold of a machine — this blueprint's own, one in the world, or one of its traits.
 *
 * <p>{@link Own} is the node every other machine node implicitly uses when its {@code machine} input is
 * unwired; it exists explicitly so the machine can also be fed to the generic nodes (comparisons, list
 * operations, a subgraph parameter) that know nothing about the fallback.</p>
 */
public final class MachineLookupNodes {

    private static final String GROUP = "mbd2/machine";

    private MachineLookupNodes() {}

    /** The machine this blueprint is running for. */
    @NodeAttribute(name = "mbd2_machine_self", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Own extends AnnotatedNode {
        @OutputPort public MBDMachine machine;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("machine", MachineNodes.ownMachine(ctx.getExecutor()));
        }
    }

    /**
     * The MBD machine at a position, or nothing if that block is not one.
     *
     * <p>The level input falls back to the blueprint's own level, so reaching a neighbour is one
     * {@code Offset} node away rather than a level wire plus an offset.</p>
     */
    @NodeAttribute(name = "mbd2_machine_at_pos", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AtPosition extends AnnotatedNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @OutputPort public MBDMachine machine;
        @OutputPort public boolean found;

        @Override
        public void evaluate(EvalContext ctx) {
            Level target = ctx.getInput("level", Level.class, null);
            if (target == null) {
                var own = MachineNodes.ownMachine(ctx.getExecutor());
                target = own == null ? null : own.getLevel();
            }
            BlockPos at = ctx.getInput("pos", BlockPos.class, null);
            if (target == null || at == null) {
                ctx.setOutput("found", false);
                return;
            }
            var machine = IMachine.ofMachine(target, at)
                    .filter(MBDMachine.class::isInstance)
                    .map(MBDMachine.class::cast)
                    .orElse(null);
            ctx.setOutput("machine", machine);
            ctx.setOutput("found", machine != null);
        }
    }

    /**
     * A machine's trait by name — an item slot group, a fluid tank, an energy storage.
     *
     * <p>The name is the one shown in the machine editor's trait list. Feed the result to the
     * {@code Item Slots} / {@code Fluid Tanks} / {@code Energy} nodes.</p>
     */
    @NodeAttribute(name = "mbd2_machine_trait", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Trait extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @InputPort public String name = "";
        @OutputPort public ITrait trait;
        @OutputPort public boolean found;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var name = ctx.getInput("name", String.class, "");
            var trait = target == null || name.isEmpty() ? null : target.getTraitByName(name);
            ctx.setOutput("trait", trait);
            ctx.setOutput("found", trait != null);
        }
    }

    /** Every trait on the machine, for iterating with the generic list nodes. */
    @NodeAttribute(name = "mbd2_machine_traits", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Traits extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public List<ITrait> traits;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            ctx.setOutput("traits", target == null ? List.of() : target.getAdditionalTraits());
        }
    }

    /**
     * Narrow a machine to a multiblock controller.
     *
     * <p>A separate node rather than an automatic conversion because the answer can be no: the same
     * blueprint may be attached to both a controller and a plain machine, and {@code isController}
     * is how it tells.</p>
     */
    @NodeAttribute(name = "mbd2_machine_as_controller", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AsController extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public MBDMultiblockMachine controller;
        @OutputPort public boolean isController;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var controller = target instanceof MBDMultiblockMachine multi ? multi : null;
            ctx.setOutput("controller", controller);
            ctx.setOutput("isController", controller != null);
        }
    }

    /** Narrow a machine to a multiblock part. @see AsController */
    @NodeAttribute(name = "mbd2_machine_as_part", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class AsPart extends AnnotatedNode {
        @InputPort public MBDMachine machine;
        @OutputPort public MBDPartMachine part;
        @OutputPort public boolean isPart;

        @Override
        public void evaluate(EvalContext ctx) {
            var target = MachineNodes.resolve(ctx, MachineNodes.MACHINE_INPUT);
            var part = target instanceof MBDPartMachine partMachine ? partMachine : null;
            ctx.setOutput("part", part);
            ctx.setOutput("isPart", part != null);
        }
    }
}

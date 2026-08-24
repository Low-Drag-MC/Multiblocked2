package com.lowdragmc.mbd2.common.blueprint.node.machine;

import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoContextNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import org.jetbrains.annotations.Nullable;

/**
 * Holds an {@link MBDMachine} for the blocks in {@link MachineInfoBlocks} to read.
 *
 * <p>Leave {@code target} unwired to read the blueprint's own machine — which is what almost every
 * graph wants. Wire it to reach a different one: a neighbour found with {@code Machine At Position},
 * or a multiblock part.</p>
 */
@NodeAttribute(name = "mbd2_machine_info", group = "mbd2/machine", graphTypes = MachineBlueprintGraph.class)
public class MachineInfoNode extends MachineInfoContextNode<MBDMachine> {

    @Override
    protected Class<MBDMachine> targetClass() {
        return MBDMachine.class;
    }

    @Override
    @Nullable
    protected MBDMachine defaultTarget(EvalContext ctx) {
        return MachineNodes.ownMachine(ctx.getExecutor());
    }
}

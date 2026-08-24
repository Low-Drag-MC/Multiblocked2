package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineNeighborChangedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** Fires when a block next to the machine changes — the usual hook for reacting to redstone. */
@NodeAttribute(name = "mbd2_event_neighbor_changed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class NeighborChangedEventNode extends MachineEventNode<MachineNeighborChangedEvent> {

    @OutputPort
    public Block block;
    @OutputPort
    public BlockPos fromPos;

    @Override
    public Class<MachineNeighborChangedEvent> eventClass() {
        return MachineNeighborChangedEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineNeighborChangedEvent event) {
        ctx.setOutput("block", event.block);
        ctx.setOutput("fromPos", event.fromPos);
    }
}

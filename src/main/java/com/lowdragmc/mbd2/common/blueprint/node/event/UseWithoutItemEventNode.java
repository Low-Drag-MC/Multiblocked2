package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseWithoutItemEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fires when a player right-clicks the machine empty-handed.
 *
 * <p>Decide the outcome with {@code Set Interaction Result}.</p>
 */
@NodeAttribute(name = "mbd2_event_use_without_item", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class UseWithoutItemEventNode extends MachineEventNode<MachineUseWithoutItemEvent> {

    @OutputPort
    public Player player;
    @OutputPort
    public BlockHitResult hit;

    @Override
    public Class<MachineUseWithoutItemEvent> eventClass() {
        return MachineUseWithoutItemEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineUseWithoutItemEvent event) {
        ctx.setOutput("player", event.player);
        ctx.setOutput("hit", event.hit);
    }
}

package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineDropsEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Fires while the machine is being broken, with the list of stacks it is about to drop.
 *
 * <p>{@code drops} is the live list. Use {@code Set Event Drops} to replace it — editing the list
 * through the generic list nodes builds a new list and leaves the event's untouched.</p>
 */
@NodeAttribute(name = "mbd2_event_drops", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class DropsEventNode extends MachineEventNode<MachineDropsEvent> {

    @OutputPort
    public Entity entity;
    @OutputPort
    public List<ItemStack> drops;

    @Override
    public Class<MachineDropsEvent> eventClass() {
        return MachineDropsEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineDropsEvent event) {
        ctx.setOutput("entity", event.entity);
        ctx.setOutput("drops", event.drops);
    }
}

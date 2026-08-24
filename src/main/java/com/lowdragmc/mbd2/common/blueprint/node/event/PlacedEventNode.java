package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachinePlacedEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Fires when the machine is placed. {@code placer} is the entity that placed it, and may be null. */
@NodeAttribute(name = "mbd2_event_placed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class PlacedEventNode extends MachineEventNode<MachinePlacedEvent> {

    /**
     * Typed as {@code Entity} rather than the event's {@code LivingEntity} so it lands on the handle
     * KilaGraph's entity nodes already speak. Nothing downstream is narrower than {@code Entity}, and a
     * second handle for the same object would just split the ecosystem in two.
     */
    @OutputPort
    public Entity placer;
    @OutputPort
    public ItemStack itemStack;

    @Override
    public Class<MachinePlacedEvent> eventClass() {
        return MachinePlacedEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachinePlacedEvent event) {
        ctx.setOutput("placer", event.player);
        ctx.setOutput("itemStack", event.itemStack);
    }
}

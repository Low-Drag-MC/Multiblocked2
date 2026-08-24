package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseItemOnEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fires when a player right-clicks the machine holding an item.
 *
 * <p>Decide the outcome with {@code Set Item Interaction Result}; leaving it unset lets the machine's
 * normal handling (opening its UI) proceed.</p>
 */
@NodeAttribute(name = "mbd2_event_use_item_on", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class UseItemOnEventNode extends MachineEventNode<MachineUseItemOnEvent> {

    @OutputPort
    public Player player;
    @OutputPort
    public InteractionHand hand;
    @OutputPort
    public ItemStack heldItem;
    @OutputPort
    public BlockHitResult hit;

    @Override
    public Class<MachineUseItemOnEvent> eventClass() {
        return MachineUseItemOnEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineUseItemOnEvent event) {
        ctx.setOutput("player", event.player);
        ctx.setOutput("hand", event.hand);
        // Derived rather than carried by the event: every graph that hooks this wants the stack, and
        // player+hand -> stack is the step it would otherwise have to wire by hand every time.
        ctx.setOutput("heldItem", event.player == null ? ItemStack.EMPTY : event.player.getItemInHand(event.hand));
        ctx.setOutput("hit", event.hit);
    }
}

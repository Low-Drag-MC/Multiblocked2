package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUseCatalystEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fires when a player uses a catalyst item on a multiblock controller to form it.
 *
 * <p>Cancelable: cancelling stops the structure from forming.</p>
 */
@NodeAttribute(name = "mbd2_event_use_catalyst", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class UseCatalystEventNode extends MachineEventNode<MachineUseCatalystEvent> {

    @OutputPort
    public ItemStack catalyst;
    @OutputPort
    public Player player;
    @OutputPort
    public InteractionHand hand;

    @Override
    public Class<MachineUseCatalystEvent> eventClass() {
        return MachineUseCatalystEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineUseCatalystEvent event) {
        ctx.setOutput("catalyst", event.catalyst);
        ctx.setOutput("player", event.player);
        ctx.setOutput("hand", event.hand);
    }
}

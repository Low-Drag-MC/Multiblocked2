package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineDropsEvent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Replace what the machine drops when broken. Only meaningful under {@code Machine Drops}.
 *
 * <p>Mutates the event's list in place rather than reassigning it, because the list the event carries
 * is the caller's — {@code MBDMachine.onDrops} hands its own list to the event and never reads a
 * replacement back, so assigning a new one would silently do nothing.</p>
 */
@NodeAttribute(name = "mbd2_event_set_drops", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class SetEventDropsNode extends MachineActionNode {

    @InputPort
    public List<ItemStack> drops;

    @Override
    protected void run(ExecContext ctx, MachineEnvironment env) {
        if (!(env.getCurrentEvent() instanceof MachineDropsEvent event) || event.drops == null) return;
        var replacement = ctx.getInput("drops", List.class, null);
        if (replacement == null) return;
        event.drops.clear();
        for (Object entry : replacement) {
            if (entry instanceof ItemStack stack && !stack.isEmpty()) {
                event.drops.add(stack);
            }
        }
    }
}

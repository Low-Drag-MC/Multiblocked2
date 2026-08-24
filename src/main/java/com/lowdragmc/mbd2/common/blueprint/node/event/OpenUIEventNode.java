package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOpenUIEvent;
import net.minecraft.world.entity.player.Player;

/** Fires before the machine's UI opens. Cancelable: cancelling keeps the UI closed. */
@NodeAttribute(name = "mbd2_event_open_ui", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OpenUIEventNode extends MachineEventNode<MachineOpenUIEvent> {

    @OutputPort
    public Player player;

    @Override
    public Class<MachineOpenUIEvent> eventClass() {
        return MachineOpenUIEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineOpenUIEvent event) {
        ctx.setOutput("player", event.player);
    }
}

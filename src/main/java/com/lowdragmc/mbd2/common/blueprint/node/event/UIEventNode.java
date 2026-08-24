package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUIEvent;
import net.minecraft.world.entity.player.Player;

/** Fires while the machine's UI is being assembled, so a blueprint can inspect or extend it. */
@NodeAttribute(name = "mbd2_event_ui", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class UIEventNode extends MachineEventNode<MachineUIEvent> {

    @OutputPort
    public UI ui;
    @OutputPort
    public Player player;

    @Override
    public Class<MachineUIEvent> eventClass() {
        return MachineUIEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineUIEvent event) {
        ctx.setOutput("ui", event.ui);
        ctx.setOutput("player", event.player);
    }
}

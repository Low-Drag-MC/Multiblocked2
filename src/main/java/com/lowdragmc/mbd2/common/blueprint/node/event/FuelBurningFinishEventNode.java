package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineFuelBurningFinishEvent;

/** Fires when a fuel recipe finishes burning. The recipe output may be null. */
@NodeAttribute(name = "mbd2_event_fuel_burning_finish", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class FuelBurningFinishEventNode extends MachineEventNode<MachineFuelBurningFinishEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineFuelBurningFinishEvent> eventClass() {
        return MachineFuelBurningFinishEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineFuelBurningFinishEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

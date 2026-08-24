package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeStatusChangedEvent;

/** Fires when the recipe logic moves between idle / working / waiting / suspend. */
@NodeAttribute(name = "mbd2_event_recipe_status_changed", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class RecipeStatusChangedEventNode extends MachineEventNode<MachineRecipeStatusChangedEvent> {

    @OutputPort
    public RecipeLogic.Status oldStatus;
    @OutputPort
    public RecipeLogic.Status newStatus;

    @Override
    public Class<MachineRecipeStatusChangedEvent> eventClass() {
        return MachineRecipeStatusChangedEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineRecipeStatusChangedEvent event) {
        ctx.setOutput("oldStatus", event.oldStatus);
        ctx.setOutput("newStatus", event.newStatus);
    }
}

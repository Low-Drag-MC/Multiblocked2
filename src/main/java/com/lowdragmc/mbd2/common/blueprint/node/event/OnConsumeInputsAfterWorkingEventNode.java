package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineOnConsumeInputsAfterWorkingEvent;

/** Fires after a running recipe has consumed its per-tick inputs. */
@NodeAttribute(name = "mbd2_event_on_consume_inputs_after_working", group = "mbd2/event", graphTypes = MachineBlueprintGraph.class)
public class OnConsumeInputsAfterWorkingEventNode extends MachineEventNode<MachineOnConsumeInputsAfterWorkingEvent> {

    @OutputPort
    public MBDRecipe recipe;

    @Override
    public Class<MachineOnConsumeInputsAfterWorkingEvent> eventClass() {
        return MachineOnConsumeInputsAfterWorkingEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineOnConsumeInputsAfterWorkingEvent event) {
        ctx.setOutput("recipe", event.recipe);
    }
}

package com.lowdragmc.mbd2.common.graphprocessor;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.processor.TriggerProcessor;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;

public class MachineEventGraphProcessor extends TriggerProcessor {
    private final Class<? extends MachineEvent> eventType;

    public MachineEventGraphProcessor(Class<? extends MachineEvent> eventType, BaseGraph graph) {
        super(graph);
        this.eventType = eventType;
        this.graph.updateComputeOrder(BaseGraph.ComputeOrderType.DepthFirst);
        this.updateComputeOrder();
    }

    public void postEvent(MachineEvent event) {
        if (event.getClass() != this.eventType) {
            MBD2.LOGGER.error("Attempted to post event of type " + event.getClass().getName() + " to processor of type " + this.eventType.getName());
            return;
        }
        // bind parameters -> run -> gather parameters
        event.bindParameters(graph.exposedParameters);
        run();
        event.gatherParameters(graph.exposedParameters);
    }

}

package com.lowdragmc.mbd2.common.machine.definition.config.event.graphprocess;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.mbd2.MBD2;

import java.util.List;

public class MachineEventGraphView extends GraphViewWidget {
    public MachineEventGraphView(BaseGraph graph, int x, int y, int width, int height) {
        super(graph, x, y, width, height);
    }

    @Override
    protected void setupNodeGroups(List<String> supportNodeGroups) {
        super.setupNodeGroups(supportNodeGroups);
        supportNodeGroups.add("graph_processor.node.mbd2.machine");
        if (MBD2.isGeckolibLoaded()) {
            supportNodeGroups.add("graph_processor.node.mbd2.machine.geckolib");
        }
        if (MBD2.isPhotonLoaded()) {
            supportNodeGroups.add("graph_processor.node.mbd2.machine.photon");
        }
    }
}

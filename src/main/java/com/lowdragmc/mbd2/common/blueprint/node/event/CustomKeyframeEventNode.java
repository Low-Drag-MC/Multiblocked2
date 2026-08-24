package com.lowdragmc.mbd2.common.blueprint.node.event;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineEventNode;
import com.lowdragmc.mbd2.integration.geckolib.MachineCustomKeyframeEvent;

/**
 * Fires when a Geckolib animation reaches a custom instruction keyframe.
 *
 * <p>Client-side, like the animation itself.</p>
 */
@NodeAttribute(name = "mbd2_event_custom_keyframe", group = "mbd2/event", modID = "geckolib",
        graphTypes = MachineBlueprintGraph.class)
public class CustomKeyframeEventNode extends MachineEventNode<MachineCustomKeyframeEvent> {

    @OutputPort
    public String instruction;
    @OutputPort
    public String controllerName;
    @OutputPort
    public double animationTick;

    @Override
    public Class<MachineCustomKeyframeEvent> eventClass() {
        return MachineCustomKeyframeEvent.class;
    }

    @Override
    protected void publish(ExecContext ctx, MachineCustomKeyframeEvent event) {
        ctx.setOutput("instruction", event.instruction);
        ctx.setOutput("controllerName", event.controllerName);
        ctx.setOutput("animationTick", event.animationTick);
    }
}

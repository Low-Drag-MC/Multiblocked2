package com.lowdragmc.mbd2.common.blueprint.node.ui;

import com.lowdragmc.kilagraph.blueprint.nodes.ui.UIActions;
import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.ExecInputPort;
import com.lowdragmc.kilagraph.graph.core.ExecOutputPort;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.ExecutionFlow;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.gui.builtin.BuiltinMachineUIs;

import java.util.List;

/**
 * Reaching MBD2's built-in UI pieces from a graph.
 */
public final class BuiltinUINodes {

    private static final String GROUP = "mbd2/ui";
    private static final String NAME = "name";

    private BuiltinUINodes() {}

    /**
     * A fresh copy of one of MBD2's built-in UIs.
     *
     * <p>{@code ldlib2_ui_load_xml} does the same job for an authored file, and cannot be used for
     * anything a machine UI needs: a machine UI is assembled on both sides, and a ui xml resolves
     * through the active resource manager — assets on the client, datapacks on the server. A built-in
     * is code, so both sides get the same tree with nothing to keep in step.</p>
     *
     * <p>An exec node rather than a pure value for the reason {@code ldlib2_ui_element_new} is one:
     * an element has identity, so when one is made has to be something the graph states.</p>
     */
    @NodeAttribute(name = "mbd2_builtin_ui", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Builtin extends AnnotatedNode {
        @ExecInputPort public ExecutionFlow trigger;
        @ExecOutputPort public ExecutionFlow next;

        @InputPort public String name = "";
        @OutputPort public UIElement root;
        @OutputPort public boolean ok;

        @Override
        public List<String> optionChoices(String optionId) {
            return NAME.equals(optionId) ? names() : List.of();
        }

        @Override
        public void execute(ExecContext ctx) {
            var created = BuiltinMachineUIs.create(ctx.getInput(NAME, String.class, ""));
            UIActions.produce(ctx, "root", created);
            UIActions.done(ctx, created != null);
        }

        @Override
        public void evaluate(EvalContext ctx) {
            UIActions.republish(ctx, "root");
        }
    }

    private static List<String> names() {
        var names = new java.util.ArrayList<String>();
        BuiltinMachineUIs.names().forEach(names::add);
        return names;
    }
}

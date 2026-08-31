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
     * A fresh copy of one of MBD2's built-in UIs, straight from the code that defines it.
     *
     * <h2>Why not {@code ldlib2_ui_template_load}</h2>
     * These built-ins <em>are</em> registered as {@code UITemplate}s, and loading one by path would
     * be the obvious way to reach it. It does not work, because a {@code UITemplate} is a
     * {@code CompoundTag}: the tree goes through NBT on the way out and back, and three things do not
     * survive the trip.
     *
     * <ul>
     *   <li>{@code UIElement.allowHitTest} has no {@code @Configurable}, so it is never written. The
     *       auto-IO panel draws each face's neighbouring block on a child element that opts out of
     *       hit testing; without that flag the child becomes the target of every click and the face
     *       buttons stop working. Nothing throws — the panel just stops responding.</li>
     *   <li>{@code UIElement.deserializeNBT} restores inline styles <b>only on the client</b>
     *       ({@code if (!LDLib2.isServer())}). A machine UI is built on both sides, so the server's
     *       copy would come back unstyled and the two trees would no longer match.</li>
     *   <li>Only inline values are serialised at all, so anything a document deliberately puts lower
     *       in the cascade is lost.</li>
     * </ul>
     *
     * <p>Code has none of those problems: both sides run the same constructor and get the same tree,
     * flags and all. {@code ldlib2_ui_load_xml} is out for a related reason — a ui xml resolves
     * through the active resource manager, which is assets on the client and datapacks on the server,
     * so one file has to ship twice and stay in step.</p>
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

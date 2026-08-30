package com.lowdragmc.mbd2.common.blueprint.node;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;

/**
 * Small pure operations on {@link IO}, the direction enum that runs through auto IO, capability sides
 * and recipe contents alike.
 *
 * <p>They exist because the alternative in a graph is a chain of equality tests and branches per use
 * — six of them for a panel with six sides, which buries the thing the graph is actually saying.</p>
 */
public final class IONodes {

    private static final String GROUP = "mbd2/io";

    private IONodes() {}

    /**
     * The next {@link IO} a click should land on, cycling {@code NONE → IN → OUT → BOTH → NONE}.
     *
     * <p>A side toggle is the reason this exists: a player clicking a face expects it to walk through
     * the options and come back round, and the order below is the one the machine editor's own IO
     * controls use.</p>
     *
     * <p>{@code includeBoth} is separate because not everything can do both at once — a trait whose
     * auto IO only pushes or only pulls would offer a third state that behaves like one of the other
     * two, which reads as a bug to whoever is clicking.</p>
     */
    @NodeAttribute(name = "mbd2_io_next", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Next extends AnnotatedNode {
        @InputPort public IO io = IO.NONE;
        @InputPort public boolean includeBoth = true;
        @OutputPort public IO next;

        @Override
        public void evaluate(EvalContext ctx) {
            var io = ctx.getInput("io", IO.class, IO.NONE);
            var includeBoth = ctx.getInput("includeBoth", Boolean.class, true);
            ctx.setOutput("next", switch (io) {
                case NONE -> IO.IN;
                case IN -> IO.OUT;
                case OUT -> includeBoth ? IO.BOTH : IO.NONE;
                default -> IO.NONE;
            });
        }
    }

    /**
     * An {@link IO} broken out as flags, for a graph that has to branch on which one it is.
     *
     * <p>Comparing enums in a graph means an equality node and a constant per case; this is the same
     * four answers in one node, which is what a UI needs to pick a colour or a label.</p>
     */
    @NodeAttribute(name = "mbd2_io_info", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Info extends AnnotatedNode {
        @InputPort public IO io = IO.NONE;
        @OutputPort public boolean isNone;
        @OutputPort public boolean isIn;
        @OutputPort public boolean isOut;
        @OutputPort public boolean isBoth;
        @OutputPort public String name;

        @Override
        public void evaluate(EvalContext ctx) {
            var io = ctx.getInput("io", IO.class, IO.NONE);
            ctx.setOutput("isNone", io == IO.NONE);
            ctx.setOutput("isIn", io == IO.IN);
            ctx.setOutput("isOut", io == IO.OUT);
            ctx.setOutput("isBoth", io == IO.BOTH);
            ctx.setOutput("name", io.name());
        }
    }
}

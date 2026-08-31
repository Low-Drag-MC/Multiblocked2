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
     * One of four values, picked by an {@link IO} — a colour, a label, a texture, a key.
     *
     * <p>Every UI that draws an IO needs this mapping, and expressing it with equality tests and
     * branches costs four nodes each time and buries what the graph is saying. Here the four answers
     * sit side by side where they can be read and changed, which for a built-in blueprint means a pack
     * author can restyle it without touching Java.</p>
     *
     * <p>Strings rather than an untyped value, because an untyped port has no inline editor: the four
     * answers could then only be wired in, which defeats the point of having them here.</p>
     */
    @NodeAttribute(name = "mbd2_io_choose", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Choose extends AnnotatedNode {
        @InputPort public IO io = IO.NONE;
        @InputPort public String whenNone = "";
        @InputPort public String whenIn = "";
        @InputPort public String whenOut = "";
        @InputPort public String whenBoth = "";
        @OutputPort public String value;

        @Override
        public void evaluate(EvalContext ctx) {
            var port = switch (ctx.getInput("io", IO.class, IO.NONE)) {
                case IN -> "whenIn";
                case OUT -> "whenOut";
                case BOTH -> "whenBoth";
                default -> "whenNone";
            };
            ctx.setOutput("value", ctx.getInput(port, String.class, ""));
        }
    }

    /**
     * The {@link IO} a name stands for, the inverse of {@code IO Info}'s {@code name}.
     *
     * <p>Exists because a sync value is serialised by the accessor for its type, and a name is a
     * {@code String} — which every side can carry — where an enum's support is less certain. Send the
     * name, turn it back here.</p>
     *
     * <p>An unknown name is {@code NONE} rather than an error: a value that arrived garbled should
     * make a panel show nothing, not stop it drawing.</p>
     */
    @NodeAttribute(name = "mbd2_io_of_name", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class OfName extends AnnotatedNode {
        @InputPort public String name = "";
        @OutputPort public IO io;

        @Override
        public void evaluate(EvalContext ctx) {
            var name = ctx.getInput("name", String.class, "");
            var io = IO.NONE;
            for (var candidate : IO.values()) {
                if (candidate.name().equalsIgnoreCase(name)) {
                    io = candidate;
                    break;
                }
            }
            ctx.setOutput("io", io);
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

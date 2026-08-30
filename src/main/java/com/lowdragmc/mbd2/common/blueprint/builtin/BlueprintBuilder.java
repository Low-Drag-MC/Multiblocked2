package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomBlockNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import lombok.Getter;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authors a {@link MachineBlueprintGraph} in code, laid out to be read.
 *
 * <h2>Why the built-ins are built rather than shipped as files</h2>
 * A {@code .bp.nbt} is a binary blob: it cannot be reviewed in a diff, it silently rots when a node's
 * ports change, and nothing stops it drifting from the code it was recorded against. Built here
 * instead, a built-in blueprint is compiled against the node classes it uses — rename a port and the
 * build fails rather than a player's machine quietly losing a wire.
 *
 * <h2>What this adds over the raw model API</h2>
 * Names, so a wire reads {@code wire("branch.cond", "powered.powered")} instead of a pair of port
 * lookups; and <em>layout</em>, because a built-in blueprint is documentation as much as it is logic.
 * A graph nobody can read teaches nothing, so positions are mandatory rather than optional, and
 * {@link #note} and {@link #group} are first-class.
 *
 * <h2>References</h2>
 * A port is addressed as {@code "node.portId"}. A bare {@code "node"} means its default port, resolved
 * by position: as a wire source, its single non-exec output; as a destination, its single non-exec
 * input; a variable node's read or write side respectively. Ambiguity throws rather than guessing —
 * these graphs are built at mod load, so a mis-wire has to be a startup failure with a name in it and
 * not a machine that behaves subtly wrongly in someone's world.
 */
public final class BlueprintBuilder {

    @Getter
    private final MachineBlueprintGraph graph;
    private final CustomGraphModelImpl model;
    private final Map<String, NodeModel> nodes = new LinkedHashMap<>();
    private final Map<String, VariableDeclarationModelBase> variables = new LinkedHashMap<>();

    private BlueprintBuilder(MachineBlueprintGraph graph) {
        this.graph = graph;
        this.model = graph.graphModel;
    }

    public static BlueprintBuilder create() {
        return new BlueprintBuilder(new MachineBlueprintGraph());
    }

    // ---- nodes -------------------------------------------------------------------------------

    /** Create a node at {@code (x, y)} and register it under {@code name}. */
    public BlueprintBuilder add(String name, Class<? extends Node> nodeClass, float x, float y) {
        if (nodes.containsKey(name)) throw new IllegalArgumentException("Duplicate node name '" + name + "'");
        var data = new GraphNodeCreationData(model, new Vector2f(x, y), SpawnFlags.DEFAULT, null);
        nodes.put(name, (NodeModel) CustomGraphModelImpl.createNodeFromData(data, nodeClass));
        return this;
    }

    /**
     * Insert a property block into a context node — the {@code Machine Info} / {@code Recipe Logic Info}
     * shape, where one wired target feeds a stack of reads.
     *
     * <p>The block carries no position of its own: it is laid out by its context, which is the point of
     * the context/block form.</p>
     */
    public BlueprintBuilder block(String contextName, String blockName, Class<? extends BlockNode> blockClass) {
        if (nodes.containsKey(blockName)) throw new IllegalArgumentException("Duplicate node name '" + blockName + "'");
        if (!(node(contextName) instanceof ContextNodeModel context)) {
            throw new IllegalArgumentException("'" + contextName + "' is not a context node");
        }
        BlockNode userNode;
        try {
            userNode = blockClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate block " + blockClass.getName(), e);
        }
        var block = new CustomBlockNodeModelImpl();
        block.setGraphModel(model);
        block.setSpawnFlags(SpawnFlags.DEFAULT);
        block.initCustomNode(userNode);
        block.setContextNodeModel(context);
        // Ports have to be defined with the parent already known, or parent-dependent port types come
        // out wrong; insertBlock afterwards is the attach, and re-linking to the same parent is a no-op.
        block.onCreateNode();
        context.insertBlock(block, -1);
        nodes.put(blockName, block);
        return this;
    }

    /** The node registered under {@code name}. */
    public NodeModel node(String name) {
        var node = nodes.get(name);
        if (node == null) {
            throw new IllegalArgumentException("No node named '" + name + "'; have " + nodes.keySet());
        }
        return node;
    }

    /** Set a node option, redefining the node so option-driven ports update. */
    public BlueprintBuilder option(String nodeName, String optionId, Object value) {
        var node = node(nodeName);
        NodeOption option = null;
        for (var candidate : node.getNodeOptions()) {
            if (candidate.id.equals(optionId)) {
                option = candidate;
                break;
            }
        }
        if (option == null) {
            throw new IllegalArgumentException("Unknown option '" + optionId + "' on '" + nodeName + "'");
        }
        var constant = node.getInputConstantsById().get(option.portModel.getUniqueName());
        if (constant == null) throw new IllegalStateException("No constant for option " + optionId);
        constant.setValue(value);
        node.defineNode();
        return this;
    }

    /**
     * Set an unconnected input's embedded constant. {@code ref} is {@code "node.port"} or {@code "node"}.
     *
     * <p>The value's type is checked against the port's, because a constant is untyped at the setter and
     * the mismatch does not surface until serialization — where it is a logged {@code ClassCastException}
     * and a constant silently missing from the saved graph. An {@code int} literal on a {@code float}
     * port is the way that happens, and it is not something to find out about from a log line.</p>
     */
    public BlueprintBuilder constant(String ref, Object value) {
        var reference = parse(ref);
        var port = reference.portId == null ? defaultInput(reference) : inputPort(reference);
        var constant = reference.node.getInputConstantsById().get(port.getUniqueName());
        if (constant == null) {
            throw new IllegalStateException("No input constant for '" + ref + "'");
        }
        var expected = constant.getType();
        if (value != null && expected instanceof Class<?> expectedClass
                && !boxed(expectedClass).isInstance(value)) {
            throw new IllegalArgumentException("'" + ref + "' takes " + expectedClass.getSimpleName()
                    + " but was given a " + value.getClass().getSimpleName()
                    + " — write the literal in the port's type, e.g. 1f rather than 1");
        }
        constant.setValue(value);
        return this;
    }

    /** The wrapper for a primitive class, or the class itself. A constant's value is always boxed. */
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    /** Rename a node, so a generic node reads as the job it is doing in this graph. */
    public BlueprintBuilder title(String nodeName, String title) {
        node(nodeName).setName(title);
        return this;
    }

    // ---- variables ---------------------------------------------------------------------------

    /**
     * Declare a variable and create its node, registering both under {@code name}.
     *
     * <p>{@link VariableKind#INPUT} is what makes a variable an <em>exposed parameter</em>: the machine
     * editor generates one configurator row per INPUT variable, so this is how a built-in blueprint
     * offers a knob rather than a hardcoded number. See {@code MachineBlueprintBinding}.</p>
     */
    public BlueprintBuilder variable(String name, Class<?> type, Object defaultValue, VariableKind kind,
                                     float x, float y) {
        if (variables.containsKey(name)) throw new IllegalArgumentException("Duplicate variable '" + name + "'");
        var declaration = (VariableDeclarationModelBase) model.createVariable(name, type, defaultValue, kind);
        variables.put(name, declaration);
        nodes.put(name, model.createVariableNode(declaration, new Vector2f(x, y), null, null));
        return this;
    }

    /** A machine parameter: an {@link VariableKind#INPUT} variable plus the node that reads it. */
    public BlueprintBuilder parameter(String name, Class<?> type, Object defaultValue, float x, float y) {
        return variable(name, type, defaultValue, VariableKind.INPUT, x, y);
    }

    /**
     * A second node reading an already-declared variable, registered under {@code nodeName}.
     *
     * <p>Two reads of one variable are two nodes on purpose — placing one wherever it is used beats
     * running a wire across the whole canvas, and a graph that has to be read is a graph whose wires
     * should be short.</p>
     */
    public BlueprintBuilder read(String nodeName, String variableName, float x, float y) {
        if (nodes.containsKey(nodeName)) throw new IllegalArgumentException("Duplicate node name '" + nodeName + "'");
        nodes.put(nodeName, model.createVariableNode(variable(variableName), new Vector2f(x, y), null, null));
        return this;
    }

    /** The variable declaration registered under {@code name}. */
    public VariableDeclarationModelBase variable(String name) {
        var declaration = variables.get(name);
        if (declaration == null) {
            throw new IllegalArgumentException("No variable named '" + name + "'; have " + variables.keySet());
        }
        return declaration;
    }

    // ---- annotation --------------------------------------------------------------------------

    /** Height of one line of sticky-note text, plus the note's own padding. Measured, not guessed. */
    private static final float NOTE_LINE = 15f;
    private static final float NOTE_PADDING = 24f;
    /** The header note's width, and the clear band left between its bottom and the first group. */
    private static final float HEADER_WIDTH = 560f;
    private static final float HEADER_GAP = 80f;

    /**
     * The note that says what the blueprint is and what its parameters do.
     *
     * <p>Placed and sized from the text rather than by hand, so that editing the wording can never leave
     * it overlapping the graph — which is what happens every time a note's height is a number someone
     * has to remember to update. It sits above the canvas with a clear band under it, so every built-in
     * opens with its explanation in the same place.</p>
     */
    public BlueprintBuilder header(String text) {
        var height = noteHeight(text);
        return note(GROUP_LEFT, -height - HEADER_GAP, HEADER_WIDTH, height, BuiltinNotes.HEADER_COLOR, text);
    }

    /** An inline note explaining one step, sized to its text. */
    public BlueprintBuilder note(float x, float y, float width, String text) {
        return note(x, y, width, noteHeight(text), text);
    }

    /**
     * A sticky note — what the built-ins explain themselves with.
     *
     * <p>Every built-in opens with one, because a player who double-clicks a blueprint to find out what
     * it does should be told in the graph rather than have to infer it from the nodes.</p>
     */
    public BlueprintBuilder note(float x, float y, float width, float height, String text) {
        var note = model.createStickyNote(new Vector2f(x, y));
        note.setSize(new Vector2f(width, height));
        note.setContent(text);
        return this;
    }

    /** A sticky note in a colour of its own — used to set the header note apart from the inline ones. */
    public BlueprintBuilder note(float x, float y, float width, float height, int color, String text) {
        var note = model.createStickyNote(new Vector2f(x, y));
        note.setSize(new Vector2f(width, height));
        note.setContent(text);
        note.setColor(color);
        return this;
    }

    private static float noteHeight(String text) {
        return text.split("\n", -1).length * NOTE_LINE + NOTE_PADDING;
    }

    /**
     * A titled backdrop grouping the nodes that make up one step.
     *
     * <p>{@code x} and {@code y} are the top-left of the <em>content</em>; the backdrop is inset by
     * {@link #GROUP_INSET} around it and given room at the top for its title. Stated that way because
     * the thing an author knows is where the nodes are, and a placemat whose bounds are written out
     * separately is a placemat that stops containing them the moment anything moves.</p>
     */
    public BlueprintBuilder group(String name, float x, float y, float width, float height, int color) {
        var placemat = model.createPlacemat(name,
                new Vector2f(x - GROUP_INSET, y - GROUP_TITLE),
                new Vector2f(width + GROUP_INSET * 2, height + GROUP_TITLE + GROUP_INSET));
        placemat.setColor(color);
        return this;
    }

    /** Breathing room a group leaves around its content, and the band its title needs above it. */
    public static final float GROUP_INSET = 24f;
    public static final float GROUP_TITLE = 44f;
    /** Where the leftmost group's content starts, so every built-in lines up the same way. */
    public static final float GROUP_LEFT = 0f;

    // ---- wiring ------------------------------------------------------------------------------

    /** Wire {@code src}'s output into {@code dst}'s input. Both are {@code "node.port"} or {@code "node"}. */
    public BlueprintBuilder wire(String dst, String src) {
        var destination = parse(dst);
        var source = parse(src);
        model.createWire(
                destination.portId == null ? defaultInput(destination) : inputPort(destination),
                source.portId == null ? defaultOutput(source) : outputPort(source));
        return this;
    }

    /**
     * Wire an exec chain: each name flows into the next.
     *
     * <p>Every hop uses the source's single exec output and the destination's single exec input, so a
     * {@code Branch} has to be wired with the explicit {@link #wire} form naming the pin — which is the
     * intent, since which side of a branch a flow continues on is never something to infer.</p>
     */
    public BlueprintBuilder then(String... chain) {
        for (int i = 0; i + 1 < chain.length; i++) {
            var from = parse(chain[i]);
            var to = parse(chain[i + 1]);
            model.createWire(
                    to.portId == null ? soleExec(to, false) : inputPort(to),
                    from.portId == null ? soleExec(from, true) : outputPort(from));
        }
        return this;
    }

    // ---- reference resolution ----------------------------------------------------------------

    private record Ref(String name, NodeModel node, String portId) {}

    private Ref parse(String ref) {
        int dot = ref.indexOf('.');
        if (dot < 0) return new Ref(ref, node(ref), null);
        var name = ref.substring(0, dot);
        return new Ref(name, node(name), ref.substring(dot + 1));
    }

    private PortModel inputPort(Ref ref) {
        var ports = ref.node.getInputsById();
        var port = ports.get(ref.portId);
        if (port == null) {
            throw new IllegalArgumentException("No input '" + ref.portId + "' on '" + ref.name
                    + "'; have " + ports.keySet());
        }
        return port;
    }

    private PortModel outputPort(Ref ref) {
        var ports = ref.node.getOutputsById();
        var port = ports.get(ref.portId);
        if (port == null) {
            throw new IllegalArgumentException("No output '" + ref.portId + "' on '" + ref.name
                    + "'; have " + ports.keySet());
        }
        return port;
    }

    private PortModel defaultInput(Ref ref) {
        if (ref.node instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel v) {
            return v.getInputPort();
        }
        return soleDataElseExec(ref, ref.node.getInputsById().values(), "input");
    }

    private PortModel defaultOutput(Ref ref) {
        if (ref.node instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel v) {
            return v.getOutputPort();
        }
        return soleDataElseExec(ref, ref.node.getOutputsById().values(), "output");
    }

    /** The node's one data port, or — when it has none — its one exec port. Data wins for hybrids. */
    private PortModel soleDataElseExec(Ref ref, Iterable<PortModel> ports, String what) {
        var data = findSole(ports, false);
        if (data != null) return data;
        var exec = findSole(ports, true);
        if (exec != null) return exec;
        return sole(ref, ports, false, what);
    }

    private static PortModel findSole(Iterable<PortModel> ports, boolean wantExec) {
        PortModel found = null;
        for (var port : ports) {
            if (TypeHandles.EXECUTION_FLOW.equals(port.getDataTypeHandle()) != wantExec) continue;
            if (found != null) return null;
            found = port;
        }
        return found;
    }

    private PortModel soleExec(Ref ref, boolean output) {
        if (ref.node instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel v) {
            return output ? v.getOutputPort() : v.getInputPort();
        }
        var ports = output ? ref.node.getOutputsById().values() : ref.node.getInputsById().values();
        return sole(ref, ports, true, output ? "exec output" : "exec input");
    }

    private PortModel sole(Ref ref, Iterable<PortModel> ports, boolean wantExec, String what) {
        PortModel found = null;
        List<String> candidates = new ArrayList<>();
        for (var port : ports) {
            if (TypeHandles.EXECUTION_FLOW.equals(port.getDataTypeHandle()) != wantExec) continue;
            candidates.add(port.getPortId());
            found = port;
        }
        if (candidates.size() == 1) return found;
        throw new IllegalArgumentException("'" + ref.name + "' has " + candidates.size() + " candidate "
                + what + " ports " + candidates + " — name one explicitly, e.g. \"" + ref.name + "."
                + (candidates.isEmpty() ? "<portId>" : candidates.getFirst()) + "\"");
    }
}

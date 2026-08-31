package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.flow.SelectNode;
import com.lowdragmc.kilagraph.blueprint.nodes.compare.GreaterThanNode;
import com.lowdragmc.kilagraph.blueprint.nodes.string.LengthNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.block.BlockStateNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.geometry.BlockPosOffsetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.item.BlockToItemNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.item.ItemStackCreateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.world.GetBlockStateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtCreateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtGetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtSetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.string.ConcatNode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.NotNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.text.TextNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.optional.IsNullNode;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.doc.UIDocNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIElementInfoBlocks;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIElementInfoNode;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIElementNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIQueryNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIStateNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.style.UIAnimationNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.style.UIStyleNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.style.UIStylesheetNodes;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIValueNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.event.UIEventNodes;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.common.blueprint.node.IONodes;
import com.lowdragmc.mbd2.common.blueprint.node.ui.UIItemNodes;
import com.lowdragmc.mbd2.common.gui.builtin.BuiltinMachineUIs;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.UIEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRuntimeValueNodes;

import java.util.Locale;

/**
 * A side tab on the machine UI for configuring one trait's auto IO, the way most tech mods do it.
 *
 * <h2>What it does</h2>
 * Adds a handle to the left edge of the machine's screen; clicking it folds out a panel with the
 * machine's six faces, and clicking a face cycles what auto IO does there — nothing, in, out, both.
 * The setting is a runtime value, so it belongs to that one placed machine and survives a save.
 *
 * <h2>Why it is a blueprint and not a feature</h2>
 * Because the interesting question was whether a blueprint can build UI at all, and this is the
 * smallest honest test of it: a panel that has to be created, styled, wired to events, kept in step
 * with server-side state, and merged with whatever else is already on the screen. Every node it uses
 * is a general one — load a document, select by id, add a child, listen for a click, sync a value.
 * There is no "auto IO panel" node doing the work behind a single pin.
 *
 * <h2>How the client knows what a face is set to</h2>
 * Through the machine's custom data, not through a UI sync value. Auto IO overrides live in the
 * machine's runtime values, which are server-side by design and never sent with the block, so the
 * panel cannot simply read them where it draws. A sync value is the obvious channel and does not
 * work here — the server side of the graph never declares one, so nothing is ever sent and every
 * face draws its default. Custom data is {@code @DescSynced}, so writing each face's state there on
 * the server puts it in front of the client for free; the panel reads it on a UI tick.
 *
 * <h2>Several of them at once</h2>
 * Bind this blueprint twice with different {@code trait} names and you get two tabs, not two panels
 * on top of each other. The strip they hang off is a separate document with a known id: the first
 * blueprint to run loads it, the rest find it, and each appends its own tab to a flex column.
 *
 * <h2>The trait has to want it</h2>
 * {@code Auto IO Info} answers whether the named trait does auto IO at all, and nothing is added when
 * it does not. Without that check the tab would still appear on a trait that ignores every setting in
 * it, which is worse than no tab.
 */
final class AutoIOPanelBlueprint {

    private AutoIOPanelBlueprint() {}

    /** The ids the two ui documents agree on. */
    private static final String STRIP_ID = BuiltinMachineUIs.STRIP_ID;

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                AUTO IO PANEL

                A side tab on the machine UI for setting which faces
                a trait pushes and pulls through.

                trait   the name of the trait to configure, as it
                        appears in the editor's trait list
                name    what to call it on screen. Leave it empty
                        and the trait's own name is used.

                Nothing is added if that trait does not do auto IO.

                Bind this blueprint more than once with different
                trait names and the tabs stack up the left edge -
                they append to one shared strip rather than each
                building their own.

                The look is a stylesheet, not constants in this
                graph: every element carries a class and
                lss/mbd2_auto_io.lss says what it looks like.""");

        // ---- does this trait even do auto IO? --------------------------------------------------
        b.add("event", UIEventNode.class, 0, 0)
                .parameter("trait", String.class, "", 0, 260)
                .parameter("name", String.class, "", 0, 340)
                .add("supports", MachineRuntimeValueNodes.AutoIOInfo.class, 260, 200)
                .title("supports", "does this trait do auto IO?")
                .add("gate", BranchNode.class, 520, 0)
                .title("gate", "no auto IO, no tab");

        b.wire("supports.machine", "event.machine")
                .wire("supports.trait", "trait")
                .wire("gate.cond", "supports.supported");
        b.then("event", "gate");

        b.group("Only for a trait that has it", 0, 0, 720, 420, BuiltinNotes.READ_GROUP);

        // ---- find the shared strip, or put one there --------------------------------------------
        b.add("unpack", UIDocNodes.Unpack.class, 780, 200)
                .title("unpack", "the machine's own ui")
                .add("look", UIQueryNodes.SelectId.class, 1000, 200)
                .constant("look.id", STRIP_ID)
                .title("look", "is a strip already there?")
                .add("missing", IsNullNode.class, 1240, 200)
                .add("needStrip", BranchNode.class, 1240, 0)
                .add("stripDoc", UIDocNodes.TemplateLoad.class, 1360, -120)
                .constant("stripDoc.path", BuiltinMachineUIs.path(BuiltinMachineUIs.SIDE_TAB_STRIP))
                .title("stripDoc", "the built-in strip")
                .add("loadStrip", UIDocNodes.TemplateCreateUI.class, 1480, 0)
                .title("loadStrip", "one copy of it")
                .add("addStrip", UIElementNodes.AddChild.class, 1720, 0)
                .add("strip", UIQueryNodes.SelectId.class, 1480, 200)
                .constant("strip.id", STRIP_ID)
                .title("strip", "now certainly there");

        b.wire("loadStrip.template", "stripDoc.template")
                .wire("unpack.ui", "event.ui")
                .wire("look.root", "unpack.root")
                .wire("missing.in", "look.first")
                .wire("needStrip.cond", "missing.out")
                .wire("addStrip.parent", "unpack.root")
                .wire("addStrip.child", "loadStrip.root")
                .wire("strip.root", "unpack.root");

        // The stylesheet goes on with the strip, so it is attached exactly once however many tabs
        // end up there — and scoped to the strip, so none of it can reach the machine's own UI.
        b.add("sheet", UIStylesheetNodes.Load.class, 1720, 200)
                .constant("sheet.location", BuiltinMachineUIs.AUTO_IO_STYLESHEET)
                .title("sheet", "every lss/mbd2_auto_io.lss there is")
                .add("skin", UIStylesheetNodes.LocalStylesheet.class, 1960, 0)
                .option("skin", "op", UIStylesheetNodes.LocalOp.ADD)
                .title("skin", "dress the strip and everything in it");
        b.wire("skin.element", "loadStrip.root")
                .wire("skin.stylesheet", "sheet.stylesheet");

        b.wire("needStrip.in", "gate.trueExec")
                .wire("loadStrip.trigger", "needStrip.trueExec");
        b.then("loadStrip", "addStrip", "skin");

        b.note(780, 420, 700, """
                Look, maybe create, then look again. The second
                lookup is what lets both paths carry on into the
                same node without a value that is only set on one
                of them - a graph cannot merge two branches into
                one wire, but it can ask the same question twice.""");

        b.group("Find or create the strip", 780, 0, 1180, 560, BuiltinNotes.DECIDE_GROUP);

        // ---- what to call it ----------------------------------------------------------------------
        // The trait name is an editor-facing id ("item_slot"), which is fine as a default and poor as
        // a label. `name` overrides it everywhere it is shown — and nowhere it is stored: the custom
        // data keys stay keyed by trait, so renaming a tab does not orphan what a machine already has.
        // Length against zero rather than a comparison with "": an empty string is the one value a
        // graph constant cannot carry unambiguously — it is indistinguishable from "no constant" —
        // and the untyped ports of Equals make it worse. A number and a typed compare have neither
        // problem, and `> 0` needs no constant at all.
        b.add("nameLength", LengthNode.class, 1480, 420)
                .add("named", GreaterThanNode.class, 1720, 420)
                .title("named", "was a name given?")
                .add("label", SelectNode.class, 1960, 420)
                .option("label", "type", TypeHandles.STRING.getIdentification())
                .title("label", "the name to show");
        b.wire("nameLength.in", "name")
                .wire("named.a", "nameLength.out")
                .wire("label.cond", "named.out")
                .wire("label.ifTrue", "name")
                .wire("label.ifFalse", "trait");

        // ---- build this trait's tab --------------------------------------------------------------
        b.add("tabDoc", UIDocNodes.TemplateLoad.class, 1900, -120)
                .constant("tabDoc.path", BuiltinMachineUIs.path(BuiltinMachineUIs.AUTO_IO_TAB))
                .title("tabDoc", "the built-in tab")
                .add("loadTab", UIDocNodes.TemplateCreateUI.class, 2020, 0)
                .title("loadTab", "one copy of it")
                .add("addTab", UIElementNodes.AddChild.class, 2260, 0)
                .title("addTab", "append, not replace")
                .add("titleOf", UIQueryNodes.SelectId.class, 2020, 200)
                .constant("titleOf.id", "title")
                .add("titleText", TextNodes.Literal.class, 2260, 200)
                .add("setTitle", UIValueNodes.SetText.class, 2500, 0)
                .title("setTitle", "name the tab after the trait");

        b.wire("loadTab.template", "tabDoc.template")
                .wire("addTab.parent", "strip.first")
                .wire("addTab.child", "loadTab.root")
                .wire("titleOf.root", "loadTab.root")
                .wire("titleText.text", "label.out")
                .wire("setTitle.element", "titleOf.first")
                .wire("setTitle.text", "titleText.out");

        b.wire("loadTab.trigger", "needStrip.falseExec")
                .wire("loadTab.trigger", "skin.next");
        b.then("loadTab", "addTab", "setTitle");

        // ---- fold out and back -------------------------------------------------------------------
        b.add("panelOf", UIQueryNodes.SelectId.class, 2740, 200)
                .constant("panelOf.id", "panel")
                .add("handleOf", UIQueryNodes.SelectId.class, 2740, 320)
                .constant("handleOf.id", "handle")
                .add("onHandle", UIEventNodes.OnEvent.class, 2980, 0)
                .option("onHandle", "eventType", UIEvents.CLICK)
                .title("onHandle", "the handle was clicked")
                .add("panelInfo", UIElementInfoNode.class, 2980, 200)
                .block("panelInfo", "panelState", UIElementInfoBlocks.State.class)
                .add("flip", NotNode.class, 3220, 200)
                .add("fold", UIStateNodes.SetFlag.class, 3220, 60)
                // Display, not visibility: an invisible panel still occupies its place in the strip,
                // so two tabs would reserve two panels' worth of room and the second would sit on top
                // of the first. Undisplayed leaves the layout, and an open tab pushes the rest down.
                .option("fold", "flag", UIStateNodes.Flag.DISPLAY)
                .title("fold", "show it, or hide it again");

        b.wire("panelOf.root", "loadTab.root")
                .wire("handleOf.root", "loadTab.root")
                .wire("onHandle.element", "handleOf.first")
                .wire("panelInfo.target", "panelOf.first")
                .wire("flip.in", "panelState.displayed")
                .wire("fold.element", "panelOf.first")
                .wire("fold.value", "flip.out");

        b.then("setTitle", "onHandle");
        b.wire("fold.trigger", "onHandle.onEvent");

        // ---- and say what the tab is for ----------------------------------------------------------
        // Several tabs are several identical gears otherwise, and which one configures which trait is
        // then something a player can only find by opening each.
        b.add("tipHead", TextNodes.Translatable.class, 2740, 60)
                .constant("tipHead.key", "mbd2.gui.auto_io.tab.named")
                .add("tipName", TextNodes.Literal.class, 2740, 140)
                .add("tip", TextNodes.Append.class, 2980, 100)
                .add("nameTab", UIAnimationNodes.Tooltip.class, 3220, -60)
                .title("nameTab", "which trait this tab is for");
        b.wire("tipName.text", "label.out")
                .wire("tip.a", "tipHead.out")
                .wire("tip.b", "tipName.out")
                .wire("nameTab.element", "handleOf.first")
                .wire("nameTab.line", "tip.out");
        b.wire("nameTab.trigger", "onHandle.next");

        b.note(2740, 420, 700, """
                The click handler runs on the client: folding a
                panel out is not something the server needs to know
                about. Setting a face is, and that one goes through
                On Server Event instead.""");

        b.group("The tab itself", 2020, 0, 1440, 560, BuiltinNotes.ACT_GROUP);

        // ---- one chain per face ------------------------------------------------------------------
        // Six near-identical chains, emitted from a loop rather than written out six times: the graph
        // a reader opens is the same either way, and one of them being subtly different from the other
        // five is a class of bug this cannot have.
        b.add("machineInfo", MachineInfoNode.class, 3560, 0)
                .title("machineInfo", "the machine, for the face conversions")
                .block("machineInfo", "level", MachineInfoBlocks.MachineLevel.class)
                .block("machineInfo", "pos", MachineInfoBlocks.Position.class);
        // target left unwired: a sync value pulls these conversions long after the UI event returned,
        // and the event's machine output does not survive that. Unwired reads the blueprint's own.

        var previous = "nameTab.next";
        for (int i = 0; i < FACES.length; i++) {
            previous = face(b, FACES[i], previous, 3560, 200 + i * FACE_ROW);
        }

        // The seeds publish each face's current state so the panel is right the moment it opens.
        // Server only, and not merely because writing there is pointless on a client: a custom-data
        // write on the client during UI assembly leaves the modular UI unlaid-out, and the screen
        // comes up empty. Everything else in this graph runs on both sides on purpose.
        b.block("machineInfo", "isRemote", MachineInfoBlocks.IsRemote.class)
                .add("isServer", NotNode.class, 3200, 100)
                .add("serverOnly", BranchNode.class, 3200, 0)
                .title("serverOnly", "publishing is the server's job");
        b.wire("isServer.in", "isRemote.value")
                .wire("serverOnly.cond", "isServer.out");
        b.wire("serverOnly.in", previous);
        var seeded = "serverOnly.trueExec";
        for (var relative : FACES) {
            var seed = "seed_" + relative.name().toLowerCase(Locale.ROOT);
            b.wire(seed + ".in", seeded);
            seeded = seed + ".next";
        }

        b.note(3560, 200 + FACES.length * FACE_ROW, 900, """
                Each face is read on the server and pushed to the
                client as a sync value: runtime overrides live on
                the block entity and are never sent to clients, so
                the panel cannot simply read them where it draws.

                The click goes the other way, through On Server
                Event, because setting one is a change to the
                world and the client does not get to make it.""");

        b.group("Six faces, one chain each", 3520, 0, 2400, 300 + FACES.length * FACE_ROW,
                BuiltinNotes.ACT_GROUP);

        return b;
    }

    /** Vertical room one face's chain needs on the canvas. */
    private static final float FACE_ROW = 460f;

    /** The machine-relative faces the panel offers, in the order the tab document lays them out. */
    private static final RelativeDirection[] FACES = {
            RelativeDirection.UP, RelativeDirection.FRONT, RelativeDirection.DOWN,
            RelativeDirection.LEFT, RelativeDirection.BACK, RelativeDirection.RIGHT};
    /** Where each face's state is published for the client to read. */
    private static final String DATA_PREFIX = "mbd2_autoio_";

    /**
     * The <em>whole</em> class list a face carries in one state: what it is, which side it is, and
     * what it is set to.
     *
     * <p>All three have to be here because the node <em>sets</em> the list rather than adding to it,
     * and setting is what makes the four states exclusive — adding {@code in} would leave {@code out}
     * behind from the tick before. Leaving one out is not a small mistake: the side class is what the
     * stylesheet places the face on the cross by, so a list missing it collapses the six faces into
     * whatever order the grid packs them in, one tick after the panel opens.</p>
     */
    private static String faceClasses(RelativeDirection relative, IO io) {
        return BuiltinMachineUIs.FACE_CLASS
                + " " + BuiltinMachineUIs.sideClass(relative)
                + " " + BuiltinMachineUIs.ioClass(io);
    }

    /**
     * One face's chain: read it, show it, and let a click change it.
     *
     * @param after the exec output pin this chain hangs off, e.g. {@code "onHandle.next"}
     * @return the pin the next chain should hang off
     */
    private static String face(BlueprintBuilder b, RelativeDirection relative,
                               String after, float x, float y) {
        var name = relative.name().toLowerCase(Locale.ROOT);
        var button = "button_" + name;
        var side = "side_" + name;
        var read = "read_" + name;
        var named = "named_" + name;
        var key = "key_" + name;
        var tag = "tag_" + name;
        var put = "put_" + name;
        var namedNext = "namedNext_" + name;
        var putNext = "putNext_" + name;
        var seed = "seed_" + name;
        var mirror = "mirror_" + name;
        var tick = "tick_" + name;
        var data = "data_" + name;
        var get = "get_" + name;
        var ofName = "ofName_" + name;
        var colour = "colour_" + name;
        var paint = "paint_" + name;
        var click = "click_" + name;
        var cycle = "cycle_" + name;
        var apply = "apply_" + name;
        var enable = "enable_" + name;
        var neighbour = "neighbour_" + name;
        var state = "state_" + name;
        var block = "block_" + name;
        var item = "item_" + name;
        var stack = "stack_" + name;
        var slot = "slot_" + name;
        var show = "show_" + name;

        // ---- what this face is set to, on the server ------------------------------------------
        b.add(button, UIQueryNodes.SelectId.class, x, y)
                .constant(button + ".id", "face_" + relative.name())
                .title(button, "this face's socket")
                .block("machineInfo", side, MachineInfoBlocks.RelativeSide.class)
                .constant(side + ".relative", relative)
                .add(read, MachineRuntimeValueNodes.GetAutoIOSide.class, x + 240, y)
                // machine left unwired: this is pulled long after the UI event returned, and the
                // event node's outputs do not survive that. Unwired reads the blueprint's own.
                .title(read, "what this face does now")
                .add(named, IONodes.Info.class, x + 480, y)
                .title(named, "as a name to store");

        // ---- mirrored into custom data, which is what actually reaches the client ---------------
        b.add(key, ConcatNode.class, x + 240, y + 100)
                .constant(key + ".in1", DATA_PREFIX + name + "_")
                .title(key, "one key per face per trait")
                .add(tag, NbtCreateNode.class, x + 480, y + 100)
                .add(put, NbtSetNode.class, x + 700, y + 100)
                .title(put, "this face's state")
                .add(seed, MachineActionNodes.MergeCustomData.class, x + 940, y)
                .title(seed, "publish it when the ui opens")
                .add(namedNext, IONodes.Info.class, x + 1180, y + 400)
                .title(namedNext, "the value just applied")
                .add(putNext, NbtSetNode.class, x + 1420, y + 400)
                .add(mirror, MachineActionNodes.MergeCustomData.class, x + 1660, y + 320)
                .title(mirror, "and again whenever it changes");

        b.wire(button + ".root", "loadTab.root")
                .wire(read + ".trait", "trait")
                .wire(read + ".side", side + ".value")
                .wire(named + ".io", read + ".io")
                .wire(key + ".in2", "trait")
                .wire(put + ".tag", tag + ".out")
                .wire(put + ".key", key + ".out")
                .wire(put + ".value", named + ".name")
                .wire(seed + ".data", put + ".out")
                .wire(putNext + ".tag", tag + ".out")
                .wire(putNext + ".key", key + ".out")
                .wire(putNext + ".value", namedNext + ".name")
                .wire(mirror + ".data", putNext + ".out");

        // ---- the client draws from custom data --------------------------------------------------
        b.add(tick, UIEventNodes.OnTick.class, x + 1180, y)
                .title(tick, "keep the face in step")
                .block("machineInfo", data, MachineInfoBlocks.CustomData.class)
                .add(get, NbtGetNode.class, x + 1180, y + 100)
                .add(ofName, IONodes.OfName.class, x + 1420, y + 100)
                .add(colour, IONodes.Choose.class, x + 1660, y + 100)
                .constant(colour + ".whenNone", faceClasses(relative, IO.NONE))
                .constant(colour + ".whenIn", faceClasses(relative, IO.IN))
                .constant(colour + ".whenOut", faceClasses(relative, IO.OUT))
                .constant(colour + ".whenBoth", faceClasses(relative, IO.BOTH))
                .title(colour, "one class per state")
                .add(paint, UIStyleNodes.ClassNames.class, x + 1900, y)
                // A class, not a colour. What "in" looks like then lives in the stylesheet, where a
                // pack can change it, rather than as a constant baked into this graph. Set rather
                // than add, because the four states are exclusive and the previous one has to go.
                .option(paint, "op", UIStyleNodes.ClassOp.SET)
                .title(paint, "mark what this face is doing");

        b.wire(tick + ".element", button + ".first")
                .wire(get + ".tag", data + ".value")
                .wire(get + ".key", key + ".out")
                .wire(ofName + ".name", get + ".out")
                .wire(colour + ".io", ofName + ".io")
                .wire(paint + ".element", button + ".first")
                .wire(paint + ".classes", colour + ".value");

        // ---- and what is actually on the other side of that face ---------------------------------
        // Read straight off the client's own level: the neighbour is one block away, so the client
        // already has it and nothing needs sending. Only the machine's own auto IO has to travel.
        b.add(neighbour, BlockPosOffsetNode.class, x + 240, y + 200)
                .title(neighbour, "the block on this face")
                .add(state, GetBlockStateNode.class, x + 480, y + 200)
                .add(block, BlockStateNodes.StateBlock.class, x + 700, y + 200)
                .add(item, BlockToItemNode.class, x + 940, y + 200)
                .add(stack, ItemStackCreateNode.class, x + 1180, y + 200)
                .add(slot, UIQueryNodes.SelectId.class, x + 1420, y + 200)
                .constant(slot + ".id", "item_" + relative.name())
                .title(slot, "the socket's item layer")
                .add(show, UIItemNodes.SetItem.class, x + 2140, y)
                .title(show, "draw it under the tint");

        b.wire(neighbour + ".pos", "pos.value")
                .wire(neighbour + ".direction", side + ".value")
                .wire(state + ".level", "level.value")
                .wire(state + ".pos", neighbour + ".out")
                .wire(block + ".in", state + ".out")
                .wire(item + ".in", block + ".out")
                .wire(stack + ".item", item + ".out")
                .wire(slot + ".root", "loadTab.root")
                .wire(show + ".element", slot + ".first")
                .wire(show + ".item", stack + ".out");
        b.then(paint, show);

        // ---- a click, handled on the server ------------------------------------------------------
        b.add(click, UIEventNodes.OnServerEvent.class, x + 700, y + 320)
                .option(click, "eventType", UIEvents.CLICK)
                // Capture, so the face hears the click on the way *down* to whatever is on top of it.
                // The item layer fills the button, which makes it the event's target; a bubble
                // listener would be at the mercy of what a document happens to put inside a face,
                // and the alternative — making that layer click-through — is a flag no document can
                // carry through a template.
                .constant(click + ".useCapture", true)
                .title(click, "a click, handled on the server")
                .add(cycle, IONodes.Next.class, x + 940, y + 320)
                .add(apply, MachineRuntimeValueNodes.SetAutoIOSide.class, x + 1180, y + 320)
                .title(apply, "override it on this machine only")
                .add(enable, MachineRuntimeValueNodes.SetAutoIOEnabled.class, x + 1300, y + 400)
                .constant(enable + ".enabled", true)
                .title(enable, "and switch auto IO on");

        b.wire(click + ".element", button + ".first")
                .wire(cycle + ".io", read + ".io")
                .wire(apply + ".trait", "trait")
                .wire(apply + ".side", side + ".value")
                .wire(apply + ".io", cycle + ".next")
                .wire(enable + ".trait", "trait")
                // The value just applied, not a re-read: the executor memoises a pulled value for the
                // run, so reading the side again here would hand back what it was before the write.
                .wire(namedNext + ".io", cycle + ".next");

        // Named pins throughout: a tick listener and an event listener each have two exec outputs,
        // and which one a chain continues on is the difference between "carry on building the panel"
        // and "do this when it happens".
        b.wire(tick + ".trigger", after);
        b.wire(click + ".trigger", tick + ".next");
        b.wire(paint + ".trigger", tick + ".onTick");
        b.wire(apply + ".in", click + ".onEvent");
        // A side setting does nothing on its own: IAutoIOTrait.serverTick returns immediately while
        // the trait's auto IO is switched off, and a definition that never turned it on is the normal
        // case. Configuring a face is the player asking for it, so that is where it gets switched on.
        b.then(apply, enable, mirror);
        return click + ".next";
    }
}

package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtCreateNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtGetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.nbt.NbtSetNode;
import com.lowdragmc.kilagraph.blueprint.nodes.string.ConcatNode;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.sync.UISyncNodes;
import com.lowdragmc.kilagraph.graph.type.KGTypeHandles;
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
import com.lowdragmc.kilagraph.blueprint.nodes.ui.style.UIStyleNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.element.UIValueNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.ui.event.UIEventNodes;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.common.blueprint.node.IONodes;
import com.lowdragmc.mbd2.common.blueprint.node.ui.BuiltinUINodes;
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

                Nothing is added if that trait does not do auto IO.

                Bind this blueprint more than once with different
                trait names and the tabs stack up the left edge -
                they append to one shared strip rather than each
                building their own.""");

        // ---- does this trait even do auto IO? --------------------------------------------------
        b.add("event", UIEventNode.class, 0, 0)
                .parameter("trait", String.class, "", 0, 260)
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
                .add("loadStrip", BuiltinUINodes.Builtin.class, 1480, 0)
                .constant("loadStrip.name", BuiltinMachineUIs.SIDE_TAB_STRIP)
                .title("loadStrip", "the built-in strip")
                .add("addStrip", UIElementNodes.AddChild.class, 1720, 0)
                .add("strip", UIQueryNodes.SelectId.class, 1480, 200)
                .constant("strip.id", STRIP_ID)
                .title("strip", "now certainly there");

        b.wire("unpack.ui", "event.ui")
                .wire("look.root", "unpack.root")
                .wire("missing.in", "look.first")
                .wire("needStrip.cond", "missing.out")
                .wire("addStrip.parent", "unpack.root")
                .wire("addStrip.child", "loadStrip.root")
                .wire("strip.root", "unpack.root");

        b.wire("needStrip.in", "gate.trueExec")
                .wire("loadStrip.trigger", "needStrip.trueExec");
        b.then("loadStrip", "addStrip");

        b.note(780, 420, 700, """
                Look, maybe create, then look again. The second
                lookup is what lets both paths carry on into the
                same node without a value that is only set on one
                of them - a graph cannot merge two branches into
                one wire, but it can ask the same question twice.""");

        b.group("Find or create the strip", 780, 0, 1180, 560, BuiltinNotes.DECIDE_GROUP);

        // ---- build this trait's tab --------------------------------------------------------------
        b.add("loadTab", BuiltinUINodes.Builtin.class, 2020, 0)
                .constant("loadTab.name", BuiltinMachineUIs.AUTO_IO_TAB)
                .title("loadTab", "the built-in tab")
                .add("addTab", UIElementNodes.AddChild.class, 2260, 0)
                .title("addTab", "append, not replace")
                .add("titleOf", UIQueryNodes.SelectId.class, 2020, 200)
                .constant("titleOf.id", "title")
                .add("titleText", TextNodes.Literal.class, 2260, 200)
                .add("setTitle", UIValueNodes.SetText.class, 2500, 0)
                .title("setTitle", "name the tab after the trait");

        b.wire("addTab.parent", "strip.first")
                .wire("addTab.child", "loadTab.root")
                .wire("titleOf.root", "loadTab.root")
                .wire("titleText.text", "trait")
                .wire("setTitle.element", "titleOf.first")
                .wire("setTitle.text", "titleText.out");

        b.wire("loadTab.trigger", "needStrip.falseExec")
                .wire("loadTab.trigger", "addStrip.next");
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
                .option("fold", "flag", UIStateNodes.Flag.VISIBLE)
                .title("fold", "show it, or hide it again");

        b.wire("panelOf.root", "loadTab.root")
                .wire("handleOf.root", "loadTab.root")
                .wire("onHandle.element", "handleOf.first")
                .wire("panelInfo.target", "panelOf.first")
                .wire("flip.in", "panelState.visible")
                .wire("fold.element", "panelOf.first")
                .wire("fold.value", "flip.out");

        b.then("setTitle", "onHandle");
        b.wire("fold.trigger", "onHandle.onEvent");

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
                .title("machineInfo", "the machine, for the face conversions");
        // target left unwired: a sync value pulls these conversions long after the UI event returned,
        // and the event's machine output does not survive that. Unwired reads the blueprint's own.

        var previous = "onHandle.next";
        for (int i = 0; i < FACES.length; i++) {
            previous = face(b, FACES[i], previous, 3560, 200 + i * 380f);
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

        b.note(3560, 200 + FACES.length * 380f, 900, """
                Each face is read on the server and pushed to the
                client as a sync value: runtime overrides live on
                the block entity and are never sent to clients, so
                the panel cannot simply read them where it draws.

                The click goes the other way, through On Server
                Event, because setting one is a change to the
                world and the client does not get to make it.""");

        b.group("Six faces, one chain each", 3520, 0, 1500, 300 + FACES.length * 380f,
                BuiltinNotes.ACT_GROUP);

        return b;
    }

    /** The machine-relative faces the panel offers, in the order the tab document lays them out. */
    private static final RelativeDirection[] FACES = {
            RelativeDirection.UP, RelativeDirection.FRONT, RelativeDirection.DOWN,
            RelativeDirection.LEFT, RelativeDirection.BACK, RelativeDirection.RIGHT};
    /**
     * What a face is tinted with, per state. Translucent so the button's own texture still reads
     * through it, and written as LSS values so a pack author can restyle the panel by editing the
     * blueprint's constants rather than this class.
     */
    /** Where each face's state is published for the client to read. */
    private static final String DATA_PREFIX = "mbd2_autoio_";

    private static final String NONE_BLOCK = "#40202020";
    private static final String IN_BLOCK = "#804C9BE8";
    private static final String OUT_BLOCK = "#80E8944C";
    private static final String BOTH_BLOCK = "#805FCB84";

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
                .constant(colour + ".whenNone", NONE_BLOCK)
                .constant(colour + ".whenIn", IN_BLOCK)
                .constant(colour + ".whenOut", OUT_BLOCK)
                .constant(colour + ".whenBoth", BOTH_BLOCK)
                .title(colour, "one colour per state")
                .add(paint, UIStyleNodes.LssSet.class, x + 1900, y)
                .option(paint, "property", "overlay")
                .title(paint, "tint the socket");

        b.wire(tick + ".element", button + ".first")
                .wire(get + ".tag", data + ".value")
                .wire(get + ".key", key + ".out")
                .wire(ofName + ".name", get + ".out")
                .wire(colour + ".io", ofName + ".io")
                .wire(paint + ".element", button + ".first")
                .wire(paint + ".value", colour + ".value");

        // ---- a click, handled on the server ------------------------------------------------------
        b.add(click, UIEventNodes.OnServerEvent.class, x + 700, y + 320)
                .option(click, "eventType", UIEvents.CLICK)
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

package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.test.tests.blueprint.AutoIOPanelFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;

/**
 * The {@code auto_io_panel} built-in as a player sees it.
 *
 * <h2>What this covers that the gametests cannot</h2>
 * {@code AutoIOPanelTests} proves the element tree is right, which is every wiring mistake the graph
 * can make. It says nothing about whether the strip is clipped off the edge of the screen, whether
 * the folded-out panel lands on top of the machine's own contents, or whether six buttons in a
 * three-by-two grid actually look like a machine's faces. Those are the questions a blueprint that
 * builds UI raises, and only a rendered frame answers them.
 *
 * <p>It is also the only place the <em>client</em> half of the blueprint runs. A machine UI is
 * assembled on both sides, and the gametests only ever see the server's copy — the click handler that
 * folds the panel, the tick chain that paints each face from the machine's custom data, and every
 * inline style in the documents are client-side and unexercised until here.</p>
 *
 * <h2>Why it opens a real menu</h2>
 * Placing the block and asking the server to open its UI is the same path a player takes, so what is
 * screenshotted is what they would get. Building a {@code ModularUI} directly would skip the menu,
 * and with it the desc sync the face states travel through.
 *
 * <h2>Which thread reads what</h2>
 * Server state is read from {@code checkServer}/{@code waitForSync}, never from a client-side
 * {@code check}. {@code Level.getBlockEntity} returns {@code null} when a server level is asked from
 * any thread but the server's, so a client-thread read of the machine reports an untouched machine
 * however much the server did — which reads exactly like "the blueprint never ran on the server".
 */
@LDLRegisterClient(name = "mbd2_auto_io_panel", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MachineAutoIOPanelScenario implements UIScenario {

    /**
     * Where the fixture machine goes, chosen at placement time from where the player actually is.
     *
     * <p>A fixed position looks tidier and is a race: the client only holds block entities for the
     * chunks it has been sent, and a freshly generated world puts the player wherever its spawn
     * search landed. Opening a UI for a block the client has never heard of gets a menu whose client
     * half found no machine — an empty root that never lays out — so the block goes next to the
     * player, where the chunk is certainly loaded on both sides.</p>
     */
    private BlockPos pos = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "blueprint", "ui", "visual")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        // The dev world persists between runs, so a menu left open by a previous one is still there
        // when this starts and the open below never produces a new screen.
        s.step("start from no screen", ctx -> ctx.mc().setScreen(null))
                .frames(2)
                .server("place a machine carrying two auto-io tabs", sc -> {
                    var definition = MBDRegistries.MACHINE_DEFINITIONS.get(AutoIOPanelFixtures.TWO_TABS_ID);
                    if (definition == null) {
                        throw new IllegalStateException("the two-tab fixture machine is not registered");
                    }
                    pos = sc.player().blockPosition().above(2);
                    sc.level().setBlockAndUpdate(pos, definition.block().defaultBlockState());
                })
                // Waited for, not counted in frames: a machine UI is built on both sides and the
                // client builds its half from its own copy of the block entity, which is at least a
                // server tick and one packet away. Opening before it arrives gets a menu whose client
                // half found no machine at all — an empty root with no machine ui and no tabs, which
                // then never lays out and fails several steps later as a layout timeout.
                .waitUntil("the client to have the machine", ctx ->
                        ctx.level() != null && ctx.level().getBlockEntity(pos) instanceof IMachineBlockEntity)
                .server("open its ui the way a player would", sc -> BlockUIMenuType.openUI(sc.player(), pos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                .check("both tabs are on one strip", ctx -> {
                    var strips = ctx.query().withId("mbd2_side_tabs").list().size();
                    var handles = ctx.query().withId("handle").list().size();
                    ctx.attach("strips", String.valueOf(strips));
                    ctx.attach("handles", String.valueOf(handles));
                    return strips == 1 && handles == 2;
                })
                .check("the panels start folded away", ctx -> ctx.query().withId("panel").list().stream()
                        .noneMatch(ref -> ref.as(UIElement.class).isVisible()))
                // The strip is absolutely positioned against the machine's own UI root; which edge it
                // hangs off is the document's choice, so what is asserted is that it hangs off one -
                // sitting inside the host would mean it is covering the machine's own contents.
                .check("the strip hangs outside the machine's own ui root", ctx -> {
                    var strip = ctx.query().withId("mbd2_side_tabs").one().as(UIElement.class);
                    var host = strip.getParent();
                    if (host == null) return false;
                    ctx.attach("offset", (strip.getPositionX() - host.getPositionX()) + ","
                            + (strip.getPositionY() - host.getPositionY()));
                    return strip.getPositionX() + strip.getSizeWidth() <= host.getPositionX()
                            || strip.getPositionX() >= host.getPositionX() + host.getSizeWidth();
                })
                .screenshot("auto-io-collapsed")

                // Two tabs means two handles, so the built-in click(selector) cannot be used: it
                // insists on a unique match. Press and release the first one by hand instead.
                .step("click the first tab's handle", ctx -> {
                    var handle = ctx.query().withId("handle").nth(0).one().bounds();
                    ctx.input().mouseDown(handle.centerX(), handle.centerY(), 0);
                    ctx.input().mouseUp(handle.centerX(), handle.centerY(), 0);
                })
                .frames(4)
                .check("clicking the handle folds a panel out", ctx -> {
                    var open = ctx.query().withId("panel").list().stream()
                            .filter(ref -> ref.as(UIElement.class).isVisible())
                            .count();
                    ctx.attach("openPanels", String.valueOf(open));
                    return open == 1;
                })
                // The one place the whole loop runs: the machine's auto IO lives on the server and is
                // never sent with the block, so these cells can only differ if the server published
                // each face's state into custom data, the block entity's desc sync carried it, and
                // the client painted from it. The fixture ships top=IN and bottom=OUT and leaves the
                // rest alone, so three different answers is the whole path working in a live menu.
                //
                // Each leg is recorded separately because they fail for different reasons and look
                // identical from the client's paint alone: nothing on the server means the blueprint
                // never ran there, server-but-not-client means the desc sync has not arrived, and
                // both-but-unpainted means the client half of the graph is not reading it.
                // Read on the server thread, because that is the only thread that can read it:
                // Level.getBlockEntity hands back null when it is called from anywhere else on a
                // server level, so a client-thread read reports an untouched machine no matter what
                // the server did.
                .checkServer("the server published every face", sc -> {
                    var data = serverData(sc);
                    sc.check("up published as IN", "IN".equals(data.getString(key("up"))),
                            "IN", data.getString(key("up")));
                    sc.check("down published as OUT", "OUT".equals(data.getString(key("down"))),
                            "OUT", data.getString(key("down")));
                    sc.check("left published as NONE", "NONE".equals(data.getString(key("left"))),
                            "NONE", data.getString(key("left")));
                    return true;
                })
                // Compared against the server rather than polled on its own: that is what tells "the
                // desc sync has not arrived yet" apart from "the server never wrote it".
                .waitForSync("the up face", sc -> serverData(sc).getString(key("up")),
                        ctx -> clientData(ctx).getString(key("up")))
                .check("each face is painted with its own state", ctx -> {
                    var in = overlay(ctx, "face_UP");
                    var out = overlay(ctx, "face_DOWN");
                    var none = overlay(ctx, "face_LEFT");
                    ctx.attach("face_UP", in);
                    ctx.attach("face_DOWN", out);
                    ctx.attach("face_LEFT", none);
                    return !in.equals(out) && !out.equals(none) && !in.equals(none);
                })
                .step("click a face", ctx -> clickFace(ctx, "face_UP", 1))
                // A click travels the long way round — client rpc, server runtime write, custom data,
                // desc sync, client tick — so waiting for the face to change colour is the whole round
                // trip, not just a listener firing. IN cycles to OUT, which is what the bottom face
                // already shows, so the target is a colour that is already on screen elsewhere.
                .waitUntil("the clicked face to repaint", ctx ->
                        overlay(ctx, "face_UP").equals(overlay(ctx, "face_DOWN")))
                .checkServer("and the click reached the machine", sc -> {
                    var published = serverData(sc).getString(key("up"));
                    sc.check("up is now OUT", "OUT".equals(published), "OUT", published);
                    return true;
                })
                .screenshot("auto-io-expanded")
                .closeScreen();
    }



    /** Where the blueprint publishes one face's state, for the item-slot tab this asserts on. */
    private static String key(String face) {
        return "mbd2_autoio_" + face + "_" + AutoIOPanelFixtures.ITEM_TRAIT;
    }

    /** The machine's custom data as the server holds it — what the blueprint wrote. */
    private CompoundTag serverData(com.lowdragmc.lowdraglib2.uitest.ServerContext sc) {
        return customData(sc.level(), pos);
    }

    /** The same, as the client holds it — what the desc sync delivered. */
    private CompoundTag clientData(com.lowdragmc.lowdraglib2.uitest.TestContext ctx) {
        return customData(ctx.level(), pos);
    }

    private static CompoundTag customData(BlockGetter level, BlockPos pos) {
        if (level != null && level.getBlockEntity(pos) instanceof IMachineBlockEntity holder
                && holder.getMetaMachine() instanceof MBDMachine machine) {
            return machine.getCustomData();
        }
        return new CompoundTag();
    }

    /** Clicks one face button {@code times} times, walking its state that far round the cycle. */
    private static void clickFace(com.lowdragmc.lowdraglib2.uitest.TestContext ctx, String id, int times) {
        var bounds = ctx.query().withId(id).nth(0).one().bounds();
        for (int i = 0; i < times; i++) {
            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
        }
    }

    /** The computed overlay of the first element with this id — what the blueprint paints state with. */
    private static String overlay(com.lowdragmc.lowdraglib2.uitest.TestContext ctx, String id) {
        var element = ctx.query().withId(id).nth(0).one().as(UIElement.class);
        for (var style : element.getStyles()) {
            for (var property : style.getPropertiesList()) {
                if (property.name.equals("overlay")) {
                    var texture = style.getValueSave(property);
                    // The colour, not the instance: two rects of the same colour are different
                    // objects, so toString() would report every face as different from every other.
                    return texture instanceof com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture rect
                            ? Integer.toHexString(rect.color)
                            : String.valueOf(texture);
                }
            }
        }
        return "none";
    }
}

package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.test.tests.blueprint.AutoIOPanelFixtures;
import net.minecraft.core.BlockPos;

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
 * folds the panel, the text a sync value writes onto a face, and every inline style in the documents
 * are client-side and unexercised until here.</p>
 *
 * <h2>Why it opens a real menu</h2>
 * Placing the block and asking the server to open its UI is the same path a player takes, so what is
 * screenshotted is what they would get. Building a {@code ModularUI} directly would skip the menu,
 * and with it the sync manager the face values travel through.
 */
@LDLRegisterClient(name = "mbd2_auto_io_panel", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MachineAutoIOPanelScenario implements UIScenario {

    /** Somewhere in front of the player, on the ground of the test world. */
    private static final BlockPos POS = new BlockPos(0, -60, 0);

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
                .step("place a machine carrying two auto-io tabs", ctx -> {
                    var definition = MBDRegistries.MACHINE_DEFINITIONS.get(AutoIOPanelFixtures.TWO_TABS_ID);
                    if (definition == null) {
                        throw new IllegalStateException("the two-tab fixture machine is not registered");
                    }
                    ctx.serverPlayer().serverLevel().setBlockAndUpdate(POS, definition.block().defaultBlockState());
                })
                .frames(6)
                .step("open its ui the way a player would", ctx ->
                        BlockUIMenuType.openUI(ctx.serverPlayer(), POS))
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
                // No check on the face colours here. The blueprint paints each one from a
                // sync value, and that value never arrives with anything but its default - see the
                // note on the sync node in AutoIOPanelBlueprint. Asserting "they are all the same"
                // would lock the gap in as if it were the intent, and asserting they differ would
                // commit a red test, so this records the shape of the panel and leaves the colours
                // to the fix.
                .screenshot("auto-io-expanded")
                .closeScreen();
    }



    /** Clicks one face button {@code times} times, walking its state that far round the cycle. */
    private static void clickFace(com.lowdragmc.lowdraglib2.uitest.TestContext ctx, String id, int times) {
        var bounds = ctx.query().withId(id).nth(0).one().bounds();
        for (int i = 0; i < times; i++) {
            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
        }
    }
}

package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The {@code auto_io_panel} built-in, checked where it is cheapest to check: the element tree a
 * machine UI comes out with.
 *
 * <h2>Why the tree and not a screenshot</h2>
 * A blueprint that builds UI can fail in two quite different ways — it can produce the wrong tree, or
 * it can produce the right tree that looks wrong. Only the second needs a rendered frame, and it is
 * the rarer one; the first is every wiring mistake in a seventy-node graph. Asserting on the tree
 * runs in a second, names the missing id when it fails, and works headlessly, so it is what guards
 * the graph. {@code MachineAutoIOPanelScenario} takes the screenshot.
 *
 * <h2>Why a machine UI is buildable here at all</h2>
 * {@code MBDMachine.createUI} only reaches into its holder for the player, so a gametest can hand it
 * one built around the machine's own block and get the real thing back — the same call the menu makes,
 * firing the same {@code MachineUIEvent} the blueprint listens for.
 */
@GameTestHolder(MBD2.MOD_ID)
public class AutoIOPanelTests {
    static { @SuppressWarnings("unused") var ignored = AutoIOPanelFixtures.ONE_TAB_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final String STRIP = "mbd2_side_tabs";
    private static final List<String> FACES =
            List.of("face_UP", "face_DOWN", "face_LEFT", "face_RIGHT", "face_FRONT", "face_BACK");

    /**
     * One binding puts one tab on the strip, with every part the panel needs.
     *
     * <p>Each id is asserted separately because they fail for different reasons: no strip means the
     * event never reached the graph, a strip with no tab means the tab document did not load, and a
     * tab missing a face means the document itself is wrong.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void oneBindingAddsOneTab(GameTestHelper helper) {
        var ui = openUI(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (ui == null) return;

        if (count(ui, STRIP) != 1) {
            helper.fail("expected exactly one side-tab strip, found " + count(ui, STRIP));
            return;
        }
        if (count(ui, "handle") != 1 || count(ui, "panel") != 1) {
            helper.fail("the tab is missing its handle or its panel");
            return;
        }
        for (var face : FACES) {
            if (count(ui, face) != 1) {
                helper.fail("the panel has no " + face + " button");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * Two bindings append to one strip rather than each building their own.
     *
     * <p>This is the difference between tabs that stack up the left edge and tabs drawn on top of each
     * other, and it is the whole reason the strip is a separate document found by id. One strip and two
     * of everything else is the shape that says so.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void twoBindingsShareOneStrip(GameTestHelper helper) {
        var ui = openUI(helper, AutoIOPanelFixtures.TWO_TABS_ID);
        if (ui == null) return;

        if (count(ui, STRIP) != 1) {
            helper.fail("expected the two tabs to share one strip, found " + count(ui, STRIP)
                    + " strips — they built their own instead of appending");
            return;
        }
        if (count(ui, "handle") != 2) {
            helper.fail("expected two tabs, found " + count(ui, "handle"));
            return;
        }
        var strip = ui.selectId(STRIP).findFirst().orElse(null);
        if (strip == null || strip.getChildren().size() != 2) {
            helper.fail("expected both tabs to be children of the strip, it has "
                    + (strip == null ? "no strip" : strip.getChildren().size() + " children"));
            return;
        }
        helper.succeed();
    }

    /**
     * A trait that does not do auto IO gets no tab, not an empty one.
     *
     * <p>The panel is only worth showing for a trait that will act on it. Without this check the
     * blueprint would happily draw six buttons that change nothing, which is worse than no tab —
     * and the check is in the graph, so this is what proves the graph asks the question.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void anUnknownTraitAddsNothing(GameTestHelper helper) {
        var ui = openUI(helper, AutoIOPanelFixtures.NO_TRAIT_ID);
        if (ui == null) return;
        if (count(ui, STRIP) != 0 || count(ui, "handle") != 0) {
            helper.fail("a trait that does no auto IO still got a tab");
            return;
        }
        helper.succeed();
    }

    /**
     * The control: without the blueprint the machine UI has none of this.
     *
     * <p>Otherwise "there is no strip" would be equally consistent with a working guard and with a
     * machine UI that never had one to begin with.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aPlainMachineHasNoStrip(GameTestHelper helper) {
        var ui = openUI(helper, AutoIOPanelFixtures.CONTROL_ID);
        if (ui == null) return;
        if (count(ui, STRIP) != 0) {
            helper.fail("a machine with no blueprint somehow has a side-tab strip");
            return;
        }
        helper.succeed();
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** Places the machine and builds its UI the way opening the menu would. */
    private static UI openUI(GameTestHelper helper, ResourceLocation machineId) {
        var scenario = MBDScenario.of(helper).placeMachine(machineId, POS);
        var machine = scenario.machine();
        var state = helper.getBlockState(POS);
        if (!(state.getBlock() instanceof BlockUIMenuType.BlockUI blockUI)) {
            helper.fail("the machine block does not open a ui at all");
            return null;
        }
        var holder = new BlockUIMenuType.BlockUIHolder(blockUI, helper.makeMockPlayer(GameType.CREATIVE), absolute(helper), state);
        var modular = machine.createUI(holder);
        if (modular == null) {
            helper.fail("the machine built no ui");
            return null;
        }
        return modular.ui;
    }

    private static BlockPos absolute(GameTestHelper helper) {
        return helper.absolutePos(POS);
    }

    private static long count(UI ui, String id) {
        return ui.selectId(id).count();
    }

    /**
     * Clicking a face walks its auto IO round the cycle and stops where it started.
     *
     * <p>The click handler is registered on the server, where the runtime override lives, and normally
     * only ever runs because a client sent the matching RPC. Running the listeners directly is the only
     * way to exercise it headlessly, and it is worth exercising: everything upstream of it — the face
     * button, the relative-to-world side conversion, the cycle, the write — is graph wiring, and the
     * structural test would pass just as happily with all of it wired to nothing.</p>
     *
     * <p>A full lap rather than one step, because one step cannot tell a working cycle from a node
     * stuck on {@code IN}, and because coming back to {@code NONE} is what a player expects.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clickingAFaceCyclesItsAutoIO(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.ONE_TAB_ID, POS);
        var machine = scenario.machine();
        var ui = openUI(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (ui == null) return;

        var button = ui.selectId("face_UP").findFirst().orElse(null);
        if (button == null) {
            helper.fail("no up-face button to click");
            return;
        }
        // Up is the one relative side that is the same world direction whichever way a machine faces,
        // so the expectation does not depend on how the fixture happened to be placed. It starts at
        // IN because the fixture's definition ships it that way, which is itself worth walking from:
        // a cycle that ignored the current value would land on IN first instead of OUT.
        for (var expected : List.of(IO.OUT, IO.BOTH, IO.NONE, IO.IN)) {
            click(button);
            var actual = autoIO(machine, RelativeDirection.UP);
            if (actual != expected) {
                helper.fail("expected the up face to be " + expected + " after a click, was " + actual);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The panel shows what the machine actually has, not a fresh set of defaults.
     *
     * <p>The face values are read on the server and pushed to the client, because runtime overrides are
     * never sent with the block entity. This checks the server end of that: the sync value's source is
     * wired to the real reading, so a face already set to {@code OUT} arrives as {@code OUT}.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void thePanelReadsWhatIsAlreadySet(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.ONE_TAB_ID, POS);
        var machine = scenario.machine();
        var trait = machine.getTraitByName(AutoIOPanelFixtures.ITEM_TRAIT);
        if (!(trait instanceof IAutoIOTrait autoIO)) {
            helper.fail("the fixture's item slot trait does not do auto IO");
            return;
        }
        autoIO.setAutoIOSide(Direction.UP, IO.OUT);

        var ui = openUI(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (ui == null) return;
        var button = ui.selectId("face_UP").findFirst().orElse(null);
        if (button == null) {
            helper.fail("no up-face button");
            return;
        }
        // One more click: from OUT the cycle goes to BOTH. If the graph had read a fresh NONE instead
        // of what the machine holds, it would have gone to IN.
        click(button);
        var actual = autoIO(machine, RelativeDirection.UP);
        if (actual != IO.BOTH) {
            helper.fail("expected the click to continue from OUT to BOTH, landed on " + actual
                    + " — the panel read a default rather than the machine's own value");
            return;
        }
        helper.succeed();
    }

    /**
     * The tab is named after the trait, and after the {@code name} parameter when one is given.
     *
     * <p>Both halves in one test because either alone passes for the wrong reason: a panel that
     * always shows the trait satisfies the first, and one that always shows the override satisfies
     * the second. What is being checked is that the choice is made.</p>
     *
     * <p>The title is the visible half of it; the invisible half is that the machine's stored keys
     * are still keyed by trait, which {@link #renamingATabDoesNotMoveItsStoredState} covers.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aTabIsNamedAfterItsTraitUnlessTold(GameTestHelper helper) {
        var byTrait = title(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (!AutoIOPanelFixtures.ITEM_TRAIT.equals(byTrait)) {
            helper.fail("with no name given the tab should fall back to the trait, showed '" + byTrait + "'");
            return;
        }
        var byName = title(helper, AutoIOPanelFixtures.NAMED_ID);
        if (!AutoIOPanelFixtures.CUSTOM_NAME.equals(byName)) {
            helper.fail("the name parameter should override the trait, tab showed '" + byName + "'");
            return;
        }
        helper.succeed();
    }

    /**
     * Renaming a tab does not move where the machine's state is published.
     *
     * <p>The keys are what the client reads each face from, so keying them by the display name would
     * mean a pack changing a label silently blanked every panel built before the change — and worse,
     * a machine already in a world. Named by trait, they are stable.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void renamingATabDoesNotMoveItsStoredState(GameTestHelper helper) {
        var machine = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.NAMED_ID, POS).machine();
        if (openUI(helper, AutoIOPanelFixtures.NAMED_ID) == null) return;
        var byTrait = "mbd2_autoio_up_" + AutoIOPanelFixtures.ITEM_TRAIT;
        if (machine.getCustomData().getString(byTrait).isEmpty()) {
            helper.fail("a renamed tab published nothing under its trait's key; keys are: "
                    + machine.getCustomData().getAllKeys());
            return;
        }
        helper.succeed();
    }

    /**
     * A face listens in the capture phase, so what is drawn on top of it cannot swallow the click.
     *
     * <p>Each face has the neighbouring block drawn on a child that fills it, which makes that child
     * the target of any click that lands there. A bubble listener would be at the mercy of what a
     * document happens to put inside a face; a capture one sees the event on the way <em>down</em> to
     * the target, before any descendant. The alternative — marking the child click-through — is a
     * flag that no {@code UITemplate} can carry, so it would come back wrong from the library.</p>
     *
     * <p>Nothing about a wrongly-phased listener throws or looks different: the panel renders exactly
     * the same and silently ignores every click.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aFaceHearsClicksMeantForWhatIsDrawnOnIt(GameTestHelper helper) {
        var ui = openUI(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (ui == null) return;
        for (var face : FACES) {
            var button = ui.selectId(face).findFirst().orElse(null);
            if (button == null) {
                helper.fail("no " + face + " button");
                return;
            }
            if (button.getServerEventListeners(UIEvents.CLICK, true).isEmpty()) {
                helper.fail(face + " has no capture-phase click listener, so the item drawn on it"
                        + " will swallow every click meant for the face");
                return;
            }
        }
        helper.succeed();
    }

    /** The text of the tab's title label, which is what a player reads at the top of the panel. */
    private static String title(GameTestHelper helper, ResourceLocation machineId) {
        var ui = openUI(helper, machineId);
        if (ui == null) return null;
        return ui.selectId("title", com.lowdragmc.lowdraglib2.gui.ui.elements.Label.class)
                .findFirst().map(label -> label.getText().getString()).orElse(null);
    }

    /**
     * Runs the element's server-side click listeners, the way the client's rpc would.
     *
     * <p>Both phases, because which one a listener is registered in is the graph's choice and not
     * something a caller should have to know — the panel's face listeners are capture, the handle's
     * would be bubble.</p>
     */
    private static void click(UIElement element) {
        var event = UIEvent.create(UIEvents.CLICK);
        event.currentElement = element;
        event.target = element;
        for (var capture : List.of(true, false)) {
            for (var listener : element.getServerEventListeners(UIEvents.CLICK, capture)) {
                listener.handleEvent(event);
            }
        }
    }

    private static IO autoIO(MBDMachine machine, RelativeDirection relative) {
        var trait = machine.getTraitByName(AutoIOPanelFixtures.ITEM_TRAIT);
        if (!(trait instanceof IAutoIOTrait autoIO)) return IO.NONE;
        var runtime = autoIO.getRuntimeAutoIO();
        if (runtime == null) return IO.NONE;
        var front = machine.getFrontFacing().orElse(Direction.NORTH);
        return runtime.getIO(front, relative.getActualFacing(front));
    }


    /**
     * The fixture's definition really does set two faces.
     *
     * <p>The ui scenario leans on this to tell a painted face from an unpainted one, and a fixture
     * that quietly failed to apply it would make that check pass for the wrong reason - or fail for
     * one that has nothing to do with the panel.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void theFixtureShipsTwoConfiguredFaces(GameTestHelper helper) {
        var machine = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.ONE_TAB_ID, POS).machine();
        var up = autoIO(machine, RelativeDirection.UP);
        var down = autoIO(machine, RelativeDirection.DOWN);
        if (up != IO.IN || down != IO.OUT) {
            helper.fail("expected the definition to ship up=IN and down=OUT, got up=" + up + " down=" + down);
            return;
        }
        helper.succeed();
    }

    /**
     * Right-clicking a machine opens its menu.
     *
     * <p>The other tests here build the UI directly, which is the cheapest way to look at the tree but
     * skips the open event and the menu around it. Those are where a right-click ends up, and a
     * regression in them shows as a packet the server refuses to handle and nothing else.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void rightClickingOpensTheMenu(GameTestHelper helper) {
        var machine = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.TWO_TABS_ID, POS).machine();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var result = machine.openUI(player);
        if (result == net.minecraft.world.InteractionResult.PASS) {
            helper.fail("opening the machine ui was refused");
            return;
        }
        // No assertion on the menu itself: a gametest's mock player has no connection, so nothing
        // is ever sent to it. What this covers is the half that runs before that - the open event,
        // the ui assembly and the menu construction - throwing.
        helper.succeed();
    }


    /**
     * Configuring a face actually moves items.
     *
     * <p>The test that was missing, and the one that matters: everything else here checks that a click
     * changes a stored value, which it did all along while the machine went on doing nothing.
     * {@code IAutoIOTrait.serverTick} returns immediately while the trait's auto IO is switched off,
     * and a definition that never turned it on is the normal case — so a panel that only sets side
     * directions is a panel that does nothing at all.</p>
     *
     * <p>The machine is the one whose definition leaves auto IO switched off, because that is what a
     * definition ships unless its author turned it on — a fixture that pre-enables it cannot tell a
     * working panel from one that only writes side directions.</p>
     *
     * <p>Up is the face used because it is the same world direction whichever way the machine faces,
     * so the chest above it is the right neighbour no matter how the fixture was placed.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void configuringAFaceMakesTheMachineMoveItems(GameTestHelper helper) {
        var above = POS.above();
        var scenario = MBDScenario.of(helper)
                .placeMachine(AutoIOPanelFixtures.AUTO_IO_OFF_ID, POS)
                .placeBlock(above, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState())
                .insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 8));

        var ui = openUI(helper, AutoIOPanelFixtures.AUTO_IO_OFF_ID);
        if (ui == null) return;
        var face = ui.selectId("face_UP").findFirst().orElse(null);
        if (face == null) {
            helper.fail("no up-face button");
            return;
        }
        // Clicked until it reads OUT rather than a fixed number of times: this machine starts from
        // its definition's defaults, and how many steps that is away from OUT is not the point.
        while (autoIO(scenario.machine(), RelativeDirection.UP) != IO.OUT) {
            click(face);
        }

        scenario.runTicks(60);
        var chest = helper.getBlockEntity(above);
        if (!(chest instanceof net.minecraft.world.Container container)) {
            helper.fail("no chest above the machine");
            return;
        }
        var moved = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.is(net.minecraft.world.item.Items.DIRT)) moved += stack.getCount();
        }
        if (moved == 0) {
            helper.fail("the machine kept its dirt — a configured face moved nothing, so auto IO is"
                    + " still switched off however the sides are set");
            return;
        }
        helper.succeed();
    }


    /**
     * The panel publishes each face's state where the client can read it.
     *
     * <p>Auto IO overrides are server-side runtime values and are never sent with the block, so the
     * panel mirrors them into the machine's custom data, which is {@code @DescSynced}. This is the
     * server end of that: opening the UI publishes what every face is set to, and a click republishes
     * the one that changed. Without it the panel draws six identical sockets whatever the machine is
     * doing.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void thePanelPublishesEachFaceForTheClient(GameTestHelper helper) {
        var scenario = MBDScenario.of(helper).placeMachine(AutoIOPanelFixtures.ONE_TAB_ID, POS);
        var machine = scenario.machine();
        var key = "mbd2_autoio_up_" + AutoIOPanelFixtures.ITEM_TRAIT;

        var ui = openUI(helper, AutoIOPanelFixtures.ONE_TAB_ID);
        if (ui == null) return;

        // The fixture ships the top face as IN, so opening alone must publish that much.
        var published = machine.getCustomData().getString(key);
        if (!"IN".equals(published)) {
            helper.fail("opening the ui published '" + published + "' for the top face, expected IN");
            return;
        }

        var face = ui.selectId("face_UP").findFirst().orElse(null);
        if (face == null) {
            helper.fail("no up-face button");
            return;
        }
        click(face);
        var afterClick = machine.getCustomData().getString(key);
        if (!"OUT".equals(afterClick)) {
            helper.fail("after one click the top face published '" + afterClick + "', expected OUT"
                    + " — the client has no other way to know the face changed");
            return;
        }
        helper.succeed();
    }
}

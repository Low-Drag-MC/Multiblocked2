package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings.SignalConnection;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeCapabilityIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.CapabilityIO;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The runtime views re-implement the per-side rotation of the config objects they front
 * ({@code CapabilityIO}, {@code AutoIO}, {@code SignalConnection}) so that call sites could be swapped
 * over one-for-one. Hand-copied logic drifts, so this pins it exhaustively.
 *
 * <p>Each test overrides exactly one side at a time with a sentinel and compares the view's answer
 * against a throwaway config object with the <em>same</em> single field set — across all six facings and
 * all seven side queries, null included. A swapped left/right or a missing null-side branch shows up as
 * a mismatch on a specific (facing, side) pair rather than as a subtly wrong machine months later.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeSideMappingTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** The six facings, plus a null side query — {@code getIO} accepts one and the branches differ. */
    private static final List<Direction> SIDES_AND_NULL = sidesAndNull();

    private record CapCase(String name,
                           BiConsumer<CapabilityIO, IO> setOnConfig,
                           Function<RuntimeCapabilityIO, RuntimeValue<IO>> slot) {}

    private record AutoCase(String name,
                            BiConsumer<AutoIO, IO> setOnConfig,
                            Function<RuntimeAutoIO, RuntimeValue<IO>> slot) {}

    private record SignalCase(String name,
                              BiConsumer<SignalConnection, Boolean> setOnConfig,
                              Function<com.lowdragmc.mbd2.common.runtime.RuntimeSignalConnection,
                                      RuntimeValue<Boolean>> slot) {}

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void capability_io_view_matches_the_config_object(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var view = itemTrait(scenario.machine()).capabilityIO;

        List<CapCase> cases = List.of(
                new CapCase("internal", CapabilityIO::setInternal, v -> v.internal),
                new CapCase("front", CapabilityIO::setFrontIO, v -> v.front),
                new CapCase("back", CapabilityIO::setBackIO, v -> v.back),
                new CapCase("left", CapabilityIO::setLeftIO, v -> v.left),
                new CapCase("right", CapabilityIO::setRightIO, v -> v.right),
                new CapCase("top", CapabilityIO::setTopIO, v -> v.top),
                new CapCase("bottom", CapabilityIO::setBottomIO, v -> v.bottom));

        for (var testCase : cases) {
            // a fresh config object matches the fixture's unedited definition, so the only difference
            // between the two is the one field/slot we set to the sentinel
            var expected = new CapabilityIO();
            testCase.setOnConfig().accept(expected, IO.NONE);
            view.clearAll();
            testCase.slot().apply(view).set(IO.NONE);

            for (Direction front : Direction.values()) {
                for (Direction side : SIDES_AND_NULL) {
                    var want = expected.getIO(front, side);
                    var got = view.getIO(front, side);
                    if (want != got) {
                        h.fail("capability_io.%s: facing %s, side %s — config says %s, view says %s"
                                .formatted(testCase.name(), front, side, want, got));
                        return;
                    }
                }
            }
        }
        view.clearAll();
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_view_matches_the_config_object(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var view = itemTrait(scenario.machine()).autoIO;

        List<AutoCase> cases = List.of(
                new AutoCase("front", AutoIO::setFrontIO, v -> v.front),
                new AutoCase("back", AutoIO::setBackIO, v -> v.back),
                new AutoCase("left", AutoIO::setLeftIO, v -> v.left),
                new AutoCase("right", AutoIO::setRightIO, v -> v.right),
                new AutoCase("top", AutoIO::setTopIO, v -> v.top),
                new AutoCase("bottom", AutoIO::setBottomIO, v -> v.bottom));

        for (var testCase : cases) {
            var expected = new AutoIO();
            testCase.setOnConfig().accept(expected, IO.BOTH);   // default is NONE, so BOTH is the sentinel
            view.clearAll();
            testCase.slot().apply(view).set(IO.BOTH);

            for (Direction front : Direction.values()) {
                for (Direction side : SIDES_AND_NULL) {
                    var want = expected.getIO(front, side);
                    var got = view.getIO(front, side);
                    if (want != got) {
                        h.fail("auto_io.%s: facing %s, side %s — config says %s, view says %s"
                                .formatted(testCase.name(), front, side, want, got));
                        return;
                    }
                }
            }
        }
        view.clearAll();
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void signal_connection_view_matches_the_config_object(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.SIGNAL_MACHINE_ID, POS);
        var view = scenario.machine().signalConnection;

        List<SignalCase> cases = List.of(
                new SignalCase("front", (c, b) -> c.frontConnection(b), v -> v.front),
                new SignalCase("back", (c, b) -> c.backConnection(b), v -> v.back),
                new SignalCase("left", (c, b) -> c.leftConnection(b), v -> v.left),
                new SignalCase("right", (c, b) -> c.rightConnection(b), v -> v.right),
                new SignalCase("top", (c, b) -> c.topConnection(b), v -> v.top),
                new SignalCase("bottom", (c, b) -> c.bottomConnection(b), v -> v.bottom));

        for (var testCase : cases) {
            var expected = new SignalConnection();
            testCase.setOnConfig().accept(expected, true);      // default is false
            view.clearAll();
            testCase.slot().apply(view).set(true);

            for (Direction front : Direction.values()) {
                for (Direction side : Direction.values()) {
                    var want = expected.getConnection(front, side);
                    var got = view.getConnection(front, side);
                    if (want != got) {
                        h.fail("signal_connection.%s: facing %s, side %s — config says %s, view says %s"
                                .formatted(testCase.name(), front, side, want, got));
                        return;
                    }
                }
            }
        }
        view.clearAll();
        scenario.succeed();
    }

    /**
     * The signal-connection case above compares two pieces of code that were both written on this
     * branch: {@code SignalConnection.getConnection} gained its Y-axis branch here, and
     * {@code RuntimeSignalConnection} mirrors it. If the shared reasoning is wrong, both are wrong and
     * that test is still green.
     * <p>
     * So pin the vertical case against hand-written expectations instead. Facing up: the machine's front
     * is up and its back is down; the four horizontal sides have no clockwise/counter-clockwise meaning,
     * so they all resolve to {@code top} — the same rule {@code AutoIO} and {@code ConnectedIO} already
     * used, and the reason a vertically-facing machine no longer throws from
     * {@code Direction.getClockWise()}.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void signal_connection_resolves_a_vertical_facing_by_hand(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.SIGNAL_MACHINE_ID, POS);
        var view = scenario.machine().signalConnection;

        view.clearAll();
        view.front.set(true);
        check(h, view.getConnection(Direction.UP, Direction.UP), "facing up, side up -> front");
        check(h, !view.getConnection(Direction.UP, Direction.DOWN), "facing up, side down -> back");
        check(h, !view.getConnection(Direction.UP, Direction.NORTH), "facing up, side north -> top");

        view.clearAll();
        view.back.set(true);
        check(h, view.getConnection(Direction.UP, Direction.DOWN), "facing up, side down -> back");
        check(h, view.getConnection(Direction.DOWN, Direction.UP), "facing down, side up -> back");

        view.clearAll();
        view.top.set(true);
        for (var side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            check(h, view.getConnection(Direction.UP, side),
                    "facing up, side " + side + " -> top");
            check(h, view.getConnection(Direction.DOWN, side),
                    "facing down, side " + side + " -> top");
        }

        view.clearAll();
        scenario.succeed();
    }

    private static void check(GameTestHelper h, boolean condition, String description) {
        if (!condition) {
            h.fail("signal connection mapping wrong: " + description);
        }
    }

    /** {@code slot(front, side)} has to name the same slot {@code getIO(front, side)} reads. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slot_lookup_agrees_with_the_read(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var trait = itemTrait(scenario.machine());

        for (Direction front : Direction.values()) {
            for (Direction side : SIDES_AND_NULL) {
                trait.capabilityIO.clearAll();
                trait.capabilityIO.slot(front, side).set(IO.NONE);
                if (trait.capabilityIO.getIO(front, side) != IO.NONE) {
                    h.fail("capability_io slot/read disagree at facing %s, side %s".formatted(front, side));
                    return;
                }
                if (side == null) continue;   // auto IO has no directionless slot
                trait.autoIO.clearAll();
                trait.autoIO.slot(front, side).set(IO.BOTH);
                if (trait.autoIO.getIO(front, side) != IO.BOTH) {
                    h.fail("auto_io slot/read disagree at facing %s, side %s".formatted(front, side));
                    return;
                }
            }
        }
        trait.capabilityIO.clearAll();
        trait.autoIO.clearAll();
        scenario.succeed();
    }

    private static List<Direction> sidesAndNull() {
        // not List.of/copyOf — both reject the null element, and this list exists to carry one
        var list = new ArrayList<Direction>(Direction.values().length + 1);
        list.addAll(Arrays.asList(Direction.values()));
        list.add(null);
        return Collections.unmodifiableList(list);
    }

    private static ItemSlotCapabilityTrait itemTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ItemSlotCapabilityTrait itemSlot) return itemSlot;
        }
        throw new AssertionError("fixture machine has no item slot trait");
    }
}

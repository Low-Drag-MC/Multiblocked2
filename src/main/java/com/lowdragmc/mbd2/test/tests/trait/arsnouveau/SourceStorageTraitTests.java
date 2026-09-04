package com.lowdragmc.mbd2.test.tests.trait.arsnouveau;

import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.CopiableSourceStorage;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class SourceStorageTraitTests {
    static { @SuppressWarnings("unused") var ignored = SourceStorageTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /** For the auto-IO tests, which need room for a neighbour to the east. */
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = MACHINE.relative(Direction.EAST);
    /**
     * Every {@code SourceUtil} scan in this class uses this radius, and it is deliberately tiny.
     * <p>
     * Game tests in a batch run at the same time in one level, laid out a block apart, and
     * {@code SourceManager}'s set is global per dimension — so a wider scan would find the machine
     * belonging to the test running next door and drain it. Four blocks separate two structures'
     * interiors; two keeps every scan inside its own test.
     */
    private static final int RADIUS = 2;

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void source_capability_is_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS)
                .assertExposes(CapabilityRegistry.SOURCE_CAPABILITY, null,
                        cap -> cap.getSourceCapacity() == SourceStorageTraitFixtures.CAPACITY)
                .assertExposes(CapabilityRegistry.SOURCE_CAPABILITY, Direction.NORTH)
                .succeed();
    }

    /**
     * The capability moves source both ways, and honours simulate.
     *
     * <p>This is the shape an Arcane Relay drives: it sizes a transfer with {@code getMaxExtract}, asks
     * for it with {@code simulate = true}, and only then commits. A simulate that moved source would
     * duplicate it once per second, forever.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capability_transfers_and_respects_simulate(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        var cap = MBDTestHelper.capability(h, POS, CapabilityRegistry.SOURCE_CAPABILITY, null);
        if (cap == null) {
            h.fail("no source capability");
            return;
        }
        if (cap.receiveSource(500, true) != 500) {
            h.fail("a simulated receive should report the full amount");
            return;
        }
        if (storage(scenario.machine()).getSource() != 0) {
            h.fail("a simulated receive stored source anyway");
            return;
        }
        if (cap.receiveSource(500, false) != 500 || storage(scenario.machine()).getSource() != 500) {
            h.fail("receiving 500 source did not store 500");
            return;
        }
        if (cap.extractSource(200, true) != 200 || storage(scenario.machine()).getSource() != 500) {
            h.fail("a simulated extract took source anyway");
            return;
        }
        if (cap.extractSource(200, false) != 200 || storage(scenario.machine()).getSource() != 300) {
            h.fail("extracting 200 source left " + storage(scenario.machine()).getSource() + ", expected 300");
            return;
        }
        scenario.succeed();
    }

    /**
     * A side configured OUT-only gives but does not take — and says so before it is asked.
     *
     * <p>{@code getMaxReceive() == 0} is the part worth asserting: a relay reads it to size the
     * transfer, so a wrapper that only rejected the write would have the relay retry every second.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capability_io_gates_the_wrapper(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.EXTRACT_ONLY_MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);
        var cap = MBDTestHelper.capability(h, POS, CapabilityRegistry.SOURCE_CAPABILITY, null);
        if (cap == null) {
            h.fail("no source capability");
            return;
        }
        if (cap.getMaxReceive() != 0 || cap.canReceive()) {
            h.fail("an OUT-only machine advertised room to receive");
            return;
        }
        if (cap.receiveSource(100, false) != 0 || storage(scenario.machine()).getSource() != 1000) {
            h.fail("an OUT-only machine accepted source");
            return;
        }
        if (cap.getMaxExtract() == 0 || cap.extractSource(100, false) != 100) {
            h.fail("an OUT-only machine refused to give source");
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_source(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        storage(scenario.machine()).setSource(4321);
        scenario.assertPersistenceRoundTrip()
                .check("stored source should survive a save/load cycle",
                        m -> storage(m).getSource() == 4321)
                .succeed();
    }

    /**
     * The {@code capacity} runtime value resizes the buffer, and shrinking spills rather than leaving
     * it over-full — a storage reporting more than it can hold makes every percentage reader, in the
     * GUI and in other mods, draw past 100%.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capacity_override_resizes_and_clamps(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS)
                .with(m -> storage(m).setSource(5000))
                .check("the capacity starts on the definition",
                        m -> storage(m).getSourceCapacity() == SourceStorageTraitFixtures.CAPACITY)
                .with(m -> trait(m).capacity.set(100))
                .check("an override should resize the buffer", m -> storage(m).getSourceCapacity() == 100)
                .check("and clamp what was already stored", m -> storage(m).getSource() == 100)
                .assertPersistenceRoundTrip()
                .check("the override should survive a save/load cycle",
                        m -> storage(m).getSourceCapacity() == 100)
                .with(m -> trait(m).capacity.clear())
                .check("clearing should go back to the definition",
                        m -> storage(m).getSourceCapacity() == SourceStorageTraitFixtures.CAPACITY)
                .succeed();
    }

    /**
     * The machine is visible to Ars Nouveau's own devices.
     *
     * <p>{@code SourceUtil} is what an Enchanting Apparatus, a Ritual Brazier and a Sourcelink all use,
     * and it only finds a {@code SourceJarTile} or something registered in {@code SourceManager}. This
     * is the whole point of {@code MachineSourceProvider}, and nothing else in the integration would
     * fail if the registration silently stopped happening.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void machine_is_visible_to_ars_devices(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);
        var absPos = h.absolutePos(POS);
        if (!SourceUtil.hasSourceNearby(absPos, h.getLevel(), RADIUS, 500)) {
            h.fail("SourceUtil could not find the machine's source — it is not in SourceManager");
            return;
        }
        if (SourceUtil.hasSourceNearby(absPos, h.getLevel(), RADIUS, 5000)) {
            h.fail("SourceUtil found 5000 source in a machine holding 1000");
            return;
        }
        scenario.succeed();
    }

    /**
     * And an Ars Nouveau device really can take that source out.
     *
     * <p>Exercises the return-value contract of {@code ISourceTile}, which is the easiest thing in this
     * integration to get backwards: {@code removeSource(int)} answers with the resulting total, not the
     * amount removed, and {@code SourceUtil.takeSourceMultiple} reads an extraction as before-minus-after.
     * Returning the amount instead would look like the machine had far more source than it does.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void ars_devices_can_draw_from_the_machine(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);
        var absPos = h.absolutePos(POS);
        var drained = SourceUtil.takeSourceMultiple(absPos, h.getLevel(), RADIUS, 600);
        if (drained == null || drained.isEmpty()) {
            h.fail("takeSourceMultiple found nothing to draw from");
            return;
        }
        if (storage(scenario.machine()).getSource() != 400) {
            h.fail("drawing 600 source left " + storage(scenario.machine()).getSource() + ", expected 400");
            return;
        }
        // and the all-or-nothing path puts everything back when the area cannot cover the cost
        if (SourceUtil.takeSourceMultiple(absPos, h.getLevel(), RADIUS, 900) != null) {
            h.fail("takeSourceMultiple claimed to draw 900 from a machine holding 400");
            return;
        }
        if (storage(scenario.machine()).getSource() != 400) {
            h.fail("a failed draw did not roll back: " + storage(scenario.machine()).getSource());
            return;
        }
        scenario.succeed();
    }

    /** And put source in, the way a Sourcelink deposits into a jar. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void ars_devices_can_deposit_into_the_machine(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        var absPos = h.absolutePos(POS);
        var self = SourceUtil.canGiveSource(absPos, h.getLevel(), RADIUS).stream()
                .filter(provider -> provider.getCurrentPos().equals(absPos))
                .findFirst()
                .orElse(null);
        if (self == null) {
            h.fail("an empty machine did not offer itself as somewhere to put source");
            return;
        }
        self.getSource().addSource(750);
        if (storage(scenario.machine()).getSource() != 750) {
            h.fail("addSource(750) stored " + storage(scenario.machine()).getSource());
            return;
        }
        scenario.succeed();
    }

    /** With the toggle off the machine is invisible to those same devices. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void expose_toggle_hides_the_machine(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.PRIVATE_MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);
        if (isRegistered(h, POS)) {
            h.fail("a machine with expose_to_devices off was still registered with SourceManager");
            return;
        }
        // but the capability is still there, so an Arcane Relay can be pointed at it
        scenario.assertExposes(CapabilityRegistry.SOURCE_CAPABILITY, null).succeed();
    }

    /**
     * Turning the toggle on at runtime registers the machine, turning it off retires it.
     *
     * <p>{@code SourceManager} has no removal method — it prunes providers that report themselves
     * invalid — so "off" has to work by the provider going invalid, and a stale entry left behind
     * would keep answering for a machine that no longer wants to be found.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void expose_toggle_can_be_overridden_at_runtime(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.PRIVATE_MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);

        trait(scenario.machine()).exposeToDevices.set(true);
        if (!isRegistered(h, POS)) {
            h.fail("turning expose_to_devices on did not register the machine");
            return;
        }
        trait(scenario.machine()).exposeToDevices.set(false);
        if (isRegistered(h, POS)) {
            h.fail("turning expose_to_devices off did not retire the provider");
            return;
        }
        scenario.succeed();
    }

    /**
     * And it still works once {@code SourceManager} has actually thrown the provider away.
     *
     * <p>Turning the toggle off only makes the provider report itself invalid; the manager drops it on
     * its own schedule, up to a second later. Everything between those two moments looks fine either
     * way, which is what makes this worth its own test — the trait remembers that it registered, and a
     * version that keeps remembering across the prune never re-adds the provider, leaving a machine that
     * says it is exposed and is not.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void expose_toggle_survives_a_source_manager_prune(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(SourceStorageTraitFixtures.MACHINE_ID, POS);
        storage(scenario.machine()).setSource(1000);

        trait(scenario.machine()).exposeToDevices.set(false);
        if (!prune(h)) {
            h.fail("could not reach a game time SourceManager prunes on");
            return;
        }
        if (heldBySourceManager(h, POS)) {
            h.fail("the prune did not drop the retired provider");
            return;
        }

        trait(scenario.machine()).exposeToDevices.set(true);
        if (!isRegistered(h, POS)) {
            h.fail("turning expose_to_devices back on after a prune did not re-register the machine");
            return;
        }
        scenario.succeed();
    }

    /**
     * Run {@code SourceManager}'s own prune pass.
     *
     * <p>It only does anything on a game time divisible by 60, and it is normally driven by
     * {@code LevelTickEvent.Post} — which the test harness's direct {@code level.tick()} does not fire.
     * So: tick until the clock lines up, then call it.</p>
     */
    private static boolean prune(GameTestHelper h) {
        for (int i = 0; i < 60 && h.getLevel().getGameTime() % 60 != 0; i++) {
            MBDTestHelper.runTicks(h, 1);
        }
        if (h.getLevel().getGameTime() % 60 != 0) return false;
        SourceManager.INSTANCE.tick(h.getLevel());
        return true;
    }

    /** As {@link #isRegistered}, but counting entries the manager still holds whether valid or not. */
    private static boolean heldBySourceManager(GameTestHelper h, BlockPos relPos) {
        var absPos = h.absolutePos(relPos);
        return SourceManager.INSTANCE.getCopySetForLevel(h.getLevel()).stream()
                .anyMatch(provider -> provider.getCurrentPos().equals(absPos));
    }

    /**
     * Whether {@link com.hollingsworth.arsnouveau.api.source.SourceManager} holds a live provider for
     * the machine at {@code relPos}, asked exactly rather than by radius.
     *
     * <p>The negative assertions need this: {@code SourceUtil} answers about an area, and in a game test
     * that area can contain the machine belonging to whichever test is running next door.</p>
     */
    private static boolean isRegistered(GameTestHelper h, BlockPos relPos) {
        var absPos = h.absolutePos(relPos);
        return SourceManager.INSTANCE.getCopySetForLevel(h.getLevel()).stream()
                .anyMatch(provider -> provider.isValid() && provider.getCurrentPos().equals(absPos));
    }

    /** Auto-IO pulls from an adjacent Source Jar — no relay, no wand. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void auto_io_pulls_from_an_adjacent_source_jar(GameTestHelper h) {
        placeSourceJar(h, EAST, 2000);
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(SourceStorageTraitFixtures.AUTO_IN_MACHINE_ID, MACHINE, Direction.NORTH)
                .runTicks(4);
        if (storage(scenario.machine()).getSource() <= 0) {
            h.fail("auto-IO pulled nothing out of the adjacent source jar");
            return;
        }
        if (sourceJar(h, EAST).getSource() >= 2000) {
            h.fail("the source jar was not drained");
            return;
        }
        scenario.succeed();
    }

    /** And pushes into one. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void auto_io_pushes_into_an_adjacent_source_jar(GameTestHelper h) {
        placeSourceJar(h, EAST, 0);
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(SourceStorageTraitFixtures.AUTO_OUT_MACHINE_ID, MACHINE, Direction.NORTH);
        storage(scenario.machine()).setSource(2000);
        scenario.runTicks(4);
        if (sourceJar(h, EAST).getSource() <= 0) {
            h.fail("auto-IO pushed nothing into the adjacent source jar");
            return;
        }
        scenario.succeed();
    }

    private static void placeSourceJar(GameTestHelper h, BlockPos relPos, int source) {
        h.setBlock(relPos, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        if (source > 0) sourceJar(h, relPos).setSource(source);
    }

    private static SourceJarTile sourceJar(GameTestHelper h, BlockPos relPos) {
        if (h.getBlockEntity(relPos) instanceof SourceJarTile jar) return jar;
        throw new AssertionError("no source jar at " + relPos);
    }

    private static SourceStorageCapabilityTrait trait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof SourceStorageCapabilityTrait sourceTrait) return sourceTrait;
        }
        throw new AssertionError("fixture machine has no source storage trait");
    }

    private static CopiableSourceStorage storage(MBDMachine machine) {
        return trait(machine).getStorage();
    }
}

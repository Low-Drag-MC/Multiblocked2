package com.lowdragmc.mbd2.test.tests.fx;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.mbd2.common.machine.fx.IMachineFXManager;
import com.lowdragmc.mbd2.common.machine.fx.MachineFXManagers;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.tests.MBDSmokeFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

/**
 * The server half of machine FX: there isn't one, and that is the assertion.
 *
 * <p>Effects are client objects with no server counterpart. A server machine therefore gets
 * {@link IMachineFXManager#NOOP}, and every public entry point on {@code MBDMachine} has to be
 * callable there anyway — blueprints run on the server, so {@code Play Machine FX} downstream of
 * {@code Machine Tick} reaches these methods every tick on a dedicated server. If any of them
 * touched a client or Photon class, that is a crash rather than a relay.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class MachineFXRuntimeTests {
    static { @SuppressWarnings("unused") var ignored = MBDSmokeFixtures.SIMPLE_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void server_machine_gets_the_noop_manager(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, POS)
                .check("a server machine has no real FX manager",
                        m -> MachineFXManagers.create(m) == IMachineFXManager.NOOP)
                .check("and getFXManager agrees", m -> m.getFXManager() == IMachineFXManager.NOOP)
                .succeed();
    }

    /** The no-op has to be inert, not merely present — nothing may accumulate in it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void noop_manager_never_holds_anything(GameTestHelper h) {
        var manager = IMachineFXManager.NOOP;
        var config = new MachineFXConfig("burst",
                ResourceLocation.fromNamespaceAndPath("photon", "fire"));

        manager.play(config, "burst");
        if (manager.isPlaying("burst")) h.fail("the no-op manager should never report a live effect");
        if (!manager.playingIdentifiers().isEmpty()) h.fail("the no-op manager should hold nothing");
        manager.stop("burst", true);
        manager.stopAll(false);
        manager.stopAllWithPrefix("state:", true);
        h.succeed();
    }

    /**
     * Every FX entry point called on a server machine: each must relay and return, not throw. A
     * server has no {@code FXHelper}, so a missing guard shows up here as NoClassDefFoundError.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void server_side_fx_calls_relay_without_throwing(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, POS)
                .with(m -> {
                    m.playMachineFX("does_not_exist");
                    m.stopMachineFX("does_not_exist", true);
                    m.emitPhotonFx("adhoc",
                            ResourceLocation.fromNamespaceAndPath("photon", "fire"),
                            new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1),
                            0, false, true);
                    m.stopMachineFX("adhoc", false);
                })
                .check("nothing was recorded on the server",
                        m -> m.getFXManager().playingIdentifiers().isEmpty())
                .succeed();
    }

    /**
     * A machine ticks on the server too, and {@code clientTick} is where the state sync lives — so
     * the state path must never be reached here. Running the machine for a while and finding the
     * manager still empty is what says so.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void state_effects_do_not_start_on_a_server(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(MBDSmokeFixtures.SIMPLE_MACHINE_ID, POS)
                .with(m -> {
                    var state = m.getMachineState();
                    state.machineFXs().setEnable(true);
                    state.machineFXs().getFxs().add(new MachineFXConfig("idle",
                            ResourceLocation.fromNamespaceAndPath("photon", "idle")));
                })
                .runTicks(10)
                .check("no state effects started server-side",
                        m -> m.getFXManager().playingIdentifiers().isEmpty())
                .succeed();
    }

    /**
     * The two authoring routes must not share a slot: a library entry and a state entry may both be
     * called {@code smoke}, and stopping one must not stop the other.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void state_and_library_identifiers_do_not_collide(GameTestHelper h) {
        var stateIdentifier = com.lowdragmc.mbd2.common.machine.MBDMachine.STATE_FX_PREFIX + "smoke";
        if (stateIdentifier.equals("smoke")) {
            h.fail("state effects must not use the bare name as their identifier");
        }
        h.succeed();
    }
}

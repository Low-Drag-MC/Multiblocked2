package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.photon.PhotonFXBridge;
import com.lowdragmc.mbd2.test.tests.fx.MachineFXFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-state machine effects, on a real machine in a real world.
 *
 * <p>This is the half that matters and the half nothing else can reach. Effects are started from
 * {@code MBDMachine.clientTick} off the {@code @DescSynced} machine state — never from an event —
 * precisely so a machine that was already working before the player arrived still shows them. That
 * design only pays off across a real client/server boundary: a gametest server has no client machine
 * to sync to, and the editor preview has no boundary at all.</p>
 *
 * <p>So the scenario drives the state from the <em>server</em> and asserts on the <em>client</em>:
 * the effect must appear without anything ever telling the client to play it, and disappear when the
 * machine leaves the state.</p>
 *
 * <p>The screenshots frame the particles against sky and nothing else: the fixture machine is built
 * with {@code IRenderer.EMPTY}, so the block itself draws nothing. That is on purpose — what is being
 * captured is the effect, and a machine model would only be scenery.</p>
 *
 * <p>Run with {@code gradlew runClient -PldTest=mbd2_machine_state_fx}.</p>
 */
@LDLRegisterClient(name = "mbd2_machine_state_fx", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MachineStateFXScenario implements UIScenario {

    /** On the superflat surface, so the chunk is loaded and the block entity is tracked by the client. */
    private static final BlockPos MACHINE = new BlockPos(8, 4, 8);
    private static final Vec3 CAMERA = new Vec3(MACHINE.getX() + 0.5, MACHINE.getY() + 1.5, MACHINE.getZ() + 5.5);

    /** What {@code MBDMachine.STATE_FX_PREFIX} makes of the fixture's effect name. */
    private static final String STATE_IDENTIFIER = MBDMachine.STATE_FX_PREFIX + MachineFXFixtures.FX_NAME;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(200).tags("mbd2", "fx", "sides", "visual").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.setGameMode(GameType.SPECTATOR)
                .fill(MACHINE.offset(-2, 0, -2), MACHINE.offset(2, 3, 2), Blocks.AIR.defaultBlockState())
                .teleportPlayer(CAMERA.x, CAMERA.y, CAMERA.z, 180, 8)
                .setBlock(MACHINE, MBDRegistries.MACHINE_DEFINITIONS
                        .get(MachineFXFixtures.STATE_FX_MACHINE_ID).block())
                .awaitClientChunk(MACHINE)
                .awaitClientBlockEntity(MACHINE)
                .waitUntil("the client sees the machine", ctx -> clientMachine(ctx) != null)
                .check("Photon is loaded and the fixture's effect resolves", ctx -> {
                    var found = PhotonFXBridge.hasFX(MachineFXFixtures.TEST_FX);
                    ctx.attach("photon", MBD2.isPhotonLoaded() + ", fx loadable=" + found);
                    return MBD2.isPhotonLoaded() && found;
                })

                // The root state carries no effects, so anything playing here would mean the state
                // sync fires for states that never asked for one.
                .check("nothing plays while the machine is idle", ctx -> {
                    var machine = clientMachine(ctx);
                    return machine != null && machine.getFXManager().playingIdentifiers().isEmpty();
                })
                .screenshot("01_idle")

                // Nothing tells the client to play anything — only the state is synced.
                .server("move the machine into its working state on the server",
                        sc -> serverMachine(sc).setMachineState("working"))
                .serverTicks(6)
                .waitUntil("the client starts the state's effect by itself", ctx -> {
                    var machine = clientMachine(ctx);
                    return machine != null && machine.getFXManager().isPlaying(STATE_IDENTIFIER);
                })
                .checkServer("and the server started nothing of its own",
                        sc -> serverMachine(sc).getFXManager().playingIdentifiers().isEmpty())
                // long enough for the emitter to produce particles rather than catching the first frame
                .settleMs(900)
                .check("particles reached the world particle engine", ctx -> {
                    var count = net.minecraft.client.Minecraft.getInstance().particleEngine.countParticles();
                    ctx.attach("particle_count", count);
                    return !count.equals("0");
                })
                .screenshot("02_working_fx_playing")

                // Leaving the state must retire it, or an effect would outlive the state that owns it.
                .server("return the machine to its idle state",
                        sc -> serverMachine(sc).setMachineState("base"))
                .serverTicks(6)
                .waitUntil("the client stops the effect by itself", ctx -> {
                    var machine = clientMachine(ctx);
                    return machine != null && !machine.getFXManager().isPlaying(STATE_IDENTIFIER);
                })
                .check("and forgets it entirely rather than leaving a dead entry", ctx -> {
                    var machine = clientMachine(ctx);
                    return machine != null && machine.getFXManager().playingIdentifiers().isEmpty();
                })
                .settleMs(600)
                .screenshot("03_idle_again")
                .teardownServer("clean up",
                        sc -> sc.level().setBlockAndUpdate(MACHINE, Blocks.AIR.defaultBlockState()));
    }

    @Nullable
    private static MBDMachine clientMachine(TestContext ctx) {
        var be = ctx.clientBlockEntity(MACHINE, MachineBlockEntity.class);
        return be == null ? null : asMachine(be);
    }

    private static MBDMachine serverMachine(ServerContext sc) {
        var be = sc.blockEntity(MACHINE, MachineBlockEntity.class);
        var machine = be == null ? null : asMachine(be);
        if (machine == null) {
            throw new AssertionError("no MBD machine on the server at " + MACHINE);
        }
        return machine;
    }

    @Nullable
    private static MBDMachine asMachine(MachineBlockEntity be) {
        return IMachine.ofMachine(be).filter(MBDMachine.class::isInstance)
                .map(MBDMachine.class::cast).orElse(null);
    }
}

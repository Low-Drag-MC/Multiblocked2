package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.test.tests.runtime.RuntimeValueFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime value overrides are <b>persistence only</b>: each side owns the overrides on its own machine
 * instance and nothing is sent between them.
 * <p>
 * That contract cannot be checked headlessly — a gametest server has no client machine to compare
 * against — and getting it wrong is silent in both directions. Adding {@code @DescSynced} would
 * quietly start shipping auto IO config to every tracking client; and if a client override somehow
 * escaped to the server it would be a desync nobody could reproduce. So this pins both halves against a
 * real client:
 * <ol>
 *     <li>a server-side override does not reach the client, which keeps reading its definition;</li>
 *     <li>a client-side override takes effect on the client and leaves the server alone — which is how
 *     a client-only value (a render toggle, an overlay preference) is meant to be held.</li>
 * </ol>
 * Run with {@code gradlew runClient -PldTest=mbd2_runtime_value_sides}.
 */
@LDLRegisterClient(name = "mbd2_runtime_value_sides", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class RuntimeValueSidesScenario implements UIScenario {

    /** On the superflat surface so the chunk is loaded and the block entity is tracked by the client. */
    private static final BlockPos MACHINE = new BlockPos(8, 4, 8);
    private static final Vec3 CAMERA = new Vec3(MACHINE.getX() + 0.5, MACHINE.getY() + 1.5, MACHINE.getZ() + 5.5);

    private static final int AUTHORED_LEVEL = 0;
    private static final int SERVER_LEVEL = 7;
    private static final int CLIENT_LEVEL = 3;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(200).tags("mbd2", "runtime-value", "sides").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.setGameMode(GameType.SPECTATOR)
                .fill(MACHINE.offset(-2, 0, -2), MACHINE.offset(2, 3, 2), Blocks.AIR.defaultBlockState())
                .teleportPlayer(CAMERA.x, CAMERA.y, CAMERA.z, 180, 8)
                .setBlock(MACHINE, MBDRegistries.MACHINE_DEFINITIONS
                        .get(RuntimeValueFixtures.AUTO_IO_MACHINE_ID).block())
                .awaitClientChunk(MACHINE)
                .awaitClientBlockEntity(MACHINE)
                .waitUntil("the client sees the machine", ctx -> clientMachine(ctx) != null)

                // Prove this scenario actually crosses the client/server boundary. An integrated server
                // shares a JVM with the client, so if clientBlockEntity() ever handed back the server's
                // block entity every check below would pass for the wrong reason.
                .serverGet("record the server machine's identity", "serverMachineId",
                        sc -> System.identityHashCode(serverMachine(sc)))
                .check("the client's machine is a different object from the server's", ctx -> {
                    var client = clientMachine(ctx);
                    return client != null
                            && System.identityHashCode(client) != (int) ctx.get("serverMachineId");
                })
                .check("both sides start on the definition's values", ctx -> {
                    var machine = clientMachine(ctx);
                    var trait = clientTrait(ctx);
                    return machine != null && trait != null
                            && machine.getMachineLevel() == AUTHORED_LEVEL
                            && trait.capabilityIO.top.get() == IO.BOTH;
                })

                // (1) a server-side override stays on the server
                .server("override tier and capability IO server-side", sc -> {
                    serverMachine(sc).setMachineLevel(SERVER_LEVEL);
                    serverTrait(sc).capabilityIO.top.set(IO.NONE);
                })
                .serverTicks(4)
                .checkServer("the server holds both overrides",
                        sc -> serverMachine(sc).getMachineLevel() == SERVER_LEVEL
                                && serverTrait(sc).capabilityIO.top.get() == IO.NONE)
                .check("the client still reads its definition's tier", ctx ->
                        clientMachine(ctx) != null
                                && clientMachine(ctx).getMachineLevel() == AUTHORED_LEVEL
                                && !clientMachine(ctx).getRuntimeValues().isOverridden("machine_level"))
                .check("and its definition's capability IO", ctx ->
                        clientTrait(ctx) != null
                                && clientTrait(ctx).capabilityIO.top.get() == IO.BOTH
                                && !clientTrait(ctx).capabilityIO.top.isOverridden())

                // (2) a client-side override is how a client-only value is held, and stays client-side
                .step("override the tier on the client instance",
                        ctx -> clientMachine(ctx).setMachineLevel(CLIENT_LEVEL))
                .serverTicks(4)
                .check("the client sees its own override",
                        ctx -> clientMachine(ctx).getMachineLevel() == CLIENT_LEVEL)
                .checkServer("the server is untouched by it",
                        sc -> serverMachine(sc).getMachineLevel() == SERVER_LEVEL)

                // logged unconditionally: "no packet arrived" and "a packet arrived with the wrong value"
                // look identical from an assertion alone
                .step("report both sides", ctx -> {
                    var client = clientMachine(ctx);
                    var trait = clientTrait(ctx);
                    ctx.log("client tier=" + (client == null ? "no machine" : client.getMachineLevel())
                            + " capability_io.top=" + (trait == null ? "no trait" : trait.capabilityIO.top.get()));
                })
                .screenshot("runtime_value_sides")
                .teardownServer("clean up", sc -> sc.level().setBlockAndUpdate(MACHINE, Blocks.AIR.defaultBlockState()));
    }

    @Nullable
    private static MBDMachine clientMachine(TestContext ctx) {
        var be = ctx.clientBlockEntity(MACHINE, MachineBlockEntity.class);
        return be == null ? null : asMachine(be);
    }

    @Nullable
    private static SimpleCapabilityTrait<?, ?> clientTrait(TestContext ctx) {
        return firstCapabilityTrait(clientMachine(ctx));
    }

    private static MBDMachine serverMachine(ServerContext sc) {
        var be = sc.blockEntity(MACHINE, MachineBlockEntity.class);
        var machine = be == null ? null : asMachine(be);
        if (machine == null) {
            throw new AssertionError("no MBD machine on the server at " + MACHINE);
        }
        return machine;
    }

    private static SimpleCapabilityTrait<?, ?> serverTrait(ServerContext sc) {
        var trait = firstCapabilityTrait(serverMachine(sc));
        if (trait == null) {
            throw new AssertionError("the fixture machine has no capability trait");
        }
        return trait;
    }

    @Nullable
    private static MBDMachine asMachine(IMachineBlockEntity be) {
        return be.getMetaMachine() instanceof MBDMachine machine ? machine : null;
    }

    @Nullable
    private static SimpleCapabilityTrait<?, ?> firstCapabilityTrait(@Nullable MBDMachine machine) {
        if (machine == null) return null;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof SimpleCapabilityTrait<?, ?> capabilityTrait) return capabilityTrait;
        }
        return null;
    }
}

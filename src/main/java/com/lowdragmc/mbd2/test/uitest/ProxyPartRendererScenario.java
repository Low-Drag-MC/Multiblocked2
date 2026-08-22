package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.client.renderer.ProxyPartRenderer;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.integration.geckolib.AnimatableBlock;
import com.lowdragmc.mbd2.test.tests.multiblock.ProxyRendererFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Issue #236: a {@code proxyWhileFormed} port only ever rendered its baked model. BER-driven
 * renderers — GeckoLib above all — never got a render call, because no {@code BlockEntityRenderer}
 * was ever registered for {@link ProxyPartBlockEntity}.
 * <p>
 * This cannot be a gametest: it needs a real client with a render loop. The checks below pin the two
 * halves of the fix:
 * <ol>
 *     <li>the render dispatcher resolves a renderer for the port at all — that is the registration;</li>
 *     <li>GeckoLib bound an {@link AnimatableBlock} to the port rather than falling back to the
 *     shared static animatable, which is what makes facing and state animations follow the
 *     controller instead of being frozen.</li>
 * </ol>
 * Run with {@code gradlew runClient -PldTest=mbd2_proxy_part_renderer}.
 */
// modID gates on the FML annotation scan, before the class is loaded: this scenario reaches into
// AnimatableBlock, so without it an install without GeckoLib would hit a NoClassDefFoundError.
@LDLRegisterClient(name = "mbd2_proxy_part_renderer", group = "mbd2", registry = UIScenario.REGISTRY,
        modID = "geckolib", environment = RegistrationEnvironment.DEV_ONLY)
public class ProxyPartRendererScenario implements UIScenario {

    // Sitting on the superflat surface (grass tops out at y=3) rather than floating: the port only
    // gets a render call while its chunk section is actually being drawn, so the structure has to be
    // somewhere the player can stand and look at without falling away from it.
    private static final BlockPos CONTROLLER = new BlockPos(8, 4, 8);
    private static final BlockPos PORT = CONTROLLER.west();
    private static final BlockPos STONE = CONTROLLER.east();
    /** Fixed camera rig, south of the structure and looking north at it. */
    private static final Vec3 CAMERA = new Vec3(CONTROLLER.getX() + 0.5, CONTROLLER.getY() + 1.5, CONTROLLER.getZ() + 6.5);

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(200).tags("render", "proxy", "geckolib").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        // Spectator, so the camera is a fixed rig: a creative player teleported before the terrain
        // finishes streaming just falls through the world and takes the port out of frame with it.
        s.setGameMode(GameType.SPECTATOR)
                .fill(CONTROLLER.offset(-4, 0, -4), CONTROLLER.offset(4, 4, 4), Blocks.AIR.defaultBlockState())
                .teleportPlayer(CAMERA.x, CAMERA.y, CAMERA.z, 180, 8)
                .setBlock(CONTROLLER, MBDRegistries.MACHINE_DEFINITIONS
                        .get(ProxyRendererFixtures.GECKOLIB_PROXY_CONTROLLER_ID).block())
                .setBlock(PORT, Blocks.IRON_BLOCK)
                .setBlock(STONE, Blocks.STONE)
                .server("form the structure", sc -> {
                    if (IMachine.ofMachine(sc.level(), CONTROLLER).orElse(null)
                            instanceof MBDMultiblockMachine controller && controller.checkPatternWithLock()) {
                        controller.onStructureFormed();
                    }
                })
                .checkServer("the iron block became a ProxyPartBlock",
                        sc -> sc.level().getBlockState(PORT).is(ProxyPartBlock.BLOCK))
                .awaitClientChunk(PORT)
                .awaitClientBlockEntity(PORT)
                .waitUntil("the client sees the port as a ProxyPartBlock", ctx -> port(ctx) != null)
                .check("the proxy state resolves a BER-driven renderer", ctx -> {
                    var proxy = port(ctx);
                    return proxy != null && ProxyPartRenderer.INSTANCE.hasBlockEntityRenderer(proxy);
                })
                .check("the render dispatcher resolves a BlockEntityRenderer for the port", ctx -> {
                    var proxy = port(ctx);
                    return proxy != null && ctx.mc().getBlockEntityRenderDispatcher().getRenderer(proxy) != null;
                })
                // Logged unconditionally: when a render check fails, the auto-captured screenshot rarely
                // says why, and "the camera drifted off the structure" looks identical to "nothing drew".
                .step("report camera and client world state", ctx -> {
                    var player = ctx.requirePlayer();
                    ctx.log("player " + player.position() + " yaw=" + player.getYRot() + " pitch=" + player.getXRot());
                    ctx.log("client port=" + ctx.level().getBlockState(PORT)
                            + " controller=" + ctx.level().getBlockState(CONTROLLER)
                            + " stone=" + ctx.level().getBlockState(STONE));
                })
                // Polled rather than a fixed frame count: the section has to be recompiled with the
                // port in its renderable list before GeckoLib sees a single render call.
                .waitUntil("GeckoLib binds an animatable to the port instead of the shared static one",
                        ctx -> !boundAnimatables(ctx).isEmpty())
                .check("the bound animatable is driven by the port's own animation source",
                        ctx -> boundAnimatables(ctx).stream()
                                .anyMatch(animatable -> animatable.getSource() == port(ctx)))
                .screenshot("proxy_part_geckolib")
                .teardownServer("clean up the structure", sc -> {
                    for (var pos : BlockPos.betweenClosed(CONTROLLER.offset(-4, 0, -4), CONTROLLER.offset(4, 4, 4))) {
                        sc.level().setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
                    }
                });
    }

    @Nullable
    private static ProxyPartBlockEntity port(TestContext ctx) {
        return ctx.clientBlockEntity(PORT, ProxyPartBlockEntity.class);
    }

    /** The port's GeckoLib animatables, empty while it is still falling back to the shared static one. */
    private static List<AnimatableBlock> boundAnimatables(TestContext ctx) {
        var proxy = port(ctx);
        if (proxy == null) return List.of();
        return proxy.getAnimatableCache().values().stream()
                .filter(AnimatableBlock.class::isInstance)
                .map(AnimatableBlock.class::cast)
                .toList();
    }
}

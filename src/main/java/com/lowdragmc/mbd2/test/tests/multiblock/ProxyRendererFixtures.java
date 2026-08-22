package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * Fixture for issue #236: a {@code proxyWhileFormed} predicate whose proxy state renders with a
 * BER-driven renderer (GeckoLib here) instead of a baked JSON model.
 * <p>
 * Reuses the GeckoLib demo assets that ship with MBD2, so the scenario needs no extra resources. On a
 * dedicated server {@code MachineState.Builder#renderer} substitutes {@code IRenderer.EMPTY}, so this
 * fixture is safe to register on both sides; it simply has nothing to render there.
 */
public class ProxyRendererFixtures implements TestFixtureProvider {
    public static final ResourceLocation GECKOLIB_PROXY_CONTROLLER_ID = MBD2.id("test_proxy_geckolib_controller");

    private static final ResourceLocation GEO_MODEL = MBD2.id("geo/fire_pedestal.geo.json");
    private static final ResourceLocation GEO_TEXTURE = MBD2.id("textures/block/fire_pedestal.png");
    private static final ResourceLocation GEO_ANIMATION = MBD2.id("animations/fire_pedestal.animation.json");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.multiblock(GECKOLIB_PROXY_CONTROLLER_ID)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(Blocks.IRON_BLOCK).proxyWhileFormed(proxy ->
                                proxy.setStateMachine(new StateMachine<>(MachineState.baseBuilder()
                                        .geckolibRenderer(GEO_MODEL, GEO_TEXTURE, GEO_ANIMATION)
                                        .child("formed", formed -> formed
                                                .child("working", working -> working.child("waiting"))
                                                .child("suspend"))
                                        .child("unformed")
                                        .build()))))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}

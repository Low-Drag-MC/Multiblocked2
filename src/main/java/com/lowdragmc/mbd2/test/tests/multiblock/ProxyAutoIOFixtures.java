package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * Fixtures for issue #237: a {@code proxyWhileFormed} predicate carrying {@code proxyCapabilities}
 * with auto IO enabled has to actually move items through the proxying block, not just expose the
 * controller's trait there.
 * <p>
 * Every fixture drives the {@code top} side only (interval 1, so the tests are tick-deterministic),
 * which keeps the port from also talking to the controller sitting next to it in the pattern.
 */
public class ProxyAutoIOFixtures implements TestFixtureProvider {
    /** Predicate matches a plain iron block → replaced by a ProxyPartBlock that must push items up. */
    public static final ResourceLocation BLOCK_PORT_OUTPUT_ID = MBD2.id("test_proxy_autoio_block_out");
    /** Same, pulling items down from the block above instead. */
    public static final ResourceLocation BLOCK_PORT_INPUT_ID = MBD2.id("test_proxy_autoio_block_in");
    /** Predicate matches an MBD part; the part keeps its block but must push the controller's items up. */
    public static final ResourceLocation PART_PORT_OUTPUT_ID = MBD2.id("test_proxy_autoio_part_out");
    /** The part used by {@link #PART_PORT_OUTPUT_ID}: no proxy capabilities of its own. */
    public static final ResourceLocation PART_PORT_ID = MBD2.id("test_proxy_autoio_part");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.multiblock(BLOCK_PORT_OUTPUT_ID)
                .withItemSlots(1, IO.BOTH)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(Blocks.IRON_BLOCK)
                                .proxyWhileFormed(proxy -> proxy.getProxyCapabilities().add(autoIO(IO.OUT))))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(BLOCK_PORT_INPUT_ID)
                .withItemSlots(1, IO.BOTH)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(Blocks.IRON_BLOCK)
                                .proxyWhileFormed(proxy -> proxy.getProxyCapabilities().add(autoIO(IO.IN))))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        // A bare part: partSettings has to exist so the machine becomes an MBDPartMachine, but it
        // carries no proxyControllerCapabilities — only the predicate below grants the proxy.
        var part = TestMachineBuilder.simple(PART_PORT_ID)
                .withPartSettings(ps -> ps.setEnable(true))
                .register(event);

        TestMachineBuilder.multiblock(PART_PORT_OUTPUT_ID)
                .withItemSlots(1, IO.BOTH)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(part.block())
                                .proxyWhileFormed(proxy -> proxy.getProxyCapabilities().add(autoIO(IO.OUT))))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }

    /** A proxy capability that forwards the controller's item slot trait and auto-IOs it upwards. */
    private static ConfigPartSettings.ProxyCapability autoIO(IO io) {
        var cap = new ConfigPartSettings.ProxyCapability();
        cap.traitNameFilter("item_slot");
        cap.autoIO().setEnable(true);
        cap.autoIO().setInterval(1);
        cap.autoIO().setTopIO(io);
        return cap;
    }
}

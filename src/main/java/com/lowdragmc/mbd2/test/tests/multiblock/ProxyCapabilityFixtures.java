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
 * Fixtures for proxy-capability scenarios:
 * - {@link #PART_PROXY_CONTROLLER_ID}: controller with an item-slot trait + a part whose
 *   {@link ConfigPartSettings#proxyControllerCapabilities()} forwards the controller's item handler.
 * - {@link #PROXY_BLOCK_CONTROLLER_ID}: controller with an item-slot trait + a plain-stone slot in the
 *   pattern marked with {@code proxyWhileFormed} carrying {@code proxyCapabilities}, so the resulting
 *   {@code ProxyPartBlockEntity} exposes the controller's item handler.
 * - {@link #STACKED_CONTROLLER_ID}: like the part case but the predicate ALSO carries proxyCapabilities,
 *   so the part exposes both its static {@code partSettings.proxyControllerCapabilities} and the
 *   predicate-derived caps stacked together.
 */
public class ProxyCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation PART_PROXY_CONTROLLER_ID = MBD2.id("test_proxy_cap_part_controller");
    public static final ResourceLocation PART_PROXY_PART_ID = MBD2.id("test_proxy_cap_part_part");
    public static final ResourceLocation PROXY_BLOCK_CONTROLLER_ID = MBD2.id("test_proxy_cap_block_controller");
    public static final ResourceLocation STACKED_CONTROLLER_ID = MBD2.id("test_proxy_cap_stacked_controller");
    public static final ResourceLocation STACKED_PART_ID = MBD2.id("test_proxy_cap_stacked_part");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // Part with no item slots of its own; proxies the controller's "item_slot" trait via partSettings.
        var part = TestMachineBuilder.simple(PART_PROXY_PART_ID)
                .withPartSettings(ps -> {
                    var proxy = new ConfigPartSettings.ProxyCapability();
                    proxy.traitNameFilter("item_slot");
                    ps.proxyControllerCapabilities().add(proxy);
                })
                .register(event);

        // Controller: owns 1 input + 1 output item slot. Part proxies these.
        TestMachineBuilder.multiblock(PART_PROXY_CONTROLLER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(part.block()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        // Controller: pattern requires a stone block at the 'P' slot marked proxyWhileFormed
        // with proxyCapabilities filtering the controller's "item_slot" trait.
        TestMachineBuilder.multiblock(PROXY_BLOCK_CONTROLLER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(Blocks.IRON_BLOCK).proxyWhileFormed(proxy -> {
                            var cap = new ConfigPartSettings.ProxyCapability();
                            cap.traitNameFilter("item_slot");
                            proxy.getProxyCapabilities().add(cap);
                        }))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        // Stacked: a part with its OWN partSettings.proxyControllerCapabilities, matched by a
        // predicate that ALSO carries proxyCapabilities; both should contribute (same filter).
        var stackedPart = TestMachineBuilder.simple(STACKED_PART_ID)
                .withPartSettings(ps -> {
                    var proxy = new ConfigPartSettings.ProxyCapability();
                    proxy.traitNameFilter("item_slot");
                    ps.proxyControllerCapabilities().add(proxy);
                })
                .register(event);

        TestMachineBuilder.multiblock(STACKED_CONTROLLER_ID)
                .withItemSlots(1, IO.IN)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(stackedPart.block()).proxyWhileFormed(proxy -> {
                            var cap = new ConfigPartSettings.ProxyCapability();
                            cap.traitNameFilter("item_slot");
                            proxy.getProxyCapabilities().add(cap);
                        }))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}

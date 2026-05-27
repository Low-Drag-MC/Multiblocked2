package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap entry point for the MBD2 test framework. {@link com.lowdragmc.mbd2.common.CommonProxy}
 * registers an instance of this class on the mod event bus when the game is running in a dev
 * environment (see {@code Platform.isDevEnv()}). Test fixture providers add themselves via
 * {@link #register(TestFixtureProvider)}; their {@code registerMachines} / {@code registerRecipeTypes}
 * methods are invoked on the corresponding registry events.
 */
public class MBDTestRegistry {

    private static final List<TestFixtureProvider> PROVIDERS = new ArrayList<>();

    /** Register a fixture provider. Safe to call from a static initializer. */
    public static synchronized void register(TestFixtureProvider provider) {
        PROVIDERS.add(provider);
    }

    @SubscribeEvent
    public void onRegisterMachines(MBDRegistryEvent.Machine event) {
        for (TestFixtureProvider provider : PROVIDERS) {
            provider.registerMachines(event);
        }
    }

    @SubscribeEvent
    public void onRegisterRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        for (TestFixtureProvider provider : PROVIDERS) {
            provider.registerRecipeTypes(event);
        }
    }
}

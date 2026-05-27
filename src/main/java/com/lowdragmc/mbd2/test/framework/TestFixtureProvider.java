package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;

/**
 * Implemented by classes that want to register test-only machines and recipe types.
 * Register your provider via {@link MBDTestRegistry#register(TestFixtureProvider)} in a
 * static initializer of your test class.
 */
public interface TestFixtureProvider {

    /** Register any test machine definitions on the event. */
    default void registerMachines(MBDRegistryEvent.Machine event) {}

    /** Register any test recipe types (and their built-in recipes) on the event. */
    default void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {}
}

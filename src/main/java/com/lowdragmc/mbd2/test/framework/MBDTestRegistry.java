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

    public static void init() {
        // force-load the smoke fixture class so its static initializer runs and
        // registers itself with MBDTestRegistry before MBDRegistryEvent fires.
        register(new com.lowdragmc.mbd2.test.tests.MBDSmokeFixtures());
        register(new com.lowdragmc.mbd2.test.tests.recipe.ItemRecipeCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.recipe.ItemDurabilityRecipeCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.recipe.FluidRecipeCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.recipe.ForgeEnergyRecipeCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.recipe.EntityRecipeCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.trait.ItemSlotTraitFixtures());
        register(new com.lowdragmc.mbd2.test.tests.trait.FluidTankTraitFixtures());
        register(new com.lowdragmc.mbd2.test.tests.trait.ForgeEnergyTraitFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternBasicsFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternPredicatesFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternRepetitionFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.MultiblockWithPartsFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternRotationFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternSnapshotFixtures());
        if (net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.ChemicalTankTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.trait.MekHeatTraitFixtures());
        }
        if (net.neoforged.fml.ModList.get().isLoaded("naturesaura")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.naturesaura.AuraHandlerTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.recipe.naturesaura.NaturesAuraRecipeCapabilityFixtures());
        }
        if (net.neoforged.fml.ModList.get().isLoaded("pneumaticcraft")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft.PNCHeatTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft.PNCPressureTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft.PNCHeatRecipeCapabilityFixtures());
            register(new com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft.PNCPressureAirRecipeCapabilityFixtures());
        }
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.create.CreateKineticMachineFixtures());
        }
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

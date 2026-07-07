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
        register(new com.lowdragmc.mbd2.test.tests.trait.AutoIOTraitFixtures());
        register(new com.lowdragmc.mbd2.test.tests.trait.ForgeEnergyTraitFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternBasicsFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternPredicatesFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternRepetitionFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.MultiblockWithPartsFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.ProxyCapabilityFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternRotationFixtures());
        register(new com.lowdragmc.mbd2.test.tests.multiblock.PatternSnapshotFixtures());
        if (net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.ChemicalTankTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.trait.MekHeatTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.recipe.mekanism.ChemicalRecipeCapabilityFixtures());
        }
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            register(new com.lowdragmc.mbd2.test.tests.trait.ae2.MEInterfaceTraitFixtures());
            register(new com.lowdragmc.mbd2.test.tests.trait.ae2.MEPatternProviderTraitFixtures());
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

    /**
     * Registers game test classes that reference soft-dependency mods. These classes are deliberately
     * NOT annotated with {@code @GameTestHolder}: NeoForge's annotation scan force-loads every
     * {@code @GameTestHolder} class ({@code Class.forName} + {@code getDeclaredMethods}) regardless of
     * the enabled namespaces, which throws {@code NoClassDefFoundError} when the referenced mod is
     * absent. Registering them here means the class literal is only resolved inside the matching
     * {@code ModList.isLoaded(...)} branch, so an absent mod never triggers class loading.
     */
    @SubscribeEvent
    public void onRegisterGameTests(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
        var modList = net.neoforged.fml.ModList.get();
        if (modList.isLoaded("mekanism")) {
            event.register(com.lowdragmc.mbd2.test.tests.trait.ChemicalTankTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.MekHeatTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.mekanism.ChemicalRecipeCapabilityTests.class);
        }
        if (modList.isLoaded("ae2")) {
            event.register(com.lowdragmc.mbd2.test.tests.trait.ae2.MEInterfaceTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.ae2.MEPatternProviderTraitTests.class);
        }
        if (modList.isLoaded("naturesaura")) {
            event.register(com.lowdragmc.mbd2.test.tests.trait.naturesaura.AuraHandlerTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.naturesaura.NaturesAuraRecipeCapabilityTests.class);
        }
        if (modList.isLoaded("pneumaticcraft")) {
            event.register(com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft.PNCHeatTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft.PNCPressureTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft.PNCHeatRecipeCapabilityTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft.PNCPressureAirRecipeCapabilityTests.class);
        }
        if (modList.isLoaded("create")) {
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.KineticRotationPropagationTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateMachineStateRendererTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.KineticBESerializationTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateMachineDefinitionTypeTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateRotationTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateEditorProjectRegistrationTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateMandatoryTraitTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateDynamicTorqueTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateRotationConditionTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.trait.create.CreateCogwheelInputTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.create.CreateRotationRecipeCapabilityTests.class);
            event.register(com.lowdragmc.mbd2.test.tests.recipe.create.CreateRotationContentTests.class);
        }
        if (modList.isLoaded("geckolib")) {
            event.register(com.lowdragmc.mbd2.test.tests.geckolib.GeckolibAnimationConfigTests.class);
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

package com.lowdragmc.mbd2.test.tests.blueprint.mekanism;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismRecipeContentNodes;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.tests.blueprint.PayloadRoundTrip;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine whose blueprint builds a Mekanism chemical payload and reads it back.
 *
 * <p>Registered from {@code MBDTestRegistry} inside a {@code ModList.isLoaded("mekanism")} branch, so
 * the class literal is only resolved when the mod is there — see the note on
 * {@code onRegisterGameTests}, and {@code gradlew runGameTestServer -PnoMekanism} for the run that
 * actually checks it.</p>
 */
public class MekanismPayloadFixtures implements TestFixtureProvider {

    public static final ResourceLocation MACHINE_ID = MBD2.id("blueprint_mek_payload_round_trip");
    /** The amount asked for. Small enough to survive being read as a redstone signal, and not a default. */
    public static final int AMOUNT = 11;
    /** Vanilla Mekanism, and the chemical its own capability defaults to, so the id is certainly registered. */
    public static final ResourceLocation CHEMICAL = ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withBlueprint(PayloadRoundTrip.graph(MekanismChemicalRecipeCapability.CAP.name,
                        MekanismRecipeContentNodes.ChemicalIngredientOf.class, "ingredient",
                        maker -> {
                            KGGameTestHelpers.setInputConstant(maker, "chemical", CHEMICAL);
                            KGGameTestHelpers.setInputConstant(maker, "amount", (long) AMOUNT);
                        },
                        MekanismRecipeContentNodes.ChemicalIngredientInfo.class, "ingredient",
                        "amount", null))
                .register(event);
    }
}

package com.lowdragmc.mbd2.test.tests.blueprint.mekanism;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.tests.blueprint.BlueprintBehaviourTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Mekanism's chemical payload pair, driven through the generic recipe-content pipeline.
 *
 * <p>No {@code @GameTestHolder}: NeoForge force-loads every class carrying it, and this one names a
 * Mekanism type through its fixture. {@code MBDTestRegistry.onRegisterGameTests} registers it inside
 * the mod check instead.</p>
 */
public class MekanismPayloadTests {
    static { @SuppressWarnings("unused") var ignored = MekanismPayloadFixtures.MACHINE_ID; }


    /**
     * Chemical Ingredient Of builds a payload the capability accepts, its codec survives NBT, and
     * Chemical Ingredient Info reads the amount back.
     *
     * <p>This is the capability whose payload MBD2 cannot type by hand — the one the whole
     * capability-generic design was supposed to reach — so it is the one worth driving end to end.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void chemicalPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        BlueprintBehaviourTests.assertSignal(helper, MekanismPayloadFixtures.MACHINE_ID,
                MekanismPayloadFixtures.AMOUNT);
    }
}

package com.lowdragmc.mbd2.test.tests.blueprint.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.tests.blueprint.BlueprintBehaviourTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * PneumaticCraft's pressure/air payload pair, driven through the generic recipe-content pipeline.
 *
 * <p>No {@code @GameTestHolder} — registered from {@code MBDTestRegistry} inside the mod check, for
 * the reason documented there.</p>
 */
public class PNCPayloadTests {
    static { @SuppressWarnings("unused") var ignored = PNCPayloadFixtures.MACHINE_ID; }


    /**
     * Pressure Air Of builds a payload, its codec survives NBT, and Pressure Air Info reads both
     * fields back.
     *
     * <p>Four can only appear if {@code isAir} also came back true — the fixture gates the signal on
     * it — so one number covers the pair.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pressureAirPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        BlueprintBehaviourTests.assertSignal(helper, PNCPayloadFixtures.MACHINE_ID,
                PNCPayloadFixtures.VALUE);
    }
}

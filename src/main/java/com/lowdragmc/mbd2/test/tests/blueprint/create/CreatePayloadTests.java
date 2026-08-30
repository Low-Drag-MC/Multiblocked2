package com.lowdragmc.mbd2.test.tests.blueprint.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.tests.blueprint.BlueprintBehaviourTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create's rotation payload pair, driven through the generic recipe-content pipeline.
 *
 * <p>No {@code @GameTestHolder} — registered from {@code MBDTestRegistry} inside the mod check, for
 * the reason documented there.</p>
 */
public class CreatePayloadTests {
    static { @SuppressWarnings("unused") var ignored = CreatePayloadFixtures.VALUE_MACHINE_ID; }


    /**
     * Rotation Of builds a payload, its codec survives NBT, and Rotation Info reads the amount back.
     *
     * <p>Seven can only appear if the torque override also came back enabled — the fixture gates the
     * signal on it — so one number covers the amount and the {@code ToggleFloat} beside it.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void rotationPayloadSurvivesTheRoundTrip(GameTestHelper helper) {
        BlueprintBehaviourTests.assertSignal(helper, CreatePayloadFixtures.VALUE_MACHINE_ID,
                CreatePayloadFixtures.VALUE);
    }

    /**
     * The mode survives too, which the amount test cannot show.
     *
     * <p>A dropped mode leaves the amount untouched and turns RPM into the stress default — the same
     * number meaning a different thing, which is exactly the failure a number-based assertion misses.
     * The fixture builds two rotations with different modes and requires the reader to tell them
     * apart; see it for why "different" rather than "the same" is the assertion that holds up.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void rotationModeSurvivesTheRoundTrip(GameTestHelper helper) {
        BlueprintBehaviourTests.assertSignal(helper, CreatePayloadFixtures.MODE_MACHINE_ID,
                CreatePayloadFixtures.MODE_MATCHED);
    }
}

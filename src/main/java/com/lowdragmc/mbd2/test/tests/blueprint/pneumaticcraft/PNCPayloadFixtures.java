package com.lowdragmc.mbd2.test.tests.blueprint.pneumaticcraft;

import com.lowdragmc.kilagraph.test.gametest.KGGameTestHelpers;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCRecipeContentNodes;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.tests.blueprint.PayloadRoundTrip;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine whose blueprint builds a PneumaticCraft pressure/air payload and reads it back.
 *
 * <p>The signal is gated on {@code isAir}, which is how the boolean gets asserted at all: it has no
 * number of its own, and it is the field that decides what {@code value} even means. A codec that
 * dropped it would leave the value intact and the test would pass on a payload that had quietly
 * become a pressure.</p>
 */
public class PNCPayloadFixtures implements TestFixtureProvider {

    public static final ResourceLocation MACHINE_ID = MBD2.id("blueprint_pnc_payload_round_trip");
    /** The amount asked for; small enough to read as a redstone signal and not a default. */
    public static final int VALUE = 4;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withBlueprint(PayloadRoundTrip.graph(PNCPressureAirRecipeCapability.CAP.name,
                        PNCRecipeContentNodes.PressureAirOf.class, "pressureAir",
                        maker -> {
                            KGGameTestHelpers.setInputConstant(maker, "isAir", true);
                            KGGameTestHelpers.setInputConstant(maker, "value", (float) VALUE);
                        },
                        PNCRecipeContentNodes.PressureAirInfo.class, "pressureAir",
                        "value", "isAir"))
                .register(event);
    }
}

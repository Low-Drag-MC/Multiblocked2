package com.lowdragmc.mbd2.test.tests.trait.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class PNCPressureTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_pnc_pressure_machine");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var def = new PNCPressureAirHandlerTraitDefinition();
        def.setVolume(2000);
        def.setMaxPressure(10f);
        def.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(def)
                .register(event);
    }
}

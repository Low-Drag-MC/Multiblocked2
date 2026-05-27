package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class MekHeatTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_mek_heat_machine");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var def = new MekHeatCapabilityTraitDefinition();
        def.setHeatCapacity(1000);
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(def)
                .register(event);
    }
}

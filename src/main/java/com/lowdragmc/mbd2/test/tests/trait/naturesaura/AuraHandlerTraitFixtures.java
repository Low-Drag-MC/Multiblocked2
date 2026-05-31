package com.lowdragmc.mbd2.test.tests.trait.naturesaura;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.naturesaura.trait.AuraHandlerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class AuraHandlerTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_aura_handler_machine");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var def = new AuraHandlerTraitDefinition();
        def.setRadius(20);
        def.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(def)
                .register(event);
    }
}

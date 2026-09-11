package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class ChemicalTankTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_chemical_tank_machine");
    public static final ResourceLocation AUTO_INPUT_ID = MBD2.id("test_chemical_auto_input");
    public static final ResourceLocation AUTO_OUTPUT_ID = MBD2.id("test_chemical_auto_output");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var def = new ChemicalTankCapabilityTraitDefinition();
        def.setTankSize(2);
        def.setCapacity(16_000);
        def.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(def)
                .register(event);

        TestMachineBuilder.simple(AUTO_INPUT_ID).withTrait(autoIODefinition(IO.IN)).register(event);
        TestMachineBuilder.simple(AUTO_OUTPUT_ID).withTrait(autoIODefinition(IO.OUT)).register(event);
    }

    /** Auto IO on the machine's right side, same geometry the item and fluid fixtures use. */
    private static ChemicalTankCapabilityTraitDefinition autoIODefinition(IO io) {
        var def = new ChemicalTankCapabilityTraitDefinition();
        def.setTankSize(1);
        def.setCapacity(16_000);
        def.setRecipeHandlerIO(IO.BOTH);
        def.getAutoIO().setEnable(true);
        def.getAutoIO().setInterval(1);
        def.getAutoIO().setRightIO(io);
        return def;
    }
}

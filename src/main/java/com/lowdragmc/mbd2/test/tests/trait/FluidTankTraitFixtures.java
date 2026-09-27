package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

public class FluidTankTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_fluid_tank_machine");
    /** One tank and the UI generated from it, with the trait's GUI IO set to IN / OUT / BOTH. */
    public static final ResourceLocation GUI_IN_MACHINE_ID = MBD2.id("test_fluid_tank_gui_in");
    public static final ResourceLocation GUI_OUT_MACHINE_ID = MBD2.id("test_fluid_tank_gui_out");
    public static final ResourceLocation GUI_BOTH_MACHINE_ID = MBD2.id("test_fluid_tank_gui_both");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withFluidTanks(2, 16000, def -> def.setRecipeHandlerIO(IO.BOTH))
                .register(event);
        guiMachine(event, GUI_IN_MACHINE_ID, IO.IN);
        guiMachine(event, GUI_OUT_MACHINE_ID, IO.OUT);
        guiMachine(event, GUI_BOTH_MACHINE_ID, IO.BOTH);
    }

    private static void guiMachine(MBDRegistryEvent.Machine event, ResourceLocation id, IO guiIO) {
        TestMachineBuilder.simple(id)
                .withFluidTanks(1, 16000, def -> def.setGuiIO(guiIO))
                .withTraitUI()
                .register(event);
    }
}

package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class PNCPressureAirRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_pnc_pressure_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_pnc_pressure_cap_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // 1 dirt -> 500 mL air output
                .recipe("pnc_pressure_dirt_to_air", b -> b
                        .inputItems(Items.DIRT)
                        .output(PNCPressureAirRecipeCapability.CAP, new PressureAir(true, 500))
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var pressureDef = new PNCPressureAirHandlerTraitDefinition();
        pressureDef.setVolume(2000);
        pressureDef.setMaxPressure(10f);
        pressureDef.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(pressureDef)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }
}

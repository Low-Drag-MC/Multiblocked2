package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCHeatExchangerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class PNCHeatRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_pnc_heat_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_pnc_heat_cap_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // 1 dirt -> 100 heat (output side)
                .recipe("pnc_heat_dirt_to_heat", b -> b
                        .inputItems(Items.DIRT)
                        .output(PNCHeatRecipeCapability.CAP, 100d)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var heatDef = new PNCHeatExchangerTraitDefinition();
        heatDef.setThermalCapacity(1000);
        heatDef.setThermalResistance(1);
        heatDef.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(heatDef)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }
}

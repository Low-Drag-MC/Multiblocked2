package com.lowdragmc.mbd2.test.tests.recipe.naturesaura;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.integration.naturesaura.trait.AuraHandlerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class NaturesAuraRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_aura_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_aura_cap_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // 1 dirt -> 100 aura (output side)
                .recipe("aura_dirt_to_aura", b -> b
                        .inputItems(Items.DIRT)
                        .output(NaturesAuraRecipeCapability.CAP, 100)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var auraDef = new AuraHandlerTraitDefinition();
        auraDef.setRadius(20);
        auraDef.setRecipeHandlerIO(IO.BOTH);
        TestMachineBuilder.simple(MACHINE_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(auraDef)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }
}

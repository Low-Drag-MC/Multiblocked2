package com.lowdragmc.mbd2.test.tests.recipe.arsnouveau;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.arsnouveau.ArsSourceRecipeCapability;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class ArsSourceRecipeCapabilityFixtures implements TestFixtureProvider {
    /** Spends source out of its own buffer: 1 dirt + 500 source -> 1 cobblestone. */
    public static final ResourceLocation CONSUMER_ID = MBD2.id("test_ars_source_cap_consumer");
    /** Fills its own buffer: 1 dirt -> 500 source. */
    public static final ResourceLocation PRODUCER_ID = MBD2.id("test_ars_source_cap_producer");
    public static final ResourceLocation CONSUMER_RECIPES = MBD2.id("test_ars_source_cap_consume_recipes");
    public static final ResourceLocation PRODUCER_RECIPES = MBD2.id("test_ars_source_cap_produce_recipes");

    public static final int CAPACITY = 10000;
    public static final int COST = 500;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(CONSUMER_RECIPES)
                .recipe("source_to_cobble", b -> b
                        .inputItems(Items.DIRT)
                        .inputs(ArsSourceRecipeCapability.CAP, COST)
                        .outputItems(Items.COBBLESTONE)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(PRODUCER_RECIPES)
                .recipe("dirt_to_source", b -> b
                        .inputItems(Items.DIRT)
                        .outputs(ArsSourceRecipeCapability.CAP, COST)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(CONSUMER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withTrait(storage())
                .withRecipeType(CONSUMER_RECIPES)
                .register(event);
        TestMachineBuilder.simple(PRODUCER_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(storage())
                .withRecipeType(PRODUCER_RECIPES)
                .register(event);
    }

    private static SourceStorageCapabilityTraitDefinition storage() {
        var def = new SourceStorageCapabilityTraitDefinition();
        def.setCapacity(CAPACITY);
        def.setMaxReceive(CAPACITY);
        def.setMaxExtract(CAPACITY);
        def.setRecipeHandlerIO(IO.BOTH);
        return def;
    }
}

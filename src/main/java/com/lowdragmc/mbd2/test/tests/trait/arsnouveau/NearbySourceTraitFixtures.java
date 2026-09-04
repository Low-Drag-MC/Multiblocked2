package com.lowdragmc.mbd2.test.tests.trait.arsnouveau;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.arsnouveau.ArsSourceRecipeCapability;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.NearbySourceTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class NearbySourceTraitFixtures implements TestFixtureProvider {
    /** Spends source from the jars around it: 1 dirt + 300 source -> 1 cobblestone. */
    public static final ResourceLocation CONSUMER_ID = MBD2.id("test_ars_nearby_source_consumer");
    /** Fills the jars around it: 1 dirt -> 300 source. */
    public static final ResourceLocation PRODUCER_ID = MBD2.id("test_ars_nearby_source_producer");
    public static final ResourceLocation CONSUMER_RECIPES = MBD2.id("test_ars_nearby_consume_recipes");
    public static final ResourceLocation PRODUCER_RECIPES = MBD2.id("test_ars_nearby_produce_recipes");

    public static final int RADIUS = 2;
    public static final int COST = 300;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(CONSUMER_RECIPES)
                .recipe("nearby_source_to_cobble", b -> b
                        .inputItems(Items.DIRT)
                        .inputs(ArsSourceRecipeCapability.CAP, COST)
                        .outputItems(Items.COBBLESTONE)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(PRODUCER_RECIPES)
                .recipe("dirt_to_nearby_source", b -> b
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
                .withTrait(nearby(def -> def.setRecipeHandlerIO(IO.IN)))
                .withRecipeType(CONSUMER_RECIPES)
                .register(event);
        TestMachineBuilder.simple(PRODUCER_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(nearby(def -> def.setRecipeHandlerIO(IO.OUT)))
                .withRecipeType(PRODUCER_RECIPES)
                .register(event);
    }

    private static NearbySourceTraitDefinition nearby(Consumer<NearbySourceTraitDefinition> tweak) {
        var def = new NearbySourceTraitDefinition();
        // small radius so a test never reaches the machine of the test running beside it, and a scan
        // every tick so a test does not have to wait out an interval to see the world it just built
        def.setRadius(RADIUS);
        def.setScanInterval(1);
        // Ars Nouveau's source orb is a real entity; a test does not need dozens of them
        def.setParticles(false);
        tweak.accept(def);
        return def;
    }
}

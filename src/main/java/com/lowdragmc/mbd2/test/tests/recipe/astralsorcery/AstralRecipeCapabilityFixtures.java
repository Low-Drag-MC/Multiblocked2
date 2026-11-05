package com.lowdragmc.mbd2.test.tests.recipe.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.AstralLumenRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTraitDefinition;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.AstralStarlightRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightAmount;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import com.lowdragmc.mbd2.test.tests.trait.astralsorcery.LumenStorageTraitFixtures;
import com.lowdragmc.mbd2.test.tests.trait.astralsorcery.StarlightReceiverTraitFixtures;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Content is built from ids: this runs during mod construction, before Astral Sorcery's registries are filled. */
public class AstralRecipeCapabilityFixtures implements TestFixtureProvider {
    /** 1 dirt + 500 aevitas -> 1 cobblestone. */
    public static final ResourceLocation LUMEN_CONSUMER_ID = MBD2.id("test_astral_lumen_consumer");
    /** 1 dirt -> 500 armara. */
    public static final ResourceLocation LUMEN_PRODUCER_ID = MBD2.id("test_astral_lumen_producer");
    /** 1 dirt + 50 armara starlight -> 1 cobblestone. */
    public static final ResourceLocation STARLIGHT_CONSUMER_ID = MBD2.id("test_astral_starlight_consumer");
    /** 1 gravel + 40 starlight of any constellation -> 1 sand. */
    public static final ResourceLocation ANY_STARLIGHT_CONSUMER_ID = MBD2.id("test_astral_any_starlight_consumer");
    /** A researcher trait and nothing else, for the condition tests. */
    public static final ResourceLocation RESEARCHER_ID = MBD2.id("test_astral_researcher");

    public static final ResourceLocation LUMEN_CONSUME_RECIPES = MBD2.id("test_astral_lumen_consume_recipes");
    public static final ResourceLocation LUMEN_PRODUCE_RECIPES = MBD2.id("test_astral_lumen_produce_recipes");
    public static final ResourceLocation STARLIGHT_CONSUME_RECIPES = MBD2.id("test_astral_starlight_consume_recipes");
    public static final ResourceLocation ANY_STARLIGHT_CONSUME_RECIPES = MBD2.id("test_astral_any_starlight_consume_recipes");

    public static final int LUMEN_COST = 500;
    public static final float ARMARA_STARLIGHT_COST = 50;
    public static final float ANY_STARLIGHT_COST = 40;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(LUMEN_CONSUME_RECIPES)
                .recipe("aevitas_to_cobble", b -> b
                        .inputItems(Items.DIRT)
                        .inputs(AstralLumenRecipeCapability.CAP, new LumenAmount(LumenAS.AEVITAS.getId(), LUMEN_COST))
                        .outputItems(Items.COBBLESTONE)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(LUMEN_PRODUCE_RECIPES)
                .recipe("dirt_to_armara", b -> b
                        .inputItems(Items.DIRT)
                        .outputs(AstralLumenRecipeCapability.CAP, new LumenAmount(LumenAS.ARMARA.getId(), LUMEN_COST))
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(STARLIGHT_CONSUME_RECIPES)
                .recipe("armara_to_cobble", b -> b
                        .inputItems(Items.DIRT)
                        .inputs(AstralStarlightRecipeCapability.CAP, StarlightAmount.of(ConstellationsAS.ARMARA.getId(), ARMARA_STARLIGHT_COST))
                        .outputItems(Items.COBBLESTONE)
                        .duration(20))
                .register(event);
        TestRecipeTypeBuilder.of(ANY_STARLIGHT_CONSUME_RECIPES)
                .recipe("any_to_sand", b -> b
                        .inputItems(Items.GRAVEL)
                        .inputs(AstralStarlightRecipeCapability.CAP, StarlightAmount.any(ANY_STARLIGHT_COST))
                        .outputItems(Items.SAND)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(LUMEN_CONSUMER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withTrait(LumenStorageTraitFixtures.storage(def -> {}))
                .withRecipeType(LUMEN_CONSUME_RECIPES)
                .register(event);
        TestMachineBuilder.simple(LUMEN_PRODUCER_ID)
                .withItemSlots(1, IO.IN)
                .withTrait(LumenStorageTraitFixtures.storage(def -> {}))
                .withRecipeType(LUMEN_PRODUCE_RECIPES)
                .register(event);
        TestMachineBuilder.simple(STARLIGHT_CONSUMER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withTrait(StarlightReceiverTraitFixtures.receiver(def -> {}))
                .withRecipeType(STARLIGHT_CONSUME_RECIPES)
                .register(event);
        TestMachineBuilder.simple(ANY_STARLIGHT_CONSUMER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withTrait(StarlightReceiverTraitFixtures.receiver(def -> {}))
                .withRecipeType(ANY_STARLIGHT_CONSUME_RECIPES)
                .register(event);
        TestMachineBuilder.simple(RESEARCHER_ID)
                .withTrait(new AstralResearcherTraitDefinition())
                .register(event);
    }
}

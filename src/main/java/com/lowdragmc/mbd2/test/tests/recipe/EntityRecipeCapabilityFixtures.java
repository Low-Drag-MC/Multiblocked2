package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.EntityRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.entity.EntityHandlerTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

public class EntityRecipeCapabilityFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_entity_cap_machine");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_entity_cap_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // entity in (pig) -> item out (dirt)
                .recipe("entity_cap_pig_to_dirt", b -> b
                        .input(EntityRecipeCapability.CAP, EntityIngredient.of(1, EntityType.PIG))
                        .outputItems(Items.DIRT)
                        .duration(20))
                // item in (cobblestone) -> entity out (chicken). Use cobblestone so it doesn't
                // conflict with other recipe types that consume dirt.
                .recipe("entity_cap_cobble_to_chicken", b -> b
                        .inputItems(Items.COBBLESTONE)
                        .output(EntityRecipeCapability.CAP, EntityIngredient.of(1, EntityType.CHICKEN))
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var entityIn = new EntityHandlerTraitDefinition();
        entityIn.setName("entity_handler_in");
        entityIn.setRecipeHandlerIO(IO.IN);

        var entityOut = new EntityHandlerTraitDefinition();
        entityOut.setName("entity_handler_out");
        entityOut.setRecipeHandlerIO(IO.OUT);

        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(entityIn)
                .withTrait(entityOut)
                .withItemSlots(1, IO.IN)   // slot 0: cobblestone in (for entity-output recipe)
                .withItemSlots(1, IO.OUT)  // slot 1: dirt out (for entity-input recipe)
                .withRecipeType(RECIPE_TYPE_ID)
                .register(event);
    }
}

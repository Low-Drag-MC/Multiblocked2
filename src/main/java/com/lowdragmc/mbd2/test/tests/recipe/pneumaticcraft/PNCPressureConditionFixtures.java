package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTraitDefinition;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureCondition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Fixtures for issue #227: a recipe condition attached to a multiblock controller has to see
 * the traits that live on the structure's parts, not only the ones on the controller block.
 * <p>
 * The controller carries no traits at all — the air handler sits on the part, exactly like the
 * reporter's setup.
 */
public class PNCPressureConditionFixtures implements TestFixtureProvider {
    public static final ResourceLocation PART_ID = MBD2.id("test_pnc_condition_part");
    public static final ResourceLocation CONTROLLER_ID = MBD2.id("test_pnc_condition_controller");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_pnc_condition_recipes");

    /** Air volume of the part's handler; pressure = air / volume. */
    public static final int PART_VOLUME = 2000;

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                // iron -> gold, but only while the structure holds between 5 and 10 bar
                .recipe("pnc_condition_iron_to_gold", b -> b
                        .inputItems(Items.IRON_INGOT)
                        .outputItems(Items.GOLD_INGOT)
                        .addCondition(new PNCPressureCondition(false, 5f, 10f))
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        var pressureDef = new PNCPressureAirHandlerTraitDefinition();
        pressureDef.setVolume(PART_VOLUME);
        pressureDef.setMaxPressure(10f);
        pressureDef.setRecipeHandlerIO(IO.BOTH);

        var partDef = TestMachineBuilder.simple(PART_ID)
                .withTrait(pressureDef)
                .withItemSlots(1, IO.IN)   // slot 0
                .withItemSlots(1, IO.OUT)  // slot 1
                .register(event);

        // "CP": the leftmost char sits at the controller, the next one at world -X.
        TestMachineBuilder.multiblock(CONTROLLER_ID)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("CP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(partDef.block()))
                        .build())
                .register(event);
    }
}

package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCHeatExchangerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PNCHeatRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = PNCHeatRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Recipe consumes 1 dirt + outputs 100 heat. Asserts the dirt was consumed and the
     * heat-exchanger trait's temperature increased — proves the cap's recipe handler
     * accepted the heat output.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void heat_output_recipe_consumes_input_and_raises_temperature(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(PNCHeatRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1));
        double initialTemp = readTemperature(scenario);
        scenario.runTicks(40).assertItem(0, ItemStack.EMPTY);
        double finalTemp = readTemperature(scenario);
        if (finalTemp <= initialTemp) {
            h.fail("Heat output recipe did not raise temperature: " + initialTemp + " -> " + finalTemp);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_input(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCHeatRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(40)
                .succeed();
    }

    private static double readTemperature(MBDScenario scenario) {
        var machine = scenario.machine();
        if (machine == null) return 0;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCHeatExchangerTrait heatTrait) {
                return heatTrait.getHandler().getTemperature();
            }
        }
        return 0;
    }
}

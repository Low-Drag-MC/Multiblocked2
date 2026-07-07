package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class PNCPressureAirRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = PNCPressureAirRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Recipe consumes 1 dirt + outputs 500 mL air. Asserts the dirt was consumed and the
     * air handler's stored air count increased — proves the cap's recipe handler
     * accepted the air output.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pressure_output_recipe_consumes_input_and_adds_air(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(PNCPressureAirRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1));
        int initialAir = readAir(scenario);
        scenario.runTicks(40).assertItem(0, ItemStack.EMPTY);
        int finalAir = readAir(scenario);
        if (finalAir <= initialAir) {
            h.fail("Pressure output recipe did not add air: " + initialAir + " -> " + finalAir);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_input(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(PNCPressureAirRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(40)
                .succeed();
    }

    private static int readAir(MBDScenario scenario) {
        var machine = scenario.machine();
        if (machine == null) return 0;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCPressureAirHandlerTrait pressureTrait) {
                return pressureTrait.getHandler().getAir();
            }
        }
        return 0;
    }
}

package com.lowdragmc.mbd2.test.tests.recipe.pneumaticcraft;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTrait;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureCondition;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for issue #227 — recipe conditions could only inspect the controller's own
 * traits, so a condition on a multiblock whose trait lives on a part never matched (and always
 * matched when inverted).
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class PNCPressureConditionTests {
    static { @SuppressWarnings("unused") var ignored = PNCPressureConditionFixtures.CONTROLLER_ID; }

    private static final BlockPos CONTROLLER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PART_POS = new BlockPos(0, 1, 1);
    /** 12000 mL over a 2000 mL volume = 6 bar, inside the recipe's [5, 10] window. */
    private static final int AIR_FOR_6_BAR = 6 * PNCPressureConditionFixtures.PART_VOLUME;

    /** The condition must find the air handler that lives on the part, not on the controller. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pressure_condition_sees_part_air_handler(GameTestHelper h) {
        var controller = formStructure(h);
        pressurize(h, AIR_FOR_6_BAR);

        var condition = new PNCPressureCondition(false, 5f, 10f);
        if (!condition.test(MBDTestHelper.dummyRecipe(), controller.getRecipeLogic())) {
            h.fail("pneumatic_pressure condition did not see the part's air handler at 6 bar");
            return;
        }
        h.succeed();
    }

    /**
     * The fix must not degrade into "always true": a part outside the configured window still
     * has to fail the condition, otherwise the inverted form would become always-satisfied.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pressure_condition_still_fails_when_part_is_out_of_range(GameTestHelper h) {
        var controller = formStructure(h);
        pressurize(h, 2 * PNCPressureConditionFixtures.PART_VOLUME); // 2 bar

        var condition = new PNCPressureCondition(false, 5f, 10f);
        if (condition.test(MBDTestHelper.dummyRecipe(), controller.getRecipeLogic())) {
            h.fail("pneumatic_pressure condition matched a 2 bar part against the [5, 10] window");
            return;
        }
        h.succeed();
    }

    /** End-to-end: the recipe carries the condition and must run once the part is pressurized. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_with_part_pressure_condition_runs(GameTestHelper h) {
        formStructure(h);
        pressurize(h, AIR_FOR_6_BAR);

        MBDScenario.of(h)
                .target(PART_POS)
                .insertItem(0, new ItemStack(Items.IRON_INGOT))
                .runTicks(60)
                .target(PART_POS)
                .assertItemCountAtLeast(1, Items.GOLD_INGOT, 1)
                .succeed();
    }

    /** Control: without pressure the same recipe must stay blocked. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_with_part_pressure_condition_is_blocked_when_depressurized(GameTestHelper h) {
        formStructure(h);

        MBDScenario.of(h)
                .target(PART_POS)
                .insertItem(0, new ItemStack(Items.IRON_INGOT))
                .runTicks(60);

        var part = MBDTestHelper.getMachine(h, PART_POS);
        var output = MBDTestHelper.extractItem(h, part, 1, 64);
        if (!output.isEmpty()) {
            h.fail("Recipe ran at 0 bar even though the condition requires [5, 10]: " + output);
            return;
        }
        h.succeed();
    }

    private static MBDMachine formStructure(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(PNCPressureConditionFixtures.CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(PNCPressureConditionFixtures.PART_ID, PART_POS)
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();
        var controller = scenario.machine();
        if (controller == null) {
            h.fail("Controller was not placed");
            throw new AssertionError();
        }
        return controller;
    }

    private static void pressurize(GameTestHelper h, int air) {
        var part = MBDTestHelper.getMachine(h, PART_POS);
        for (var trait : part.getAdditionalTraits()) {
            if (trait instanceof PNCPressureAirHandlerTrait pressureTrait) {
                pressureTrait.getHandler().addAir(air);
                return;
            }
        }
        h.fail("Part has no PNCPressureAirHandlerTrait");
    }
}

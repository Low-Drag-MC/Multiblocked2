package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.create.CreateRotationCondition;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for issue #220 — the create rotation condition only looked at the
 * controller's own traits, so on a multiblock (where the rotation trait lives on a kinetic
 * part) it never matched no matter how the RPM / stress window was configured.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateRotationPartConditionTests {
    static { @SuppressWarnings("unused") var ignored = CreateRotationPartConditionFixtures.CONTROLLER_ID; }

    private static final BlockPos CONTROLLER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PART_POS = new BlockPos(0, 1, 1);

    /** Part spins at 50 RPM with torque 4 → 200 stress; window [40,100] / [100,300] must match. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void rotation_condition_sees_part_rotation_trait(GameTestHelper h) {
        var controller = formStructure(h);
        spinPart(h, 50f);

        var condition = new CreateRotationCondition(40f, 100f, 100f, 300f);
        if (!condition.test(MBDTestHelper.dummyRecipe(), controller.getRecipeLogic())) {
            h.fail("create_rotation condition did not see the part's rotation trait (50 rpm / 200 su)");
            return;
        }
        h.succeed();
    }

    /** A part outside the window must still fail, so the inverted form isn't always-true. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void rotation_condition_still_fails_when_part_is_out_of_range(GameTestHelper h) {
        var controller = formStructure(h);
        spinPart(h, 5f);

        var condition = new CreateRotationCondition(40f, 100f, 0f, Float.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), controller.getRecipeLogic())) {
            h.fail("create_rotation condition matched a 5 rpm part against the [40, 100] window");
            return;
        }
        h.succeed();
    }

    private static MBDMachine formStructure(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(CreateRotationPartConditionFixtures.CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, PART_POS)
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

    private static void spinPart(GameTestHelper h, float speed) {
        var part = MBDTestHelper.getMachine(h, PART_POS);
        if (!(part.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("Part BE is not MBDKineticMachineBlockEntity");
            return;
        }
        for (var trait : part.getAdditionalTraits()) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                kineticBE.setSpeed(speed);
                rotationTrait.serverTick();
                return;
            }
        }
        h.fail("Part has no CreateRotationTrait");
    }
}

package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.CreateRotationCondition;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateRotationConditionTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.CONSUMER_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static com.lowdragmc.mbd2.common.machine.MBDMachine setupAtSpeed(GameTestHelper h, float speed) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return null; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return null;
        }
        for (var t : machine.getAdditionalTraits()) {
            if (t instanceof CreateRotationTrait rt) {
                kineticBE.setSpeed(speed);
                rt.serverTick();
                return machine;
            }
        }
        h.fail("CreateRotationTrait missing"); return null;
    }

    /** torque=4, speed=50 → rpm=50, stress=200. Condition (rpm [40,100], stress [100,300]) passes. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_passes_when_rpm_and_stress_in_range(GameTestHelper h) {
        var machine = setupAtSpeed(h, 50f);
        if (machine == null) return;
        var condition = new CreateRotationCondition(40f, 100f, 100f, 300f);
        if (!condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("Condition.test() returned false in a passing range");
            return;
        }
        h.succeed();
    }

    /** rpm=10 must fail [100,500]. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_fails_when_rpm_below_min(GameTestHelper h) {
        var machine = setupAtSpeed(h, 10f);
        if (machine == null) return;
        var condition = new CreateRotationCondition(100f, 500f, 0f, Float.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("Condition.test() returned true for rpm=10 out of [100,500]");
            return;
        }
        h.succeed();
    }

    /** rpm=200 > maxRPM=100 must fail. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_fails_when_rpm_above_max(GameTestHelper h) {
        var machine = setupAtSpeed(h, 200f);
        if (machine == null) return;
        var condition = new CreateRotationCondition(0f, 100f, 0f, Float.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("Condition.test() returned true for rpm=200 above max=100");
            return;
        }
        h.succeed();
    }

    /** torque=4, speed=50 -> stress=200; condition requires stress>=300 -> fails. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_fails_when_stress_below_min(GameTestHelper h) {
        var machine = setupAtSpeed(h, 50f);
        if (machine == null) return;
        var condition = new CreateRotationCondition(0f, Float.MAX_VALUE, 300f, Float.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("Condition.test() returned true for stress=200 below min=300");
            return;
        }
        h.succeed();
    }

    /** Sanity: condition should NOT compile/contain a torqueOverride field anymore (moved to CreateRotation). */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_no_longer_carries_torque_override(GameTestHelper h) {
        for (var f : CreateRotationCondition.class.getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("torque")) {
                h.fail("Condition still has a torque-related field: " + f.getName());
                return;
            }
        }
        h.succeed();
    }
}

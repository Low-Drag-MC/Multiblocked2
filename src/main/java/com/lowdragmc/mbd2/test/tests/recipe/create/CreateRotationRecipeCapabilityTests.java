package com.lowdragmc.mbd2.test.tests.recipe.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.tests.trait.create.CreateKineticMachineFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Sanity checks for the unified rotation cap: registration name, default content shape,
 * deep-copy semantics, generator scheduleWorking clamp.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateRotationRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.GENERATOR_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void cap_is_registered_with_correct_name(GameTestHelper h) {
        if (!"create_rotation".equals(CreateRotationRecipeCapability.CAP.name)) {
            h.fail("Cap name is " + CreateRotationRecipeCapability.CAP.name + ", expected create_rotation");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void default_content_is_stress_with_disabled_override(GameTestHelper h) {
        var def = CreateRotationRecipeCapability.CAP.createDefaultContent();
        if (def == null) { h.fail("createDefaultContent returned null"); return; }
        if (def.mode != CreateRotation.Mode.STRESS) { h.fail("Default mode should be STRESS, got " + def.mode); return; }
        if (def.torqueOverride == null || def.torqueOverride.isEnable()) {
            h.fail("Default torqueOverride should be disabled");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void copy_preserves_mode_and_override(GameTestHelper h) {
        var original = new CreateRotation(99f, CreateRotation.Mode.RPM, ToggleFloat.of(true, 42f));
        var copy = original.copy();
        if (copy.value != 99f) { h.fail("copy.value=" + copy.value); return; }
        if (copy.mode != CreateRotation.Mode.RPM) { h.fail("copy.mode=" + copy.mode); return; }
        if (!copy.torqueOverride.isEnable() || copy.torqueOverride.getValue() != 42f) {
            h.fail("copy.torqueOverride=" + copy.torqueOverride.isEnable() + "/" + copy.torqueOverride.getValue());
            return;
        }
        // Mutating original.torqueOverride must not affect copy (deep copy).
        original.torqueOverride.setEnable(false);
        if (!copy.torqueOverride.isEnable()) {
            h.fail("Copy was not isolated from original torqueOverride mutation");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void generator_schedule_working_clamps_at_max(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        // torque=8, maxRPM=256, so max possible stress = 256*8 = 2048
        float available = kineticBE.scheduleWorking(1000f, true);
        if (available <= 0) { h.fail("scheduleWorking returned non-positive: " + available); return; }
        float over = kineticBE.scheduleWorking(99999f, true);
        if (over > 2048f + 0.1f) {
            h.fail("scheduleWorking should clamp at maxRPM*capacity but got " + over);
            return;
        }
        h.succeed();
    }
}

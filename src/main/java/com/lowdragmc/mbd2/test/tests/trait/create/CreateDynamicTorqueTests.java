package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.integration.create.machine.MBDKineticMachineBlockEntity;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards the per-recipe torque-override flow: when an input/output {@link CreateRotation}
 * carries an enabled {@link ToggleFloat}, {@link CreateRotationTrait#preWorking} pushes that
 * value into the BE; {@link CreateRotationTrait#postWorking} clears it. The override is now
 * carried by the recipe-cap content, not by {@code CreateRotationCondition}.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateDynamicTorqueTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.CONSUMER_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static CreateRotationTrait traitOf(MBDKineticMachineBlockEntity be, GameTestHelper h) {
        if (!(be.getMetaMachine() instanceof com.lowdragmc.mbd2.common.machine.MBDMachine machine)) {
            h.fail("BE has no MBDMachine"); return null;
        }
        for (var t : machine.getAdditionalTraits()) {
            if (t instanceof CreateRotationTrait rt) return rt;
        }
        h.fail("CreateRotationTrait missing"); return null;
    }

    /** Build a fake recipe whose IO map contains a single CreateRotation content. */
    private static MBDRecipe mockRecipe(IO io, CreateRotation content) {
        Map<RecipeCapability<?>, List<Content>> inputs = new HashMap<>();
        Map<RecipeCapability<?>, List<Content>> outputs = new HashMap<>();
        var list = new ArrayList<Content>();
        list.add(new Content(content, false, 1f, 0f));
        (io == IO.IN ? inputs : outputs).put(CreateRotationRecipeCapability.CAP, list);
        return new MBDRecipe(null, MBD2.id("dummy"), inputs, outputs, new ArrayList<>(), new CompoundTag(), 1, false, 0);
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void default_torque_used_when_no_override(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        float impact = kineticBE.calculateStressApplied();
        if (impact != 4f) { h.fail("Expected impact=4 (consumer torque), got " + impact); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void disabled_toggle_does_not_override(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var trait = traitOf(kineticBE, h);
        if (trait == null) return;

        // ToggleFloat present but disabled: should NOT override.
        var content = new CreateRotation(32f, CreateRotation.Mode.RPM, ToggleFloat.of(false, 99f));
        trait.preWorking(IO.IN, mockRecipe(IO.IN, content));

        float impact = kineticBE.calculateStressApplied();
        if (impact != 4f) { h.fail("Expected impact=4 (toggle disabled), got " + impact); return; }
        if (trait.getTorque() != 4f) { h.fail("Trait torque should stay 4, got " + trait.getTorque()); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void enabled_toggle_applies_override_on_input(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var trait = traitOf(kineticBE, h);
        if (trait == null) return;

        var content = new CreateRotation(32f, CreateRotation.Mode.RPM, ToggleFloat.of(true, 16f));
        trait.preWorking(IO.IN, mockRecipe(IO.IN, content));

        float impact = kineticBE.calculateStressApplied();
        if (impact != 16f) { h.fail("Expected impact=16 after override, got " + impact); return; }
        if (trait.getTorque() != 16f) { h.fail("Trait torque should be 16, got " + trait.getTorque()); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void enabled_toggle_applies_override_on_output(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var trait = traitOf(kineticBE, h);
        if (trait == null) return;

        var content = new CreateRotation(256f, CreateRotation.Mode.STRESS, ToggleFloat.of(true, 12f));
        trait.preWorking(IO.OUT, mockRecipe(IO.OUT, content));

        // Generator-mode: override applies to capacity (calculateAddedStressCapacity).
        float capacity = kineticBE.calculateAddedStressCapacity();
        if (capacity != 12f) { h.fail("Expected capacity=12 after override, got " + capacity); return; }
        if (trait.getTorque() != 12f) { h.fail("Trait torque should be 12, got " + trait.getTorque()); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void override_restored_after_post_working(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var trait = traitOf(kineticBE, h);
        if (trait == null) return;

        var content = new CreateRotation(32f, CreateRotation.Mode.RPM, ToggleFloat.of(true, 16f));
        var recipe = mockRecipe(IO.IN, content);
        trait.preWorking(IO.IN, recipe);
        if (kineticBE.calculateStressApplied() != 16f) {
            h.fail("Precondition failed: override not applied"); return;
        }
        trait.postWorking(IO.IN, recipe);

        float impact = kineticBE.calculateStressApplied();
        if (impact != 4f) { h.fail("Expected impact=4 after postWorking, got " + impact); return; }
        if (trait.getTorque() != 4f) { h.fail("Trait torque not restored, got " + trait.getTorque()); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void stress_mode_override_also_applies(GameTestHelper h) {
        // Even with mode=STRESS, the override toggle works the same — semantics depend on the
        // toggle, not on the mode.
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE)) {
            h.fail("BE is not MBDKineticMachineBlockEntity"); return;
        }
        var trait = traitOf(kineticBE, h);
        if (trait == null) return;

        var content = new CreateRotation(128f, CreateRotation.Mode.STRESS, ToggleFloat.of(true, 7.5f));
        trait.preWorking(IO.IN, mockRecipe(IO.IN, content));

        float impact = kineticBE.calculateStressApplied();
        if (impact != 7.5f) { h.fail("Expected impact=7.5, got " + impact); return; }
        h.succeed();
    }
}

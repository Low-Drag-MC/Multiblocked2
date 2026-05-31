package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards that {@link CreateRotationTrait.CreateRotationTraitDefinition} is treated as a
 * mandatory + non-multiple trait: auto-included by definition, can't be added twice, and
 * always available on every Create kinetic machine after construction.
 */
@GameTestHolder(MBD2.MOD_ID)
public class CreateMandatoryTraitTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.GENERATOR_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Sanity: the definition's static contract reports mandatory + !allowMultiple. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void definition_is_mandatory_and_not_multiple(GameTestHelper h) {
        var def = CreateRotationTrait.DEFINITION;
        if (!def.isMandatory()) { h.fail("isMandatory() should be true"); return; }
        if (def.allowMultiple()) { h.fail("allowMultiple() should be false"); return; }
        h.succeed();
    }

    /**
     * After definition.loadFactory() runs (as part of registry init for every Create kinetic
     * machine), the trait must be in {@code machineSettings.traitDefinitions()} exactly once.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void load_factory_auto_includes_trait_definition(GameTestHelper h) {
        var fresh = CreateKineticMachineDefinition.createDefault();
        fresh.loadFactory();
        long count = fresh.machineSettings().traitDefinitions().stream()
                .filter(t -> t instanceof CreateRotationTrait.CreateRotationTraitDefinition).count();
        if (count != 1) {
            h.fail("Expected exactly 1 CreateRotationTraitDefinition after loadFactory, found " + count);
            return;
        }
        h.succeed();
    }

    /** Calling loadFactory twice must not double-add the mandatory trait. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void load_factory_is_idempotent(GameTestHelper h) {
        var fresh = CreateKineticMachineDefinition.createDefault();
        fresh.loadFactory();
        fresh.loadFactory();
        fresh.loadFactory();
        long count = fresh.machineSettings().traitDefinitions().stream()
                .filter(t -> t instanceof CreateRotationTrait.CreateRotationTraitDefinition).count();
        if (count != 1) {
            h.fail("loadFactory not idempotent: " + count + " CreateRotation traits");
            return;
        }
        h.succeed();
    }

    /**
     * End-to-end: place a fixture machine, confirm the trait is on
     * {@code machine.getAdditionalTraits()} exactly once even though the fixture never adds
     * it manually (this is what catches a regression to the old setMachine-injection path).
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void placed_machine_has_exactly_one_create_rotation_trait(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        long count = machine.getAdditionalTraits().stream()
                .filter(t -> t instanceof CreateRotationTrait).count();
        if (count != 1) {
            h.fail("Expected exactly 1 CreateRotationTrait on the placed machine, found " + count);
            return;
        }
        h.succeed();
    }
}

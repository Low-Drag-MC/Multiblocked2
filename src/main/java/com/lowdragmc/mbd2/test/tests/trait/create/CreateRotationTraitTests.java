package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateRotationTraitTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineFixtures.GENERATOR_MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void generator_machine_injects_rotation_trait(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.GENERATOR_MACHINE_ID, POS)
                .machine();
        if (machine == null) {
            h.fail("Machine was not placed");
            return;
        }
        boolean foundTrait = false;
        boolean foundRotationHandler = false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                foundTrait = true;
                if (!rotationTrait.isGenerator()) {
                    h.fail("Expected generator-mode trait but isGenerator()=false");
                    return;
                }
                for (var handler : rotationTrait.getRecipeHandlerTraits()) {
                    if (handler.getRecipeCapability() == CreateRotationRecipeCapability.CAP) foundRotationHandler = true;
                    if (handler.getHandlerIO() != IO.OUT) {
                        h.fail("Generator handler should have IO.OUT, got " + handler.getHandlerIO());
                        return;
                    }
                }
            }
        }
        if (!foundTrait) { h.fail("CreateRotationTrait was not injected"); return; }
        if (!foundRotationHandler) { h.fail("RotationRecipeHandler not present"); return; }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void consumer_machine_injects_rotation_trait(GameTestHelper h) {
        var machine = MBDScenario.of(h)
                .placeMachine(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID, POS)
                .machine();
        if (machine == null) { h.fail("Machine was not placed"); return; }
        boolean foundTrait = false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                foundTrait = true;
                if (rotationTrait.isGenerator()) { h.fail("Expected consumer-mode trait but isGenerator()=true"); return; }
                for (var handler : rotationTrait.getRecipeHandlerTraits()) {
                    if (handler.getHandlerIO() != IO.IN) {
                        h.fail("Consumer handler should have IO.IN, got " + handler.getHandlerIO());
                        return;
                    }
                }
            }
        }
        if (!foundTrait) { h.fail("CreateRotationTrait was not injected"); return; }
        h.succeed();
    }
}

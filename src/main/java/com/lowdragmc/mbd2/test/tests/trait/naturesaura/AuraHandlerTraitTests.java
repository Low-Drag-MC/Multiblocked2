package com.lowdragmc.mbd2.test.tests.trait.naturesaura;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.integration.naturesaura.trait.AuraHandlerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class AuraHandlerTraitTests {
    static { @SuppressWarnings("unused") var ignored = AuraHandlerTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void aura_trait_registers_recipe_handler(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(AuraHandlerTraitFixtures.MACHINE_ID, POS);
        var machine = MBDScenario.of(h).target(POS).machine();
        if (machine == null) {
            h.fail("Machine was not placed");
            return;
        }
        boolean found = false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof AuraHandlerTrait auraTrait) {
                for (var handler : auraTrait.getRecipeHandlerTraits()) {
                    if (handler.getRecipeCapability() == NaturesAuraRecipeCapability.CAP) {
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            h.fail("AuraHandlerTrait did not register a NaturesAuraRecipeCapability recipe handler");
            return;
        }
        h.succeed();
    }

    /**
     * How far this machine reaches for aura, per machine rather than per definition.
     *
     * <p>Asserted through {@code radiusBlocks()} rather than the slot: both readers — the recipe handler
     * and the aura readout the definition binds into the machine's UI — go through it, so this is the
     * one place a reader could still be consulting the definition instead.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void radius_override_replaces_the_definition_value(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(AuraHandlerTraitFixtures.MACHINE_ID, POS)
                .check("the radius starts on the definition", m -> auraTrait(m).radiusBlocks() == 20)
                .with(m -> auraTrait(m).radius.set(5))
                .check("an override should win", m -> auraTrait(m).radiusBlocks() == 5)
                .assertPersistenceRoundTrip()
                .check("and survive a save/load cycle", m -> auraTrait(m).radiusBlocks() == 5)
                .with(m -> auraTrait(m).radius.clear())
                .check("clearing should go back to the definition", m -> auraTrait(m).radiusBlocks() == 20)
                .succeed();
    }

    private static AuraHandlerTrait auraTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof AuraHandlerTrait auraTrait) return auraTrait;
        }
        throw new AssertionError("fixture machine has no aura handler trait");
    }
}

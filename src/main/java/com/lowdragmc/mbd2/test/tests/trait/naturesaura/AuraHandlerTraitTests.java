package com.lowdragmc.mbd2.test.tests.trait.naturesaura;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.integration.naturesaura.trait.AuraHandlerTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class AuraHandlerTraitTests {
    static { @SuppressWarnings("unused") var ignored = AuraHandlerTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
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
}

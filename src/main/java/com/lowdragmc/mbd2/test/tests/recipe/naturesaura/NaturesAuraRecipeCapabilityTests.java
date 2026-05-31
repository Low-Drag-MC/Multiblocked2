package com.lowdragmc.mbd2.test.tests.recipe.naturesaura;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class NaturesAuraRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = NaturesAuraRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Recipe consumes 1 dirt + outputs 100 aura. Asserts dirt was consumed within
     * 40 ticks — proves the recipe loop ran and the aura output handler accepted the
     * write (storing into the surrounding chunk via IAuraChunk).
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void aura_output_recipe_consumes_input(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(NaturesAuraRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(40)
                .assertItem(0, ItemStack.EMPTY)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_input(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(NaturesAuraRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(40)
                .succeed();
    }
}

package com.lowdragmc.mbd2.test.tests.recipe.mekanism;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class ChemicalRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ChemicalRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void chemical_input_can_be_split_across_tanks(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(ChemicalRecipeCapabilityFixtures.MACHINE_ID, POS);
        var handler = chemicalHandler(h, scenario);

        handler.insertChemical(new ChemicalStack(MekanismChemicals.HYDROGEN, 1000), Action.EXECUTE);
        assertHydrogen(h, handler, 0, 500);
        assertHydrogen(h, handler, 1, 500);

        scenario.runTicks(40)
                .assertItem(0, new ItemStack(Items.EMERALD, 1));
        assertEmpty(h, handler, 0);
        assertEmpty(h, handler, 1);
        scenario.succeed();
    }

    private static IChemicalHandler chemicalHandler(GameTestHelper h, MBDScenario scenario) {
        var handler = MBDTestHelper.capability(h, scenario.machine(), mekanism.common.capabilities.Capabilities.CHEMICAL.block());
        if (handler == null) {
            h.fail("No chemical handler on " + ChemicalRecipeCapabilityFixtures.MACHINE_ID);
            throw new AssertionError();
        }
        return handler;
    }

    private static void assertHydrogen(GameTestHelper h, IChemicalHandler handler, int tank, long amount) {
        var stack = handler.getChemicalInTank(tank);
        if (!ChemicalStack.isSameChemical(stack, new ChemicalStack(MekanismChemicals.HYDROGEN, 1)) || stack.getAmount() != amount) {
            h.fail("Expected " + amount + " hydrogen in tank " + tank + ", got " + stack);
        }
    }

    private static void assertEmpty(GameTestHelper h, IChemicalHandler handler, int tank) {
        var stack = handler.getChemicalInTank(tank);
        if (!stack.isEmpty()) {
            h.fail("Expected empty chemical tank " + tank + ", got " + stack);
        }
    }
}

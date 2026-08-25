package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class ForgeEnergyRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ForgeEnergyRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fe_total_drain_runs(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ForgeEnergyRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertEnergy(10_000)
                // poll rather than budget a fixed 40 ticks: recipe searching is async and only re-polls
                // every 5 ticks, so a fixed budget races the background thread
                .runUntil(m -> MBDTestHelper.readItem(h, m, 0).is(Items.DIRT), 200)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_energy(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ForgeEnergyRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(40)
                .assertItem(0, ItemStack.EMPTY)
                .succeed();
    }
}

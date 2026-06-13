package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestHolder(MBD2.MOD_ID)
public class FluidRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = FluidRecipeCapabilityFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_input_produces_item_output(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 4000))
                .runTicks(40)
                .assertItemCountAtLeast(0, net.minecraft.world.item.Items.DIRT, 1)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_input_produces_fluid_output(GameTestHelper h) {
        // lava → water recipe puts output in the OUT tank (index 1)
        MBDScenario.of(h)
                .placeMachine(FluidRecipeCapabilityFixtures.MACHINE_ID, POS)
                .insertFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 2000))
                .runTicks(40)
                .assertFluid(1, new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void sized_fluid_input_can_be_split_across_tanks(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidRecipeCapabilityFixtures.SPLIT_TANK_MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 1000))
                .assertFluid(0, new FluidStack(Fluids.WATER, 500))
                .assertFluid(1, new FluidStack(Fluids.WATER, 500))
                .runTicks(40)
                .assertFluid(0, FluidStack.EMPTY)
                .assertFluid(1, FluidStack.EMPTY)
                .assertItem(0, new ItemStack(Items.EMERALD, 1))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void no_recipe_without_fluid(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(FluidRecipeCapabilityFixtures.MACHINE_ID, POS)
                .runTicks(40)
                .assertItem(0, ItemStack.EMPTY)
                .succeed();
    }
}

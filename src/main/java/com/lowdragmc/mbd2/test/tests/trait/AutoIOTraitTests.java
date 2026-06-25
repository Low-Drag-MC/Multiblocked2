package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class AutoIOTraitTests {
    static { @SuppressWarnings("unused") var ignored = AutoIOTraitFixtures.ITEM_AUTO_INPUT; }

    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = MACHINE.relative(Direction.EAST);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void item_auto_input_pulls_from_adjacent_inventory(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, ItemSlotTraitFixtures.MACHINE_ID, EAST);
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));
        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.ITEM_AUTO_INPUT, MACHINE, Direction.NORTH)
                .runTicks(2)
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 12))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void item_auto_output_pushes_to_adjacent_inventory(GameTestHelper h) {
        MBDTestHelper.placeMachine(h, ItemSlotTraitFixtures.MACHINE_ID, EAST);
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.ITEM_AUTO_OUTPUT, MACHINE, Direction.NORTH)
                .insertItem(0, new ItemStack(Items.GOLD_INGOT, 9))
                .runTicks(2);
        assertItemAt(h, EAST, Items.GOLD_INGOT, 9);
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_auto_input_pulls_from_adjacent_tank(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, FluidTankTraitFixtures.MACHINE_ID, EAST);
        MBDTestHelper.insertFluid(h, source, new FluidStack(Fluids.WATER, 2000));
        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.FLUID_AUTO_INPUT, MACHINE, Direction.NORTH)
                .runTicks(2)
                .assertFluid(0, new FluidStack(Fluids.WATER, 2000))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_auto_output_pushes_to_adjacent_tank(GameTestHelper h) {
        MBDTestHelper.placeMachine(h, FluidTankTraitFixtures.MACHINE_ID, EAST);
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.FLUID_AUTO_OUTPUT, MACHINE, Direction.NORTH)
                .insertFluid(new FluidStack(Fluids.LAVA, 2000))
                .runTicks(2);
        assertFluidAt(h, EAST, Fluids.LAVA, 2000);
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void item_world_input_collects_item_entity(GameTestHelper h) {
        var absolute = h.absolutePos(EAST);
        var item = new ItemEntity(h.getLevel(), absolute.getX() + 0.5, absolute.getY() + 0.25,
                absolute.getZ() + 0.5, new ItemStack(Items.DIAMOND, 7));
        h.getLevel().addFreshEntity(item);
        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.ITEM_WORLD_INPUT, MACHINE, Direction.NORTH)
                .runTicks(2)
                .assertItem(0, new ItemStack(Items.DIAMOND, 7))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void item_world_output_spawns_item_entity(GameTestHelper h) {
        h.setBlock(EAST, Blocks.AIR.defaultBlockState());
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.ITEM_WORLD_OUTPUT, MACHINE, Direction.NORTH)
                .insertItem(0, new ItemStack(Items.EMERALD, 6))
                .runTicks(2);
        var count = h.getLevel().getEntitiesOfClass(ItemEntity.class, absoluteBox(h, EAST),
                        entity -> entity.getItem().is(Items.EMERALD))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        if (count < 6) {
            h.fail("Expected world output to spawn 6 emeralds, got " + count);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_world_input_collects_source_block_from_input_range(GameTestHelper h) {
        h.setBlock(EAST, Blocks.WATER.defaultBlockState());
        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.FLUID_WORLD_INPUT, MACHINE, Direction.NORTH)
                .runTicks(2)
                .assertFluid(0, new FluidStack(Fluids.WATER, 1000));
        if (!h.getBlockState(EAST).isAir()) {
            h.fail("Fluid world input did not remove the water source");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_world_output_places_source_block(GameTestHelper h) {
        h.setBlock(EAST, Blocks.AIR.defaultBlockState());
        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.FLUID_WORLD_OUTPUT, MACHINE, Direction.NORTH)
                .insertFluid(new FluidStack(Fluids.WATER, 1000))
                .runTicks(2);
        if (!h.getBlockState(EAST).getFluidState().isSource()
                || !h.getBlockState(EAST).getFluidState().is(Fluids.WATER)) {
            h.fail("Fluid world output did not place a water source, got " + h.getBlockState(EAST));
            return;
        }
        h.succeed();
    }

    private static void assertItemAt(GameTestHelper h, BlockPos pos, net.minecraft.world.item.Item item, int count) {
        var handler = MBDTestHelper.capability(h, pos, Capabilities.ItemHandler.BLOCK, null);
        if (handler == null || !handler.getStackInSlot(0).is(item) || handler.getStackInSlot(0).getCount() < count) {
            h.fail("Expected " + count + " " + item + " in adjacent inventory");
        }
    }

    private static void assertFluidAt(GameTestHelper h, BlockPos pos, net.minecraft.world.level.material.Fluid fluid, int amount) {
        var handler = MBDTestHelper.capability(h, pos, Capabilities.FluidHandler.BLOCK, null);
        if (handler == null || !handler.getFluidInTank(0).is(fluid) || handler.getFluidInTank(0).getAmount() < amount) {
            h.fail("Expected " + amount + " mB of " + fluid + " in adjacent tank");
        }
    }

    private static AABB absoluteBox(GameTestHelper h, BlockPos relative) {
        return new AABB(h.absolutePos(relative)).inflate(0.25);
    }
}

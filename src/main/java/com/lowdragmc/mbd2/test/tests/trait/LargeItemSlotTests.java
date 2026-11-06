package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.LargeItemStackTransfer;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.SyncMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Item slots holding more than a stack: {@link LargeItemStackTransfer} and what {@link ItemSlotCapabilityTrait} does
 * with it. The menu clicks are in {@link LargeItemSlotMenuTests}.
 */
@GameTestHolder(MBD2.MOD_ID)
public class LargeItemSlotTests {
    static { @SuppressWarnings("unused") var ignored = LargeItemSlotFixtures.LARGE; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = MACHINE.relative(Direction.EAST);
    private static final int LIMIT = LargeItemSlotFixtures.LIMIT;

    // region limits

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void toggle_off_keeps_items_to_their_own_stack_size(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.CAPPED);
        if (!expectCount(h, "the remainder of 100 iron", insert(h, machine, 0, stack(Items.IRON_INGOT, 100)), 36)) return;
        storage(machine).setStackInSlot(0, ItemStack.EMPTY);
        if (!expectCount(h, "the remainder of 20 ender pearls", insert(h, machine, 0, stack(Items.ENDER_PEARL, 20)), 4)) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slot_holds_up_to_its_limit(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        if (!expectCount(h, "the remainder of 5000 iron", insert(h, machine, 0, stack(Items.IRON_INGOT, 5000)), 0)) return;
        if (!expectCount(h, "the remainder of another 6000", insert(h, machine, 0, stack(Items.IRON_INGOT, 6000)), 1000)) return;
        if (!expectStack(h, "the slot", storage(machine).getStackInSlot(0), stack(Items.IRON_INGOT, LIMIT))) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void items_with_a_smaller_stack_size_reach_the_limit_too(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        if (!expectCount(h, "the remainder of 12000 ender pearls", insert(h, machine, 1, stack(Items.ENDER_PEARL, 12_000)), 2000)) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unstackable_items_stay_one_per_slot(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        if (!expectCount(h, "the remainder of 3 swords", insert(h, machine, 0, stack(Items.DIAMOND_SWORD, 3)), 2)) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void extraction_hands_out_a_stack_at_a_time(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        insert(h, machine, 0, stack(Items.IRON_INGOT, 5000));
        insert(h, machine, 1, stack(Items.ENDER_PEARL, 5000));
        if (!expectCount(h, "1000 iron asked for", MBDTestHelper.extractItem(h, machine, 0, 1000), 64)) return;
        if (!expectCount(h, "100 ender pearls asked for", MBDTestHelper.extractItem(h, machine, 1, 100), 16)) return;
        if (!expectStack(h, "the iron left", storage(machine).getStackInSlot(0), stack(Items.IRON_INGOT, 4936))) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_negative_extraction_takes_nothing(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.HUGE);
        var full = stack(Items.IRON_INGOT, Integer.MAX_VALUE);
        storage(machine).setStackInSlot(0, full.copy());
        if (!expectStack(h, "extracting -5", storage(machine).extractItem(0, -5, false), ItemStack.EMPTY)) return;
        if (!expectStack(h, "extracting -5 through the capability", MBDTestHelper.extractItem(h, machine, 0, -5), ItemStack.EMPTY)) return;
        if (!expectStack(h, "the slot", storage(machine).getStackInSlot(0), full)) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void ignore_item_stack_limit_override_applies_immediately(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.CAPPED);
        trait(machine).ignoreItemStackLimit.set(true);
        if (!expectCount(h, "the remainder of 1000 iron with the override on", insert(h, machine, 0, stack(Items.IRON_INGOT, 1000)), 0)) return;
        trait(machine).ignoreItemStackLimit.clear();
        if (!expectCount(h, "the remainder of 1 more with the override cleared", insert(h, machine, 0, stack(Items.IRON_INGOT, 1)), 1)) return;
        h.succeed();
    }

    // endregion

    // region saving

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void large_counts_survive_persistence(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.LARGE, POS);
        var named = named(stack(Items.GOLD_INGOT, 100));
        insert(h, scenario.machine(), 0, stack(Items.IRON_INGOT, Item.ABSOLUTE_MAX_STACK_SIZE));
        insert(h, scenario.machine(), 1, named);
        scenario.assertPersistenceRoundTrip();
        if (!expectStack(h, "slot 0 after reload", storage(scenario.machine()).getStackInSlot(0),
                stack(Items.IRON_INGOT, Item.ABSOLUTE_MAX_STACK_SIZE))) return;
        if (!expectStack(h, "slot 1 after reload", storage(scenario.machine()).getStackInSlot(1), named)) return;
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_full_int_slot_survives_persistence_and_world_load(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.HUGE, POS);
        var full = stack(Items.IRON_INGOT, Integer.MAX_VALUE);
        if (!expectCount(h, "the remainder of a full int of iron", insert(h, scenario.machine(), 0, full), 0)) return;
        scenario.assertPersistenceRoundTrip();
        if (!expectStack(h, "the slot after reload", storage(scenario.machine()).getStackInSlot(0), full)) return;
        scenario.assertWorldLoadRoundTrip();
        if (!expectStack(h, "the slot after world load", storage(scenario.machine()).getStackInSlot(0), full)) return;
        h.succeed();
    }

    /** Older saves load, and a downgrade still reads every slot within a stack. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slots_within_the_codec_range_save_like_an_item_stack_handler(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        var plain = new ItemStackHandler(3);
        plain.setStackInSlot(0, stack(Items.IRON_INGOT, 64));
        plain.setStackInSlot(2, named(stack(Items.DIAMOND, Item.ABSOLUTE_MAX_STACK_SIZE)));
        var large = transfer(3);
        for (int i = 0; i < 3; i++) {
            large.setStackInSlot(i, plain.getStackInSlot(i).copy());
        }
        if (!large.serializeNBT(provider).equals(plain.serializeNBT(provider))) {
            h.fail("saved " + large.serializeNBT(provider) + ", ItemStackHandler saves " + plain.serializeNBT(provider));
            return;
        }
        var loaded = transfer(1);
        loaded.deserializeNBT(provider, plain.serializeNBT(provider));
        if (loaded.getSlots() != 3) {
            h.fail("an older save of 3 slots loaded as " + loaded.getSlots());
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (!expectStack(h, "slot " + i + " of an older save", loaded.getStackInSlot(i), plain.getStackInSlot(i))) return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void counts_above_the_codec_range_are_saved_apart(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        var large = transfer(2);
        large.setStackInSlot(0, stack(Items.IRON_INGOT, Item.ABSOLUTE_MAX_STACK_SIZE));
        large.setStackInSlot(1, stack(Items.IRON_INGOT, Item.ABSOLUTE_MAX_STACK_SIZE + 1));
        var items = large.serializeNBT(provider).getList("Items", Tag.TAG_COMPOUND);
        if (items.getCompound(0).contains(LargeItemStackTransfer.LARGE_COUNT)) {
            h.fail("a slot within the codec range was saved apart: " + items.getCompound(0));
            return;
        }
        if (items.getCompound(1).getInt(LargeItemStackTransfer.LARGE_COUNT) != Item.ABSOLUTE_MAX_STACK_SIZE + 1) {
            h.fail("a slot above the codec range was not saved apart: " + items.getCompound(1));
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void large_slots_are_compared_without_saving_them(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.LARGE, POS);
        SyncMarks.of(h, storage(scenario.machine()))
                .keeps("a simulated insert past a stack", s -> s.insertItem(0, stack(Items.IRON_INGOT, 500), true))
                .changes("an insert past a stack", s -> s.insertItem(0, stack(Items.IRON_INGOT, 500), false))
                .changes("a large stack grown in place", s -> s.getStackInSlot(0).grow(1000))
                .keeps("a change to a copy", s -> s.copy().insertItem(1, stack(Items.IRON_INGOT, 500), false));
        scenario.assertChangeGetsSaved("an insert past a stack",
                        m -> storage(m).insertItem(1, stack(Items.GOLD_INGOT, 500), false))
                .succeed();
    }

    // endregion

    // region copies and recipes

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void copy_keeps_limits_and_filter(GameTestHelper h) {
        var transfer = new LargeItemStackTransfer(1, () -> 500, () -> true);
        transfer.setFilter(stack -> stack.is(Items.IRON_INGOT));
        var copy = transfer.copy();
        if (copy.isItemValid(0, stack(Items.GOLD_INGOT, 1))) {
            h.fail("the copy dropped the filter");
            return;
        }
        if (!expectCount(h, "the remainder of 600 iron in the copy", copy.insertItem(0, stack(Items.IRON_INGOT, 600), false), 100)) return;
        if (!transfer.getStackInSlot(0).isEmpty()) {
            h.fail("filling the copy filled the original: " + transfer.getStackInSlot(0));
            return;
        }
        var small = traits(place(h, LargeItemSlotFixtures.SMALL_OUTPUT_MACHINE)).get(1);
        if (small.storage.copy().getStackLimit(0, stack(Items.COBBLESTONE, 1)) != 16) {
            h.fail("a copy of a 16 item slot takes " + small.storage.copy().getStackLimit(0, stack(Items.COBBLESTONE, 1)));
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_consumes_more_than_a_stack_from_one_slot(GameTestHelper h) {
        var input = LargeItemSlotFixtures.RECIPE_INPUT;
        var output = LargeItemSlotFixtures.RECIPE_OUTPUT;
        var scenario = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.RECIPE_MACHINE, POS)
                .insertItem(0, stack(Items.IRON_INGOT, input + 100))
                .insertEnergy(10_000)
                .runUntil(m -> MBDTestHelper.readItem(h, m, 1).getCount() >= output, 200);
        var traits = traits(scenario.machine());
        if (!expectStack(h, "the input left", traits.get(0).storage.getStackInSlot(0), stack(Items.IRON_INGOT, 100))) return;
        if (!expectStack(h, "the output", traits.get(1).storage.getStackInSlot(0), stack(Items.GOLD_INGOT, output))) return;
        h.succeed();
    }

    /** The simulation's storage copy used to fall back to 64: the recipe ran and lost the 16 that did not fit. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_output_beyond_the_slot_limit_does_not_run(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.SMALL_OUTPUT_MACHINE, POS)
                .insertItem(0, stack(Items.STONE, 1))
                .insertEnergy(10_000)
                .runTicks(60);
        var traits = traits(scenario.machine());
        if (!expectStack(h, "the input after 60 ticks", traits.get(0).storage.getStackInSlot(0), stack(Items.STONE, 1))) return;
        if (!expectStack(h, "the output after 60 ticks", traits.get(1).storage.getStackInSlot(0), ItemStack.EMPTY)) return;
        traits.get(1).slotLimit.set(64);
        scenario.insertItem(0, stack(Items.STONE, 1))
                .runUntil(m -> traits.get(1).storage.getStackInSlot(0).getCount() >= 32, 200)
                .succeed();
    }

    // endregion

    // region leaving the machine

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void machine_drops_normal_stacks(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        storage(machine).setStackInSlot(0, stack(Items.IRON_INGOT, 150));
        storage(machine).setStackInSlot(1, stack(Items.ENDER_PEARL, 20));
        var drops = new ArrayList<ItemStack>();
        trait(machine).onMachineDrop(null, drops);
        var counts = drops.stream().map(stack -> stack.getItem() + "x" + stack.getCount()).toList();
        var expected = List.of(Items.IRON_INGOT + "x64", Items.IRON_INGOT + "x64", Items.IRON_INGOT + "x22",
                Items.ENDER_PEARL + "x16", Items.ENDER_PEARL + "x4");
        if (!counts.equals(expected)) {
            h.fail("dropped " + counts + ", expected " + expected);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void breaking_the_machine_spawns_normal_stacks(GameTestHelper h) {
        var machine = place(h, LargeItemSlotFixtures.LARGE);
        storage(machine).setStackInSlot(0, stack(Items.IRON_INGOT, 1000));
        // helper.destroyBlock drops nothing
        h.getLevel().destroyBlock(h.absolutePos(POS), true);
        var entities = itemEntities(h, POS, Items.IRON_INGOT);
        var dropped = entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        if (dropped != 1000) {
            h.fail("breaking the machine dropped " + dropped + " of 1000 iron");
            return;
        }
        if (!allNormalStacks(h, entities)) return;
        h.succeed();
    }

    /** It used to drop a copy of the requested amount while extracting at most a stack. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void world_output_drops_what_it_extracted(GameTestHelper h) {
        h.setBlock(EAST, Blocks.AIR.defaultBlockState());
        var scenario = MBDScenario.of(h).placeMachineFacing(LargeItemSlotFixtures.WORLD_OUTPUT, MACHINE, Direction.NORTH);
        var storage = storage(scenario.machine());
        storage.setStackInSlot(0, stack(Items.IRON_INGOT, 1000));
        scenario.runUntil(m -> storage.getStackInSlot(0).getCount() < 1000, 20);
        var entities = itemEntities(h, EAST, Items.IRON_INGOT);
        var dropped = entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        var left = storage.getStackInSlot(0).getCount();
        if (dropped + left != 1000) {
            h.fail("of 1000 iron, " + dropped + " were dropped and " + left + " are left");
            return;
        }
        if (dropped % LargeItemSlotFixtures.WORLD_OUTPUT_SPEED != 0) {
            h.fail("dropped " + dropped + ", not a multiple of the speed " + LargeItemSlotFixtures.WORLD_OUTPUT_SPEED);
            return;
        }
        if (!allNormalStacks(h, entities)) return;
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_output_empties_a_large_slot_into_a_chest(GameTestHelper h) {
        MBDTestHelper.placeChestWithItems(h, EAST);
        var scenario = MBDScenario.of(h).placeMachineFacing(LargeItemSlotFixtures.AUTO_OUTPUT, MACHINE, Direction.NORTH);
        var storage = storage(scenario.machine());
        storage.setStackInSlot(0, stack(Items.IRON_INGOT, 1000));
        scenario.runUntil(m -> chestCount(h) >= 128, 40);
        var inChest = chestCount(h);
        var left = storage.getStackInSlot(0).getCount();
        if (inChest + left != 1000) {
            h.fail("of 1000 iron, " + inChest + " are in the chest and " + left + " are left");
            return;
        }
        for (var stack : MBDTestHelper.readChestItems(h, EAST)) {
            if (stack.getCount() > stack.getMaxStackSize()) {
                h.fail("the chest holds an oversized " + stack);
                return;
            }
        }
        scenario.succeed();
    }

    // endregion

    // region helpers

    private static MBDMachine place(GameTestHelper h, net.minecraft.resources.ResourceLocation id) {
        return MBDScenario.of(h).placeMachine(id, POS).machine();
    }

    private static ItemStack insert(GameTestHelper h, MBDMachine machine, int slot, ItemStack stack) {
        return MBDTestHelper.insertItem(h, machine, slot, stack);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack named(ItemStack stack) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("large item slot test"));
        return stack;
    }

    private static LargeItemStackTransfer transfer(int size) {
        return new LargeItemStackTransfer(size, () -> LIMIT, () -> true);
    }

    static List<ItemSlotCapabilityTrait> traits(MBDMachine machine) {
        var traits = new ArrayList<ItemSlotCapabilityTrait>();
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ItemSlotCapabilityTrait itemTrait) traits.add(itemTrait);
        }
        return traits;
    }

    static ItemSlotCapabilityTrait trait(MBDMachine machine) {
        var traits = traits(machine);
        if (traits.isEmpty()) throw new AssertionError("fixture machine has no item slot trait");
        return traits.getFirst();
    }

    static LargeItemStackTransfer storage(MBDMachine machine) {
        return trait(machine).storage;
    }

    private static List<ItemEntity> itemEntities(GameTestHelper h, BlockPos pos, Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1),
                entity -> entity.getItem().is(item));
    }

    private static boolean allNormalStacks(GameTestHelper h, List<ItemEntity> entities) {
        for (var entity : entities) {
            if (entity.getItem().getCount() > entity.getItem().getMaxStackSize()) {
                h.fail("an item entity holds an oversized " + entity.getItem());
                return false;
            }
        }
        return true;
    }

    private static int chestCount(GameTestHelper h) {
        var count = 0;
        for (var stack : MBDTestHelper.readChestItems(h, EAST)) count += stack.getCount();
        return count;
    }

    static boolean expectCount(GameTestHelper h, String what, ItemStack actual, int expected) {
        if (actual.getCount() != expected) {
            h.fail(what + " was " + actual + ", expected a count of " + expected);
            return false;
        }
        return true;
    }

    static boolean expectStack(GameTestHelper h, String what, ItemStack actual, ItemStack expected) {
        if (!ItemStack.matches(actual, expected)) {
            h.fail(what + " was " + actual + " " + actual.getComponentsPatch() + ", expected " + expected + " " + expected.getComponentsPatch());
            return false;
        }
        return true;
    }

    // endregion
}

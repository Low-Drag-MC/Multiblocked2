package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.LargeItemHandlerSlot;
import com.lowdragmc.mbd2.common.trait.item.LargeItemStackTransfer;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;

/**
 * Every kind of click on a {@link LargeItemHandlerSlot} through the machine's real menu. Each checks that no item was
 * created or lost and the player holds no stack of more than its size; a refused click has a twin on a normal stack.
 */
@GameTestHolder(MBD2.MOD_ID)
public class LargeItemSlotMenuTests {
    static { @SuppressWarnings("unused") var ignored = LargeItemSlotFixtures.LARGE; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int OFFHAND = 40;

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_menu_binds_large_slots(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, ItemStack.EMPTY);
        if (bench == null) return;
        var large = bench.menu.slots.stream().filter(LargeItemHandlerSlot.class::isInstance).count();
        if (large != 2) {
            h.fail("the menu has " + large + " large slots for the trait's 2");
            return;
        }
        var limit = bench.menu.slots.get(bench.large).getMaxStackSize(iron(1));
        if (limit != LargeItemSlotFixtures.LIMIT) {
            h.fail("the slot says it takes " + limit + " iron");
            return;
        }
        h.succeed();
    }

    // region picking up and placing

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void left_click_takes_one_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (bench.expect("carried", bench.menu.getCarried(), iron(64))
                && bench.expect("the slot", bench.slot(), iron(936))
                && bench.conserved(1000)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void right_click_takes_at_most_one_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.click(bench.large, 1, ClickType.PICKUP);
        if (bench.expect("carried", bench.menu.getCarried(), iron(64))
                && bench.expect("the slot", bench.slot(), iron(936))
                && bench.conserved(1000)) h.succeed();
    }

    /** {@code (count + 1) / 2} overflowed and the slot emptied. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void right_click_on_a_full_int_slot_takes_one_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(Integer.MAX_VALUE));
        if (bench == null) return;
        LargeItemSlotTests.trait(bench.machine).slotLimit.set(Integer.MAX_VALUE);
        bench.click(bench.large, 1, ClickType.PICKUP);
        if (!bench.expect("carried", bench.menu.getCarried(), iron(64))
                || !bench.expect("the slot", bench.slot(), iron(Integer.MAX_VALUE - 64))
                || !bench.conserved(Integer.MAX_VALUE)) return;
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (bench.expect("the slot with the stack put back", bench.slot(), iron(Integer.MAX_VALUE))
                && bench.expect("carried", bench.menu.getCarried(), ItemStack.EMPTY)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clicking_with_the_same_item_fills_past_a_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.menu.setCarried(iron(30));
        bench.click(bench.large, 1, ClickType.PICKUP);
        if (!bench.expect("the slot after a right click", bench.slot(), iron(1001))) return;
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (bench.expect("the slot after a left click", bench.slot(), iron(1030))
                && bench.expect("carried", bench.menu.getCarried(), ItemStack.EMPTY)
                && bench.conserved(1030)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clicking_with_another_item_does_not_swap_an_oversized_slot(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.menu.setCarried(gold(10));
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (bench.expect("carried", bench.menu.getCarried(), gold(10))
                && bench.expect("the slot", bench.slot(), iron(1000))
                && bench.conserved(1010)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clicking_with_another_item_still_swaps_a_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(40));
        if (bench == null) return;
        bench.menu.setCarried(gold(10));
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (bench.expect("carried", bench.menu.getCarried(), iron(40))
                && bench.expect("the slot", bench.slot(), gold(10))
                && bench.conserved(50)) h.succeed();
    }

    /** A slot reporting less than it holds used to hand the difference to the cursor, on existing machines too. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clicking_a_slot_over_its_limit_moves_nothing(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        var trait = LargeItemSlotTests.trait(bench.machine);
        trait.slotLimit.set(100);
        bench.menu.setCarried(iron(30));
        bench.click(bench.large, 1, ClickType.PICKUP);
        bench.click(bench.large, 0, ClickType.PICKUP);
        if (!bench.expect("the slot over a lowered limit", bench.slot(), iron(1000))
                || !bench.expect("carried", bench.menu.getCarried(), iron(30))
                || !bench.conserved(1030)) return;

        trait.ignoreItemStackLimit.set(false);
        trait.slotLimit.set(16);
        bench.storage.setStackInSlot(0, iron(64));
        bench.click(bench.large, 1, ClickType.PICKUP);
        bench.click(bench.large, 0, ClickType.PICKUP);
        bench.click(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.large, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.inventorySlot(20), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT);
        if (bench.expect("the 64 over a limit of 16", bench.slot(), iron(64))
                && bench.conserved(94)) h.succeed();
    }

    // endregion

    // region number keys

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void number_key_moves_one_stack_into_an_empty_slot(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.click(bench.large, 0, ClickType.SWAP);
        if (!bench.expect("hotbar slot 0", bench.inventory(0), iron(64))) return;
        bench.click(bench.large, OFFHAND, ClickType.SWAP);
        if (bench.expect("the offhand", bench.inventory(OFFHAND), iron(64))
                && bench.expect("the slot", bench.slot(), iron(872))
                && bench.conserved(1000)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void number_key_leaves_an_occupied_slot_alone(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.player.getInventory().setItem(0, gold(5));
        bench.player.getInventory().setItem(1, iron(5));
        bench.click(bench.large, 0, ClickType.SWAP);
        bench.click(bench.large, 1, ClickType.SWAP);
        if (bench.expect("hotbar slot 0", bench.inventory(0), gold(5))
                && bench.expect("hotbar slot 1", bench.inventory(1), iron(5))
                && bench.expect("the slot", bench.slot(), iron(1000))
                && bench.conserved(1010)) h.succeed();
    }

    /** Vanilla cancels a drag in progress instead of swapping; the guard must not swap either. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void number_key_during_a_drag_only_cancels_it(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.menu.setCarried(iron(64));
        bench.click(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.inventorySlot(20), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.large, 0, ClickType.SWAP);
        if (bench.expect("hotbar slot 0", bench.inventory(0), ItemStack.EMPTY)
                && bench.expect("the slot", bench.slot(), iron(1000))
                && bench.expect("carried", bench.menu.getCarried(), iron(64))
                && bench.conserved(1064)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void number_key_still_swaps_a_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(40));
        if (bench == null) return;
        bench.player.getInventory().setItem(0, gold(5));
        bench.click(bench.large, 0, ClickType.SWAP);
        if (bench.expect("hotbar slot 0", bench.inventory(0), iron(40))
                && bench.expect("the slot", bench.slot(), gold(5))
                && bench.conserved(45)) h.succeed();
    }

    // endregion

    // region shift-click, drag, double click, throw, clone

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void shift_click_moves_out_in_stacks_until_the_inventory_is_full(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(5000));
        if (bench == null) return;
        bench.click(bench.large, 0, ClickType.QUICK_MOVE);
        var main = 0;
        for (int i = 0; i < 36; i++) main += bench.inventory(i).getCount();
        if (main != 36 * 64) {
            h.fail("the inventory took " + main + " iron, it holds " + 36 * 64);
            return;
        }
        if (bench.expect("the slot", bench.slot(), iron(5000 - 36 * 64)) && bench.conserved(5000)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void shift_click_moves_into_a_slot_holding_more_than_a_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.player.getInventory().setItem(9, iron(64));
        bench.click(bench.inventorySlot(9), 0, ClickType.QUICK_MOVE);
        if (bench.expect("the slot", bench.slot(), iron(1064))
                && bench.expect("inventory slot 9", bench.inventory(9), ItemStack.EMPTY)
                && bench.conserved(1064)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void dragging_across_an_oversized_slot_leaves_it_alone(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.menu.setCarried(iron(64));
        bench.click(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.large, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.inventorySlot(20), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(bench.inventorySlot(21), AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        bench.click(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT);
        if (bench.expect("the slot", bench.slot(), iron(1000))
                && bench.expect("inventory slot 20", bench.inventory(20), iron(32))
                && bench.expect("inventory slot 21", bench.inventory(21), iron(32))
                && bench.conserved(1064)) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void double_click_collects_around_an_oversized_slot(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.player.getInventory().setItem(5, iron(20));
        bench.menu.setCarried(iron(10));
        bench.click(bench.inventorySlot(30), 0, ClickType.PICKUP_ALL);
        if (bench.expect("carried", bench.menu.getCarried(), iron(30))
                && bench.expect("the slot", bench.slot(), iron(1000))
                && bench.conserved(1030)) h.succeed();
    }

    /** The mock player's drops never reach the level, so only the slot tells. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void throwing_takes_at_most_one_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.SURVIVAL, iron(1000));
        if (bench == null) return;
        bench.click(bench.large, 1, ClickType.THROW);
        if (!bench.expect("the slot after throwing the stack", bench.slot(), iron(936))) return;
        bench.click(bench.large, 0, ClickType.THROW);
        if (bench.expect("the slot after throwing one", bench.slot(), iron(935))) h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void creative_middle_click_clones_one_stack(GameTestHelper h) {
        var bench = Bench.open(h, GameType.CREATIVE, iron(1000));
        if (bench == null) return;
        bench.player.getAbilities().instabuild = true;
        bench.click(bench.large, 2, ClickType.CLONE);
        if (bench.expect("carried", bench.menu.getCarried(), iron(64))
                && bench.expect("the slot", bench.slot(), iron(1000))) h.succeed();
    }

    // endregion

    private static ItemStack iron(int count) {
        return new ItemStack(Items.IRON_INGOT, count);
    }

    private static ItemStack gold(int count) {
        return new ItemStack(Items.GOLD_INGOT, count);
    }

    /**
     * @param large the menu index of storage slot 0
     */
    private record Bench(GameTestHelper h, MBDMachine machine, LargeItemStackTransfer storage, Player player,
                         AbstractContainerMenu menu, int large) {

        @Nullable
        static Bench open(GameTestHelper h, GameType gameType, ItemStack inSlot) {
            var machine = MBDScenario.of(h).placeMachine(LargeItemSlotFixtures.LARGE, POS).machine();
            var state = h.getBlockState(POS);
            if (!(state.getBlock() instanceof BlockUIMenuType.BlockUI blockUI)) {
                h.fail("the machine block does not open a ui at all");
                return null;
            }
            var player = h.makeMockPlayer(gameType);
            var holder = new BlockUIMenuType.BlockUIHolder(blockUI, player, h.absolutePos(POS), state);
            var menu = holder.createMenu(1, player.getInventory(), player);
            player.containerMenu = menu;
            var storage = LargeItemSlotTests.storage(machine);
            storage.setStackInSlot(0, inSlot);
            for (int i = 0; i < menu.slots.size(); i++) {
                if (menu.slots.get(i) instanceof LargeItemHandlerSlot slot && slot.getContainerSlot() == 0) {
                    return new Bench(h, machine, storage, player, menu, i);
                }
            }
            h.fail("the menu has no large slot for storage slot 0, only " + menu.slots);
            return null;
        }

        void click(int slot, int button, ClickType type) {
            menu.clicked(slot, button, type, player);
        }

        ItemStack slot() {
            return storage.getStackInSlot(0);
        }

        ItemStack inventory(int slot) {
            return player.getInventory().getItem(slot);
        }

        int inventorySlot(int containerSlot) {
            for (int i = 0; i < menu.slots.size(); i++) {
                var slot = menu.slots.get(i);
                if (slot.container == player.getInventory() && slot.getContainerSlot() == containerSlot) return i;
            }
            throw new AssertionError("the menu has no slot for inventory slot " + containerSlot);
        }

        boolean expect(String what, ItemStack actual, ItemStack expected) {
            return LargeItemSlotTests.expectStack(h, what, actual, expected);
        }

        /** Fails unless {@code total} items are still around and the player holds no stack of more than its size. */
        boolean conserved(long total) {
            long count = menu.getCarried().getCount();
            for (int i = 0; i < storage.getSlots(); i++) count += storage.getStackInSlot(i).getCount();
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) count += inventory.getItem(i).getCount();
            if (count != total) {
                h.fail("there are " + count + " items, there were " + total);
                return false;
            }
            if (oversized(menu.getCarried())) {
                h.fail("the player carries an oversized " + menu.getCarried());
                return false;
            }
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (oversized(inventory.getItem(i))) {
                    h.fail("inventory slot " + i + " holds an oversized " + inventory.getItem(i));
                    return false;
                }
            }
            return true;
        }

        private static boolean oversized(ItemStack stack) {
            return stack.getCount() > stack.getMaxStackSize();
        }
    }
}

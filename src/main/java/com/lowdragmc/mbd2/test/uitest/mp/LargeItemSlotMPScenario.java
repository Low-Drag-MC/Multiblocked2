package com.lowdragmc.mbd2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.LargeItemHandlerSlot;
import com.lowdragmc.mbd2.common.trait.item.LargeItemStackTransfer;
import com.lowdragmc.mbd2.test.tests.trait.LargeItemSlotFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Large item slots between a dedicated server and a client: the counts reach the client through the storage sync and
 * the menu, are drawn compacted with the full count in the tooltip, and clicks end the same on both sides.
 */
@LDLRegister(name = "mbd2_mp_large_item_slot", group = "mbd2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class LargeItemSlotMPScenario implements MPScenario {
    private static final BlockPos POS = new BlockPos(2, -60, 8);
    private static final int IRON = 12_345;
    private static final int GOLD = Integer.MAX_VALUE;

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A").tags("mbd2", "mp");
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the machine with 12345 iron and a full int of gold", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS, MBDRegistries.MACHINE_DEFINITIONS.get(LargeItemSlotFixtures.SYNCED).block().defaultBlockState());
            var storage = storage(sc.blockEntity(POS, BlockEntity.class));
            storage.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, IRON));
            storage.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, GOLD));
            var player = sc.player("A");
            player.getInventory().clearContent();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, POS.getX() + 0.5, POS.getY(), POS.getZ() + 2.5, 180f, 0f);
        })
        .sync("the storage reaches the client whole, outside any menu", "A",
                sc -> counts(storage(sc.blockEntity(POS, BlockEntity.class))),
                ctx -> {
                    var storage = storage(ctx.clientBlockEntity(POS, BlockEntity.class));
                    return storage == null ? null : counts(storage);
                })
        .client("A", "open the machine and read the slots", b -> b
                .useBlock(POS)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .step("the slots hold the counts and draw them compacted", ctx -> {
                    var iron = machineSlot(ctx, 0).getValue();
                    var gold = machineSlot(ctx, 1).getValue();
                    ctx.check("slot 0 through the menu", iron.getCount() == IRON, IRON, iron.getCount());
                    ctx.check("slot 1 through the menu", gold.getCount() == GOLD, GOLD, gold.getCount());
                    ctx.check("12345 drawn as", "12k".equals(ItemSlot.compactCount(iron)), "12k", ItemSlot.compactCount(iron));
                    ctx.check("a full int drawn as", "2.1G".equals(ItemSlot.compactCount(gold)), "2.1G", ItemSlot.compactCount(gold));
                    ctx.check("a stack drawn as it is", ItemSlot.compactCount(new ItemStack(Items.IRON_INGOT, 64)) == null,
                            null, ItemSlot.compactCount(new ItemStack(Items.IRON_INGOT, 64)));
                    var ironTips = tooltip(machineSlot(ctx, 0));
                    var goldTips = tooltip(machineSlot(ctx, 1));
                    ctx.check("the iron tooltip gives the full count", ironTips.contains(String.valueOf(IRON)), IRON, ironTips);
                    ctx.check("the gold tooltip gives the full count", goldTips.contains(String.valueOf(GOLD)), GOLD, goldTips);
                })
                .hover("slot 0", ctx -> machineSlot(ctx, 0))
                .settleMs(300)
                .screenshot("12345 iron and its tooltip")
                .hover("slot 1", ctx -> machineSlot(ctx, 1))
                .settleMs(300)
                .screenshot("a full int of gold and its tooltip"));

        clickThenCompare(s, "left click slot 0 takes one stack", IRON,
                b -> b.click("slot 0", ctx -> machineSlot(ctx, 0), Keys.MOUSE_LEFT),
                sc -> machineCount(sc, 0) == IRON - 64 && sc.player("A").containerMenu.getCarried().getCount() == 64);
        clickThenCompare(s, "left click slot 0 puts it back", IRON,
                b -> b.click("slot 0", ctx -> machineSlot(ctx, 0), Keys.MOUSE_LEFT),
                sc -> machineCount(sc, 0) == IRON && sc.player("A").containerMenu.getCarried().isEmpty());
        clickThenCompare(s, "number key 1 on slot 0 moves one stack to the empty hotbar slot", IRON,
                b -> b.hover("slot 0", ctx -> machineSlot(ctx, 0)).key(GLFW.GLFW_KEY_1),
                sc -> machineCount(sc, 0) == IRON - 64 && sc.player("A").getInventory().getItem(0).getCount() == 64);
        clickThenCompare(s, "number key 1 again leaves the now occupied hotbar slot alone", IRON,
                b -> b.hover("slot 0", ctx -> machineSlot(ctx, 0)).key(GLFW.GLFW_KEY_1),
                sc -> machineCount(sc, 0) == IRON - 64 && sc.player("A").getInventory().getItem(0).getCount() == 64);
        clickThenCompare(s, "shift-click slot 0 fills the rest of the inventory a stack at a time", IRON,
                b -> b.step("shift-click slot 0", ctx -> ClientClicks.quickMove(ctx, machineSlot(ctx, 0))),
                sc -> machineCount(sc, 0) == IRON - 36 * 64);
        clickThenCompare(s, "shift-click an inventory stack back into slot 0", IRON,
                b -> b.step("shift-click inventory slot 9", ctx -> ClientClicks.quickMove(ctx, inventorySlot(ctx, 9))),
                sc -> machineCount(sc, 0) == IRON - 35 * 64);
        clickThenCompare(s, "right click the full int of gold takes one stack", IRON,
                b -> b.click("slot 1", ctx -> machineSlot(ctx, 1), Keys.MOUSE_RIGHT),
                sc -> machineCount(sc, 1) == GOLD - 64 && sc.player("A").containerMenu.getCarried().getCount() == 64);
        clickThenCompare(s, "left click puts the gold back up to a full int", IRON,
                b -> b.click("slot 1", ctx -> machineSlot(ctx, 1), Keys.MOUSE_LEFT),
                sc -> machineCount(sc, 1) == GOLD && sc.player("A").containerMenu.getCarried().isEmpty());

        s.client("A", "after all that", b -> b.screenshot("the machine after the clicks"))
                .teardownAllClients("close the screen", b -> b.closeScreen())
                .teardownServer("remove the machine and empty the inventory", sc -> {
                    sc.server().overworld().setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
                    sc.player("A").getInventory().clearContent();
                });
    }

    private static void clickThenCompare(MPScenarioBuilder s, String what, int iron,
                                         Consumer<ScenarioBuilder> click,
                                         Predicate<ServerContext> expected) {
        s.client("A", what, click)
                .syncAll(what + ": the client sees what the server holds",
                        sc -> view(storage(sc.blockEntity(POS, BlockEntity.class)), sc.player("A").getInventory(),
                                sc.player("A").containerMenu.getCarried()),
                        ctx -> {
                            var player = ctx.player();
                            if (player == null || ctx.ui() == null) return null;
                            var slots = new ArrayList<ItemStack>();
                            for (int i = 0; i < 2; i++) slots.add(machineSlot(ctx, i).getValue());
                            return view(slots, player.getInventory(), player.containerMenu.getCarried());
                        })
                .server(what + ": on the server", sc -> {
                    var player = sc.player("A");
                    sc.check(what, expected.test(sc));
                    var carried = player.containerMenu.getCarried();
                    sc.check("the cursor holds no more than a stack", carried.getCount() <= carried.getMaxStackSize(),
                            carried.getMaxStackSize(), carried.getCount());
                    var total = (long) machineCount(sc, 0) + (carried.is(Items.IRON_INGOT) ? carried.getCount() : 0);
                    var inventory = player.getInventory();
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        var stack = inventory.getItem(i);
                        if (stack.is(Items.IRON_INGOT)) total += stack.getCount();
                        sc.check("inventory slot " + i + " holds no more than a stack", stack.getCount() <= stack.getMaxStackSize(),
                                stack.getMaxStackSize(), stack.getCount());
                    }
                    sc.check("all the iron is still there", total == iron, iron, total);
                });
    }

    private static int machineCount(ServerContext sc, int slot) {
        return storage(sc.blockEntity(POS, BlockEntity.class)).getStackInSlot(slot).getCount();
    }

    private static String counts(LargeItemStackTransfer storage) {
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < storage.getSlots(); i++) slots.add(storage.getStackInSlot(i));
        return describe(slots);
    }

    private static String view(LargeItemStackTransfer storage, Inventory inventory, ItemStack carried) {
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < storage.getSlots(); i++) slots.add(storage.getStackInSlot(i));
        return view(slots, inventory, carried);
    }

    private static String view(List<ItemStack> machine, Inventory inventory, ItemStack carried) {
        var player = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.getContainerSize(); i++) player.add(inventory.getItem(i));
        return "machine " + describe(machine) + " | cursor " + describe(List.of(carried)) + " | inventory " + describe(player);
    }

    private static String describe(List<ItemStack> stacks) {
        var parts = new ArrayList<String>();
        for (var stack : stacks) parts.add(stack.isEmpty() ? "-" : stack.getItem() + "x" + stack.getCount());
        return String.join(",", parts);
    }

    @Nullable
    private static LargeItemStackTransfer storage(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine) {
            for (var trait : machine.getAdditionalTraits()) {
                if (trait instanceof ItemSlotCapabilityTrait itemTrait) return itemTrait.storage;
            }
        }
        return null;
    }

    private static ItemSlot machineSlot(TestContext ctx, int storageSlot) {
        for (var slot : itemSlots(ctx.requireUI().ui.rootElement, new ArrayList<>())) {
            if (slot.getSlot() instanceof LargeItemHandlerSlot large
                    && large.getContainerSlot() == storageSlot) return slot;
        }
        throw new IllegalStateException("no machine slot " + storageSlot + " in the ui");
    }

    private static ItemSlot inventorySlot(TestContext ctx, int inventorySlot) {
        var inventory = ctx.requirePlayer().getInventory();
        for (var slot : itemSlots(ctx.requireUI().ui.rootElement, new ArrayList<>())) {
            if (slot.getSlot().container == inventory && slot.getSlot().getContainerSlot() == inventorySlot) return slot;
        }
        throw new IllegalStateException("no slot for inventory slot " + inventorySlot + " in the ui");
    }

    private static List<ItemSlot> itemSlots(UIElement element, List<ItemSlot> found) {
        if (element instanceof ItemSlot slot) found.add(slot);
        for (var child : element.getChildren()) itemSlots(child, found);
        return found;
    }

    /**
     * Vanilla reads shift from GLFW, which synthetic input cannot reach, so this sends the click the screen would. Its
     * own class so the dedicated server never verifies passing a {@code LocalPlayer} as a {@code Player}.
     */
    private static final class ClientClicks {
        static void quickMove(TestContext ctx, ItemSlot slot) {
            var player = ctx.requirePlayer();
            ctx.mc().gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.getSlot().index, 0,
                    ClickType.QUICK_MOVE, player);
        }
    }

    private static String tooltip(ItemSlot slot) {
        var lines = new ArrayList<String>();
        for (var line : slot.getFullTooltipTexts()) lines.add(line.getString());
        return String.join(" / ", lines);
    }
}

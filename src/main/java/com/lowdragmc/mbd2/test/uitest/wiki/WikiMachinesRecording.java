package com.lowdragmc.mbd2.test.uitest.wiki;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Wiki videos of machines at work: two recipe logics, three threads, and large slots kept through a drop. */
@LDLRegisterClient(name = "wiki_mbd2_machines", group = "mbd2_wiki", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WikiMachinesRecording implements UIRecording {
    private BlockPos pos = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("wiki", "mbd2").requiresWorld(true).guiScale(4).scenarioTimeoutMs(600_000);
    }

    @Override
    public void define(ScenarioBuilder s) {
        twoLogics(s);
        threads(s);
        largeSlots(s);
    }

    private void twoLogics(ScenarioBuilder s) {
        open(s, WikiDemoFixtures.TWO_LOGICS)
                .take("recipe-logics-side-by-side", o -> o.size(1920, 1080).fps(30).poster(9_000), t -> {
                    say(t, "This machine has two recipe logics: press, its primary, and crush, an additional one.",
                            "这台机器有两个配方逻辑：主逻辑 press，和额外逻辑 crush。");
                    t.zoom("#wiki_machine", 700).hold(3500);
                    say(t, "Each slot trait lists the logic it belongs to, so each logic has an input and an output of its own.",
                            "每个槽位 Trait 都写明了自己属于哪个逻辑，所以每个逻辑都有自己的输入和输出。");
                    t.hold(4000);
                    say(t, "Fill both inputs: press makes dirt from stone, crush makes gravel from cobblestone, at the same time.",
                            "两边都放入原料：press 把石头压成泥土，crush 把圆石碎成沙砾，两者同时进行。");
                    t.server("fill both inputs", sc -> {
                        insert(sc, "press_in", new ItemStack(Items.STONE, 4));
                        insert(sc, "crush_in", new ItemStack(Items.COBBLESTONE, 3));
                    });
                    hold(t, 7000);
                    say(t, "Each logic has its own progress bar: ui:progress_bar for the primary, ui:progress_bar@crush for crush.",
                            "每个逻辑都有自己的进度条：主逻辑用 ui:progress_bar，crush 用 ui:progress_bar@crush。");
                    t.timeoutMs(30_000).waitUntilServer("both logics finished", sc -> count(sc, "press_out") >= 3 && count(sc, "crush_out") >= 2)
                            .hold(1500);
                    say(t, "", "");
                    t.zoomOut(600).hold(400);
                })
                .checkServer("press made dirt and crush made gravel",
                        sc -> count(sc, "press_out") >= 3 && count(sc, "crush_out") >= 2)
                .step("close it", ctx -> ctx.mc().setScreen(null)).frames(2);
    }

    private void threads(ScenarioBuilder s) {
        open(s, WikiDemoFixtures.THREADS)
                .take("recipe-logic-threads", o -> o.size(1920, 1080).fps(30).poster(5_000), t -> {
                    say(t, "One logic, main, with Threads set to 3: three copies of it share the same slots.",
                            "一个逻辑 main，Threads 设为 3：它的三份副本共用同一组槽位。");
                    t.zoom("#wiki_machine", 700).hold(3000);
                    say(t, "Put in six stone. Each thread takes one and runs a recipe of its own.",
                            "放入六块石头：每个线程各取一块，各自运行一条配方。");
                    t.server("fill the input", sc -> insert(sc, "in", new ItemStack(Items.STONE, 6)))
                            .hold(4000);
                    say(t, "The threads are addressed main, main#1 and main#2; each can have its own progress bar.",
                            "线程的地址是 main、main#1 和 main#2，每个都可以有自己的进度条。");
                    t.timeoutMs(30_000).waitUntilServer("all six pressed", sc -> count(sc, "out") >= 6).hold(1500);
                    say(t, "", "");
                    t.zoomOut(600).hold(400);
                })
                .checkServer("all six stone were pressed", sc -> count(sc, "out") == 6)
                .step("close it", ctx -> ctx.mc().setScreen(null)).frames(2);
    }

    private void largeSlots(ScenarioBuilder s) {
        open(s, WikiDemoFixtures.LARGE_SLOTS)
                .take("large-item-slots", o -> o.size(1920, 1080).fps(30).poster(6_500), t -> {
                    say(t, "These item slots hold up to 4096, and Ignore Item Stack Limit lets even diamonds go past 64.",
                            "这两个物品槽最多能放 4096 个；开启 Ignore Item Stack Limit 后，钻石也能超过 64。");
                    t.zoom("#wiki_machine", 700)
                            .server("fill both slots", sc -> {
                                insert(sc, "storage", new ItemStack(Items.COBBLESTONE, 3000));
                                insertAt(sc, "storage", 1, new ItemStack(Items.DIAMOND, 1500));
                            })
                            .hold(3000);
                    say(t, "Counts of 1000 and more are shortened; the tooltip has the full count.",
                            "1000 以上的数量会缩写显示，悬停提示里是完整数量。");
                    t.hover("the cobblestone slot", ctx -> slot(ctx, "storage", 0)).hold(3000)
                            .hover("the diamond slot", ctx -> slot(ctx, "storage", 1)).hold(3000);
                    say(t, "With Keep data on drop, breaking the machine keeps the contents in its item.",
                            "开启 Keep data on drop 后，拆掉机器时内容会保存在掉落的机器物品里。");
                    t.zoomOut(500)
                            .step("close the machine", ctx -> ctx.mc().setScreen(null))
                            .step("hide the hud", ctx -> ctx.mc().options.hideGui = true)
                            .hold(1200)
                            .server("break the machine", sc -> sc.level().destroyBlock(pos, true, sc.player()))
                            .hold(2500);
                    say(t, "Placed again, it has everything back.", "重新放下后，内容全部回来了。");
                    t.server("place the dropped machine item", this::placeDropped)
                            .waitUntil("the client has it again", ctx ->
                                    ctx.level().getBlockEntity(pos) instanceof IMachineBlockEntity)
                            .hold(1200)
                            .step("show the hud", ctx -> ctx.mc().options.hideGui = false)
                            .server("open it", sc -> BlockUIMenuType.openUI(sc.player(), pos))
                            .awaitScreen(ModularUIContainerScreen.class)
                            .awaitModularUI()
                            .zoom("#wiki_machine", 600)
                            .hold(3500);
                    say(t, "", "");
                    t.zoomOut(500).hold(300);
                })
                .step("show the hud", ctx -> ctx.mc().options.hideGui = false)
                .checkServer("the contents came back with the machine", sc -> count(sc, "storage") == 3000)
                .step("close it", ctx -> ctx.mc().setScreen(null)).frames(2);
    }

    /** Places {@code id} in front of the player, who looks at it, and opens it. */
    private ScenarioBuilder open(ScenarioBuilder s, ResourceLocation id) {
        return s.step("start from no screen", ctx -> ctx.mc().setScreen(null))
                .frames(2)
                .server("place " + id.getPath(), sc -> {
                    var origin = sc.player().blockPosition();
                    pos = origin.offset(0, 1, -3);
                    for (var p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 2, 3))) {
                        if (!p.equals(origin) && !p.equals(origin.above())) {
                            sc.level().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        }
                    }
                    sc.level().setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
                    sc.level().setBlockAndUpdate(pos, MBDRegistries.MACHINE_DEFINITIONS.get(id).block().defaultBlockState());
                    sc.player().teleportTo(sc.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 180, 12);
                    sc.level().setDayTime(6000);
                })
                .waitUntil("the client has the machine", ctx ->
                        ctx.level() != null && ctx.level().getBlockEntity(pos) instanceof IMachineBlockEntity)
                .server("open it", sc -> BlockUIMenuType.openUI(sc.player(), pos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(6);
    }

    private void placeDropped(ServerContext sc) {
        var item = MBDRegistries.MACHINE_DEFINITIONS.get(WikiDemoFixtures.LARGE_SLOTS).item();
        var dropped = sc.level().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3),
                entity -> entity.getItem().is(item));
        if (dropped.isEmpty()) throw new IllegalStateException("the machine dropped no item");
        var stack = dropped.getFirst().getItem().copy();
        dropped.forEach(ItemEntity::discard);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ((BlockItem) stack.getItem()).place(new BlockPlaceContext(sc.level(), sc.player(), InteractionHand.MAIN_HAND, stack, hit));
    }

    /** A hold longer than a step may take, in pieces. */
    private static void hold(ScenarioBuilder t, long ms) {
        for (; ms > 0; ms -= 4000) t.hold(Math.min(ms, 4000));
    }

    private static void say(ScenarioBuilder t, String en, String zh) {
        t.caption("en", en).caption("zh", zh);
    }

    private ItemSlotCapabilityTrait trait(MBDMachine machine, String name) {
        return machine.getTraitByName(ItemSlotCapabilityTrait.class, name);
    }

    private void insert(ServerContext sc, String trait, ItemStack stack) {
        insertAt(sc, trait, 0, stack);
    }

    private void insertAt(ServerContext sc, String trait, int slot, ItemStack stack) {
        if (sc.level().getBlockEntity(pos) instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine) {
            trait(machine, trait).storage.insertItem(slot, stack, false);
        }
    }

    private int count(ServerContext sc, String trait) {
        if (sc.level().getBlockEntity(pos) instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine) {
            return trait(machine, trait).storage.getStackInSlot(0).getCount();
        }
        return -1;
    }

    private static UIElement slot(TestContext ctx, String trait, int index) {
        return ctx.query().withId("ui:" + trait + "_" + index).one().as(ItemSlot.class);
    }
}

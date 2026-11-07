package com.lowdragmc.mbd2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.tests.recipe.logic.MultiRecipeLogicFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

/**
 * A machine with two recipe logics between a dedicated server and a client: the second logic's state syncs like the
 * primary's, and its UI — a progress bar per logic — opens without the two sides disagreeing, each bar following its
 * own logic.
 */
@LDLRegister(name = "mbd2_mp_multi_recipe_logic", group = "mbd2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MultiRecipeLogicMPScenario implements MPScenario {
    private static final BlockPos POS = new BlockPos(2, -60, 8);
    private static final String BAR_A = MBDBindingIDs.PROGRESS_BAR;
    private static final String BAR_B = MBDBindingIDs.forRecipeLogic(MBDBindingIDs.PROGRESS_BAR, "b");

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A").tags("mbd2", "mp", "recipe_logic");
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the two-logic machine and feed both logics", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS, MBDRegistries.MACHINE_DEFINITIONS.get(MultiRecipeLogicFixtures.ISOLATED_UI)
                    .block().defaultBlockState());
            var machine = machine(level.getBlockEntity(POS));
            ((ItemSlotCapabilityTrait) machine.getTraitByName("item_slot")).storage.setStackInSlot(0, new ItemStack(Items.STONE, 16));
            ((ItemSlotCapabilityTrait) machine.getTraitByName("item_slot_2")).storage.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
            var energy = Capabilities.EnergyStorage.BLOCK.getCapability(level, POS, level.getBlockState(POS), level.getBlockEntity(POS), null);
            energy.receiveEnergy(100_000, false);
            var player = sc.player("A");
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, POS.getX() + 0.5, POS.getY(), POS.getZ() + 2.5, 180f, 0f);
        })
        .serverWaitUntil("a and b to be working", sc -> {
            var machine = machine(sc.blockEntity(POS, BlockEntity.class));
            var b = machine.getRecipeLogic("b");
            return machine.getRecipeLogic().isWorking() && b != null && b.isWorking();
        })
        .sync("b's status reaches the client like a's", "A",
                sc -> statuses(sc.blockEntity(POS, BlockEntity.class)),
                ctx -> statuses(ctx.clientBlockEntity(POS, BlockEntity.class)))
        .client("A", "open the machine", b -> b
                .useBlock(POS)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                .waitUntil("both bars to show progress", ctx -> bar(ctx, BAR_A) > 0 && bar(ctx, BAR_B) > 0)
                .waitUntil("the bars to follow different logics", ctx -> Math.abs(bar(ctx, BAR_A) - bar(ctx, BAR_B)) > 0.05f)
                .check("each bar is in range", ctx -> {
                    ctx.attach("a", Float.toString(bar(ctx, BAR_A)));
                    ctx.attach("b", Float.toString(bar(ctx, BAR_B)));
                    return bar(ctx, BAR_A) <= 1f && bar(ctx, BAR_B) <= 1f;
                })
                // the label is worked out on the client, where no logic knows its progress
                .waitUntil("each label to read its own bar", ctx -> labelFollowsBar(ctx, BAR_A) && labelFollowsBar(ctx, BAR_B))
                .step("record the labels", ctx -> {
                    ctx.attach("a label", label(ctx, BAR_A));
                    ctx.attach("b label", label(ctx, BAR_B));
                })
                .screenshot("two logics, two bars"))
        .server("the client is still connected", sc -> sc.check("player A is online",
                sc.server().getPlayerList().getPlayers().size() == 1, 1, sc.server().getPlayerList().getPlayers().size()))
        .teardownAllClients("close the screen", b -> b.closeScreen())
        .teardownServer("remove the machine", sc ->
                sc.server().overworld().setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState()));
    }

    private static MBDMachine machine(@Nullable BlockEntity be) {
        if (be instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine) return machine;
        throw new IllegalStateException("no machine at " + POS);
    }

    private static String statuses(@Nullable BlockEntity be) {
        if (!(be instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine)) return "none";
        var b = machine.getRecipeLogic("b");
        return machine.getRecipeLogic().getStatus() + "/" + (b == null ? "missing" : b.getStatus());
    }

    private static float bar(TestContext ctx, String id) {
        return ctx.query().withId(id).one().as(ProgressBar.class).getValue();
    }

    private static String label(TestContext ctx, String id) {
        return ctx.query().withId(id).one().as(ProgressBar.class).label.getValue().getString();
    }

    private static boolean labelFollowsBar(TestContext ctx, String id) {
        float value = bar(ctx, id);
        return value > 0 && label(ctx, id).equals(String.format("%.2f%%", value * 100));
    }
}

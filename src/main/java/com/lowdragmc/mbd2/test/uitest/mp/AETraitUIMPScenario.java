package com.lowdragmc.mbd2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.lowdraglib2.utils.IdentityMap;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.integration.ae2.trait.AEInterfaceSlot;
import com.lowdragmc.mbd2.integration.ae2.trait.AEPatternProviderSlot;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.tests.trait.ae2.AEMultiblockUIFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Issue #249: opening the UI of a multiblock controller that holds an AE2 trait kicked the player off a dedicated
 * server. The single-block machine opens first, as the control.
 */
@LDLRegister(name = "mbd2_mp_ae_trait_ui", group = "mbd2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY, modID = "ae2")
public class AETraitUIMPScenario implements MPScenario {
    private static final BlockPos SINGLE = new BlockPos(2, -60, 2);
    private static final BlockPos MB_INTERFACE = new BlockPos(8, -60, 2);
    private static final BlockPos MB_PATTERN_PROVIDER = new BlockPos(14, -60, 2);

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A").tags("mbd2", "ae2", "mp");
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the machines and form the multiblocks", sc -> {
            place(sc, SINGLE, AEMultiblockUIFixtures.SINGLE_INTERFACE_ID);
            for (var controller : new BlockPos[]{MB_INTERFACE, MB_PATTERN_PROVIDER}) {
                var level = sc.server().overworld();
                level.setBlockAndUpdate(controller.east(), Blocks.STONE.defaultBlockState());
                place(sc, controller.west(), AEMultiblockUIFixtures.PART_ID);
            }
            place(sc, MB_INTERFACE, AEMultiblockUIFixtures.MB_INTERFACE_ID);
            place(sc, MB_PATTERN_PROVIDER, AEMultiblockUIFixtures.MB_PATTERN_PROVIDER_ID);
            for (var controller : new BlockPos[]{MB_INTERFACE, MB_PATTERN_PROVIDER}) {
                var formed = MBDTestHelper.tryForm(machine(sc.server().overworld().getBlockEntity(controller)));
                sc.check("the multiblock at " + controller + " formed", formed);
            }
            sc.player("A").setGameMode(GameType.CREATIVE);
        });
        open(s, "the single-block AE2 interface", SINGLE, AEInterfaceSlot.class, 2);
        open(s, "the multiblock with an AE2 interface", MB_INTERFACE, AEInterfaceSlot.class, 2);
        open(s, "the multiblock with an AE2 pattern provider", MB_PATTERN_PROVIDER, AEPatternProviderSlot.class, 1);
        s.teardownAllClients("close the screen", b -> b.closeScreen())
                .teardownServer("remove the machines", sc -> {
                    var level = sc.server().overworld();
                    for (var pos : new BlockPos[]{SINGLE, MB_INTERFACE, MB_PATTERN_PROVIDER}) {
                        level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
                    }
                });
    }

    private static void open(MPScenarioBuilder s, String what, BlockPos pos, Class<? extends UIElement> slotType, int slots) {
        s.server("stand next to " + what, sc -> {
            var player = sc.player("A");
            player.teleportTo(sc.server().overworld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 180f, 0f);
        })
        .client("A", "open " + what, b -> b
                .waitUntil("the client sees the machine, formed if it is a multiblock", ctx -> isReady(ctx, pos))
                .useBlock(pos)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .step("the ui carries the AE2 trait's widgets", ctx -> {
                    var found = ctx.requireUI().ui.selectRegex(".*", slotType).count();
                    ctx.check(slotType.getSimpleName() + " widgets", found == slots, slots, found);
                }))
        .syncAll("the server and the client registered the same sync values",
                sc -> syncValues(sc.ui()), ctx -> syncValues(ctx.ui()))
        .client("A", "close " + what, b -> b.closeScreen());
    }

    @Nullable
    private static String syncValues(@Nullable ModularUI modularUI) {
        if (modularUI == null) return null;
        try {
            var field = UISyncManager.class.getDeclaredField("syncValues");
            field.setAccessible(true);
            var ids = ((IdentityMap<?>) field.get(modularUI.syncManager)).getIdToSyncValue().keySet();
            return ids.size() + " values, ids up to " + ids.intStream().max().orElse(-1);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isReady(TestContext ctx, BlockPos pos) {
        var machine = machine(ctx.clientBlockEntity(pos, BlockEntity.class));
        return machine != null && (!(machine instanceof MBDMultiblockMachine multiblock)
                || multiblock.isFormed() && !multiblock.getParts().isEmpty());
    }

    private static void place(ServerContext sc, BlockPos pos, ResourceLocation id) {
        sc.server().overworld().setBlockAndUpdate(pos, MBDRegistries.MACHINE_DEFINITIONS.get(id).block().defaultBlockState());
    }

    private static MBDMachine machine(BlockEntity blockEntity) {
        return blockEntity instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine
                ? machine : null;
    }
}

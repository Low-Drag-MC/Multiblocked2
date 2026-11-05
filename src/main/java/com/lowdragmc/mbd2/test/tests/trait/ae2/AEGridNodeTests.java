package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.holder.ISyncMangedHolder;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTrait;
import com.lowdragmc.mbd2.integration.ae2.trait.MEPatternProviderTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * The grid node and logic of the ME traits are AE2 objects, which only the server thread may touch, while the machine
 * syncs from LDLib2's async thread.
 */
// registered via MBDTestRegistry when AE2 is loaded, see MEInterfaceTraitTests
public class AEGridNodeTests {
    static {
        @SuppressWarnings("unused") var interfaceMachine = MEInterfaceTraitFixtures.MACHINE_ID;
        @SuppressWarnings("unused") var providerMachine = MEPatternProviderTraitFixtures.MACHINE_ID;
    }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int OWNER = 4242;

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void interface_node_is_saved_on_the_server_thread_only(GameTestHelper h) {
        savedOnTheServerThreadOnly(h, MEInterfaceTraitFixtures.MACHINE_ID);
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pattern_provider_node_is_saved_on_the_server_thread_only(GameTestHelper h) {
        savedOnTheServerThreadOnly(h, MEPatternProviderTraitFixtures.MACHINE_ID);
    }

    /** Without a config the interface offers its grid's storage, which it learns of when it joins the grid. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void interface_offers_the_storage_of_the_grid_it_joins(GameTestHelper h) {
        var host = bootedHost(h, MEInterfaceTraitFixtures.MACHINE_ID);
        if (((MEInterfaceTrait) host).getInterfaceLogic().getInventory() == null) {
            h.fail("the interface never picked up its grid's storage");
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void interface_node_changes_get_the_machine_saved(GameTestHelper h) {
        nodeChangesGetTheMachineSaved(h, MEInterfaceTraitFixtures.MACHINE_ID, "proxy");
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pattern_provider_node_changes_get_the_machine_saved(GameTestHelper h) {
        nodeChangesGetTheMachineSaved(h, MEPatternProviderTraitFixtures.MACHINE_ID, "pattern_provider");
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void interface_contents_and_node_survive_a_world_reload(GameTestHelper h) {
        var before = (MEInterfaceTrait) bootedHost(h, MEInterfaceTraitFixtures.MACHINE_ID);
        before.getMainNode().setOwningPlayerId(OWNER);
        before.getInterfaceLogic().getStorage().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 10));
        before.getInterfaceLogic().getConfig().setStack(2, new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 5));

        var after = (MEInterfaceTrait) reloadAndBoot(h);
        var stored = after.getInterfaceLogic().getStorage().getStack(0);
        var config = after.getInterfaceLogic().getConfig().getStack(2);
        if (!new GenericStack(AEItemKey.of(Items.IRON_INGOT), 10).equals(stored)
                || !new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 5).equals(config)) {
            h.fail("expected 10 iron stored and 5 gold configured after the reload, got " + stored + " and " + config);
        }
        requireOwner(h, after);
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void pattern_provider_contents_and_node_survive_a_world_reload(GameTestHelper h) {
        var before = (MEPatternProviderTrait) bootedHost(h, MEPatternProviderTraitFixtures.MACHINE_ID);
        before.getMainNode().setOwningPlayerId(OWNER);
        before.getStorage().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 10));

        var after = (MEPatternProviderTrait) reloadAndBoot(h);
        var stored = after.getStorage().getStack(0);
        if (!new GenericStack(AEItemKey.of(Items.IRON_INGOT), 10).equals(stored)) {
            h.fail("expected 10 iron cached after the reload, got " + stored);
        }
        requireOwner(h, after);
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void loading_twice_before_the_first_tick_creates_the_node_once(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(MEInterfaceTraitFixtures.MACHINE_ID, POS).machine();
        machine.onLoad();
        var failures = runQueuedServerTasks(h);
        if (!failures.isEmpty()) {
            h.fail("a queued node creation threw: " + failures.get(0));
        }
        requireBooted(h, gridHost(h, machine));
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void removing_an_interface_destroys_its_node(GameTestHelper h) {
        removingTheMachineDestroysItsNode(h, MEInterfaceTraitFixtures.MACHINE_ID);
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void removing_a_pattern_provider_destroys_its_node(GameTestHelper h) {
        removingTheMachineDestroysItsNode(h, MEPatternProviderTraitFixtures.MACHINE_ID);
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void a_machine_removed_before_its_first_tick_gets_no_node(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(MEInterfaceTraitFixtures.MACHINE_ID, POS).machine();
        var host = gridHost(h, machine);
        h.setBlock(POS, Blocks.AIR);
        runQueuedServerTasks(h);
        if (host.getMainNode().isReady()) {
            h.fail("a node was created for a machine already removed, and nothing will ever destroy it");
        }
        h.succeed();
    }

    private static void savedOnTheServerThreadOnly(GameTestHelper h, ResourceLocation machineId) {
        OffThreadSaveProbe.install();
        var host = bootedHost(h, machineId);
        var holder = (ISyncMangedHolder) h.getLevel().getBlockEntity(h.absolutePos(POS));
        try {
            // what LDLib2's async thread runs for the machine every 50 ms
            CompletableFuture.runAsync(() -> holder.asyncTick(0)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            h.fail("the async sync failed: " + e);
        }
        if (OffThreadSaveProbe.savedOffThread(host)) {
            h.fail("the grid node was saved off the server thread");
        }
        h.succeed();
    }

    private static void removingTheMachineDestroysItsNode(GameTestHelper h, ResourceLocation machineId) {
        var host = bootedHost(h, machineId);
        var node = host.getMainNode().getNode();
        var grid = node.getGrid();
        h.setBlock(POS, Blocks.AIR);
        var stillInGrid = false;
        for (var other : grid.getNodes()) {
            stillInGrid |= other == node;
        }
        if (host.getMainNode().isReady() || stillInGrid) {
            h.fail("the node outlived its machine");
        }
        h.succeed();
    }

    private static void nodeChangesGetTheMachineSaved(GameTestHelper h, ResourceLocation machineId, String nodeTag) {
        var host = bootedHost(h, machineId);
        var pos = h.absolutePos(POS);
        var chunk = h.getLevel().getChunkAt(pos);
        chunk.setUnsaved(false);
        host.getMainNode().setOwningPlayerId(OWNER);
        if (!chunk.isUnsaved()) {
            h.fail("changing the node's owner did not mark the machine for saving");
        }
        var saved = h.getLevel().getBlockEntity(pos).saveWithoutMetadata(h.getLevel().registryAccess());
        if (!savesOwner(saved, nodeTag)) {
            h.fail("the saved machine has no '" + nodeTag + "' node with owner " + OWNER);
        }
        h.succeed();
    }

    private static boolean savesOwner(CompoundTag tag, String nodeTag) {
        for (var key : tag.getAllKeys()) {
            if (tag.get(key) instanceof CompoundTag compound) {
                if (key.equals(nodeTag) && compound.contains("p", Tag.TAG_INT) && compound.getInt("p") == OWNER) {
                    return true;
                }
                if (savesOwner(compound, nodeTag)) return true;
            }
        }
        return false;
    }

    private static IGridConnectedBlockEntity bootedHost(GameTestHelper h, ResourceLocation machineId) {
        var machine = MBDScenario.of(h).placeMachine(machineId, POS).machine();
        runQueuedServerTasks(h);
        var host = gridHost(h, machine);
        requireBooted(h, host);
        return host;
    }

    /** Runs what the machine queued when it loaded, such as creating its node; returns what those tasks threw. */
    private static List<Throwable> runQueuedServerTasks(GameTestHelper h) {
        // the server logs a failing task and carries on, so listen for the log
        var failures = new CopyOnWriteArrayList<Throwable>();
        var logger = (Logger) LogManager.getLogger(BlockableEventLoop.class);
        var appender = new AbstractAppender("mbd2-test-task-failures", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (event.getThrown() != null) failures.add(event.getThrown());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            var server = h.getLevel().getServer();
            while (server.pollTask()) {
                // drain
            }
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return failures;
    }

    /** Saves the machine and loads it back the way world load does, then lets it create its node. */
    private static IGridConnectedBlockEntity reloadAndBoot(GameTestHelper h) {
        var machine = MBDTestHelper.roundTripPersistence(h, POS, tag -> {}, false);
        runQueuedServerTasks(h);
        var host = gridHost(h, machine);
        requireBooted(h, host);
        return host;
    }

    private static void requireOwner(GameTestHelper h, IGridConnectedBlockEntity host) {
        var owner = host.getMainNode().getNode().getOwningPlayerId();
        if (owner != OWNER) {
            h.fail("the reloaded node belongs to " + owner + ", expected " + OWNER);
        }
    }

    private static void requireBooted(GameTestHelper h, IGridConnectedBlockEntity host) {
        if (!host.getMainNode().isReady() || host.getMainNode().getGrid() == null) {
            h.fail("the grid node never joined a grid");
        }
    }

    private static IGridConnectedBlockEntity gridHost(GameTestHelper h, MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof IGridConnectedBlockEntity host) return host;
        }
        h.fail(machine.getDefinition().id() + " has no AE2 trait");
        throw new AssertionError();
    }
}

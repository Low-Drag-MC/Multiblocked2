package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;

/**
 * Tests for capability proxying:
 * - Parts forwarding their controller's trait capabilities via {@code partSettings.proxyControllerCapabilities}.
 * - {@link ProxyPartBlock} block entities forwarding the controller's caps via {@code ProxyWhileFormed.proxyCapabilities}.
 * - Stacking: a predicate's proxyCapabilities composing with a part's own proxyControllerCapabilities.
 */
@GameTestHolder(MBD2.MOD_ID)
public class ProxyCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ProxyCapabilityFixtures.PART_PROXY_CONTROLLER_ID; }

    // Pattern is a 3-block line "SCP" along +X with the controller's default NORTH facing:
    // leftmost ('S') sits at world +X (east) → STONE_POS; rightmost ('P') at world -X (west) → PART_POS.
    private static final BlockPos CONTROLLER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PART_POS = new BlockPos(0, 1, 1);
    private static final BlockPos STONE_POS = new BlockPos(2, 1, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void part_proxies_controller_item_handler(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ProxyCapabilityFixtures.PART_PROXY_CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(ProxyCapabilityFixtures.PART_PROXY_PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        // Drop one stone into slot 0 via the part's exposed ItemHandler.
        var partHandler = MBDScenarioCapabilityHelper.itemHandlerAt(h, PART_POS, null);
        if (partHandler == null) {
            h.fail("Part at " + PART_POS + " should expose proxied IItemHandler from controller");
            return;
        }
        if (partHandler.getSlots() < 2) {
            h.fail("Expected at least 2 slots from controller proxy, got " + partHandler.getSlots());
            return;
        }
        var leftover = partHandler.insertItem(0, new ItemStack(Items.STONE, 3), false);
        if (!leftover.isEmpty()) {
            h.fail("Insert into proxied part handler should consume the stack; leftover=" + leftover);
            return;
        }

        // The controller's slot 0 (IN) must now hold the stone.
        var controllerHandler = MBDScenarioCapabilityHelper.itemHandlerAt(h, CONTROLLER_POS, null);
        if (controllerHandler == null) {
            h.fail("Controller should expose its own IItemHandler");
            return;
        }
        var inSlot = controllerHandler.getStackInSlot(0);
        if (inSlot.getCount() != 3 || !inSlot.is(Items.STONE)) {
            h.fail("Controller slot 0 should contain 3x stone after proxied insert; got " + inSlot);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_part_block_proxies_controller_item_handler(GameTestHelper h) {
        // The 'P' slot needs iron_block (per fixture); after forming it becomes a ProxyPartBlock.
        MBDScenario.of(h)
                .placeMachine(ProxyCapabilityFixtures.PROXY_BLOCK_CONTROLLER_ID, CONTROLLER_POS)
                .placeBlock(PART_POS, Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        if (!h.getBlockState(PART_POS).is(ProxyPartBlock.BLOCK)) {
            h.fail("Iron block slot should have been replaced by ProxyPartBlock after forming");
            return;
        }

        var proxyHandler = MBDScenarioCapabilityHelper.itemHandlerAt(h, PART_POS, null);
        if (proxyHandler == null) {
            h.fail("ProxyPartBlock at " + PART_POS + " should expose proxied IItemHandler from controller");
            return;
        }
        if (proxyHandler.getSlots() < 2) {
            h.fail("Expected at least 2 slots from controller proxy, got " + proxyHandler.getSlots());
            return;
        }
        var leftover = proxyHandler.insertItem(0, new ItemStack(Items.DIAMOND, 2), false);
        if (!leftover.isEmpty()) {
            h.fail("Insert into proxy block handler should consume the stack; leftover=" + leftover);
            return;
        }

        var controllerHandler = MBDScenarioCapabilityHelper.itemHandlerAt(h, CONTROLLER_POS, null);
        if (controllerHandler == null) {
            h.fail("Controller should expose its own IItemHandler");
            return;
        }
        var inSlot = controllerHandler.getStackInSlot(0);
        if (inSlot.getCount() != 2 || !inSlot.is(Items.DIAMOND)) {
            h.fail("Controller slot 0 should contain 2x diamond after proxied insert; got " + inSlot);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_part_block_loses_capability_after_invalidation(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(ProxyCapabilityFixtures.PROXY_BLOCK_CONTROLLER_ID, CONTROLLER_POS)
                .placeBlock(PART_POS, Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        // Sanity: cap is present while formed.
        if (MBDScenarioCapabilityHelper.itemHandlerAt(h, PART_POS, null) == null) {
            h.fail("Expected proxied cap while formed");
            return;
        }

        if (!(scenario.machine() instanceof com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine controller)) {
            h.fail("Expected multiblock controller");
            return;
        }
        controller.onStructureInvalid(false);

        // After invalidation, the original iron block is restored — no longer a ProxyPartBlock.
        if (h.getBlockState(PART_POS).is(ProxyPartBlock.BLOCK)) {
            h.fail("ProxyPartBlock should have been restored to original block after invalidation");
            return;
        }
        // The vanilla iron block doesn't expose IItemHandler.
        if (MBDScenarioCapabilityHelper.itemHandlerAt(h, PART_POS, null) != null) {
            h.fail("Restored vanilla block should not expose IItemHandler");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void part_stacks_static_and_predicate_proxy_caps(GameTestHelper h) {
        // Controller has 1 IN slot; the part proxies it both via partSettings AND via the predicate's
        // proxyCapabilities. The merged handler should report 2 slots (one per source) — both pointing
        // at the same underlying trait but distinct merged entries.
        MBDScenario.of(h)
                .placeMachine(ProxyCapabilityFixtures.STACKED_CONTROLLER_ID, CONTROLLER_POS)
                .placeMachine(ProxyCapabilityFixtures.STACKED_PART_ID, PART_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        var partHandler = MBDScenarioCapabilityHelper.itemHandlerAt(h, PART_POS, null);
        if (partHandler == null) {
            h.fail("Stacked part should expose merged proxied handler");
            return;
        }
        // Each ProxyCapability hit contributes one IItemHandler entry; controller has 1 slot, so
        // merged ItemHandlerList ends up with 2 slots total (static + predicate).
        if (partHandler.getSlots() != 2) {
            h.fail("Expected 2 stacked proxy slots (static + predicate); got " + partHandler.getSlots());
            return;
        }
        h.succeed();
    }

    /** Local helper bundle so we don't pollute MBDTestHelper with proxy-cap-specific shortcuts. */
    private static final class MBDScenarioCapabilityHelper {
        @Nullable
        static net.neoforged.neoforge.items.IItemHandler itemHandlerAt(GameTestHelper h, BlockPos relPos, @Nullable net.minecraft.core.Direction side) {
            var abs = h.absolutePos(relPos);
            return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, side);
        }
    }
}

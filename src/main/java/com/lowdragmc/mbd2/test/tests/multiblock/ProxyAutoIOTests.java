package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for issue #237: a {@code proxyWhileFormed} predicate whose {@code proxyCapabilities}
 * have auto IO enabled must actually move items through the proxying block.
 * <p>
 * Two code paths carry a predicate proxy and both were dead before the fix:
 * <ul>
 *     <li>a non-MBD block replaced by a {@link ProxyPartBlock} — that block entity has no ticker at
 *     all, so the controller has to drive its ports;</li>
 *     <li>an MBD part matched by the predicate — its tick only ever looked at the part's own
 *     {@code partSettings.proxyControllerCapabilities}, never at the predicate's.</li>
 * </ul>
 */
@GameTestHolder(MBD2.MOD_ID)
public class ProxyAutoIOTests {
    static { @SuppressWarnings("unused") var ignored = ProxyAutoIOFixtures.BLOCK_PORT_OUTPUT_ID; }

    // Pattern is the 3-block line "SCP" along +X with the controller's default NORTH facing:
    // 'P' (the proxy port) ends up west of the controller, 'S' east. The chest sits on top of the
    // port, matching the fixtures' top-side-only auto IO config.
    private static final BlockPos CONTROLLER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PORT_POS = new BlockPos(0, 1, 1);
    private static final BlockPos STONE_POS = new BlockPos(2, 1, 1);
    private static final BlockPos CHEST_POS = new BlockPos(0, 2, 1);

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_block_port_auto_outputs_controller_items(GameTestHelper h) {
        MBDTestHelper.placeChestWithItems(h, CHEST_POS);
        var scenario = MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.BLOCK_PORT_OUTPUT_ID, CONTROLLER_POS)
                .placeBlock(PORT_POS, Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        if (!h.getBlockState(PORT_POS).is(ProxyPartBlock.BLOCK)) {
            h.fail("Iron block slot should have been replaced by ProxyPartBlock after forming");
            return;
        }

        scenario.insertItem(0, new ItemStack(Items.GOLD_INGOT, 9)).runTicks(3);

        int inChest = countInChest(h, Items.GOLD_INGOT);
        if (inChest != 9) {
            h.fail("Proxy port auto output should have pushed 9 gold ingots into the chest above; got " + inChest);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_block_port_auto_inputs_from_neighbor(GameTestHelper h) {
        MBDTestHelper.placeChestWithItems(h, CHEST_POS, new ItemStack(Items.IRON_INGOT, 12));
        MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.BLOCK_PORT_INPUT_ID, CONTROLLER_POS)
                .placeBlock(PORT_POS, Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed()
                .runTicks(3)
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 12))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void part_port_auto_outputs_via_predicate_proxy(GameTestHelper h) {
        // The part carries no proxyControllerCapabilities of its own — only the predicate grants the
        // proxy, which is exactly the case MBDPartMachine's tick used to ignore.
        MBDTestHelper.placeChestWithItems(h, CHEST_POS);
        var scenario = MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.PART_PORT_OUTPUT_ID, CONTROLLER_POS)
                .placeMachine(ProxyAutoIOFixtures.PART_PORT_ID, PORT_POS)
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed();

        if (h.getBlockState(PORT_POS).is(ProxyPartBlock.BLOCK)) {
            h.fail("A part must keep its own block instead of being replaced by ProxyPartBlock");
            return;
        }

        scenario.insertItem(0, new ItemStack(Items.EMERALD, 5)).runTicks(3);

        int inChest = countInChest(h, Items.EMERALD);
        if (inChest != 5) {
            h.fail("Predicate proxy on a part should have pushed 5 emeralds into the chest above; got " + inChest);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void proxy_block_port_stops_auto_io_after_structure_invalidates(GameTestHelper h) {
        MBDTestHelper.placeChestWithItems(h, CHEST_POS);
        var scenario = MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.BLOCK_PORT_OUTPUT_ID, CONTROLLER_POS)
                .placeBlock(PORT_POS, Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(STONE_POS, Blocks.STONE.defaultBlockState())
                .target(CONTROLLER_POS)
                .formNow()
                .assertFormed()
                .insertItem(0, new ItemStack(Items.GOLD_INGOT, 4))
                .runTicks(3);

        if (countInChest(h, Items.GOLD_INGOT) != 4) {
            h.fail("Sanity check failed: proxy port did not push while formed");
            return;
        }

        if (!(scenario.machine() instanceof MBDMultiblockMachine controller)) {
            h.fail("Expected multiblock controller");
            return;
        }
        controller.onStructureInvalid(false);

        scenario.insertItem(0, new ItemStack(Items.GOLD_INGOT, 4)).runTicks(3);

        int inChest = countInChest(h, Items.GOLD_INGOT);
        if (inChest != 4) {
            h.fail("Proxy port must stop auto IO once the structure is invalid; chest now holds " + inChest);
            return;
        }
        scenario.assertItem(0, new ItemStack(Items.GOLD_INGOT, 4)).succeed();
    }

    private static int countInChest(GameTestHelper h, Item item) {
        int total = 0;
        for (var stack : MBDTestHelper.readChestItems(h, CHEST_POS)) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}

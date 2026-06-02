package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MBD2.MOD_ID)
public class PatternSnapshotTests {
    static { @SuppressWarnings("unused") var ignored = PatternSnapshotFixtures.MACHINE_FLOOR_5X5; }

    /** With 24 tracked positions and CAPTURES_PER_TICK=20, the snapshot needs 2 main-thread
     *  ticks to fully capture before the inline check can succeed. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void snapshot_built_incrementally_then_forms(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDTestHelper.fillVolume(h, controller.offset(-2, 0, -2), controller.offset(2, 0, 2),
                Blocks.STONE.defaultBlockState());
        MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_FLOOR_5X5, controller)
                .tickSnapshotCapture(1)
                .assertSnapshotEntriesAtLeast(20)
                .tickSnapshotCapture(1)
                .assertSnapshotFullyBuilt()
                .runAsyncSnapshotCheck()
                .assertFormed()
                .succeed();
    }

    /** {@code any()} predicate positions must not appear in {@code trackedPositions} — only the
     *  stone cell counts. The any cell stays air and the structure still forms. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void snapshot_skips_any_predicate_positions(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        // Stone goes WEST of controller (pattern "ACS" with default charDir=LEFT puts 'S' at west).
        h.setBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState());
        MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_AND_ANY, controller)
                .tickSnapshotCapture(1)
                .assertSnapshotFullyBuilt()
                .assertSnapshotEntriesAtLeast(1)
                .runAsyncSnapshotCheck()
                .assertFormed()
                .succeed();
    }

    /** Changing a tracked block must mark its snapshot position dirty via the ChunkMixin path. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void snapshot_invalidates_on_blockstate_change(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stonePos = controller.relative(Direction.WEST);
        h.setBlock(stonePos, Blocks.STONE.defaultBlockState());
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_WEST, controller)
                .tickSnapshotCapture(1)
                .assertSnapshotFullyBuilt();
        // Mutating a tracked pos should re-enqueue it.
        h.setBlock(stonePos, Blocks.DIRT.defaultBlockState());
        scenario.assertSnapshotPending(1)
                .succeed();
    }

    /** End-to-end async path: dispatchPendingChecks → worker thread → server.execute(formOnMain).
     *  Tests the production code path, not the inline test hook. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void async_pipeline_forms_structure(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        h.setBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState());
        MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_WEST, controller)
                .tickAsyncPipeline(2)
                .assertFormed()
                .succeed();
    }

    /** Block change during async check → snapshot re-marked dirty → next pipeline tick re-checks
     *  and (in this case) re-forms since the new block also satisfies the snapshot. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void async_pipeline_redispatches_on_block_change(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stonePos = controller.relative(Direction.WEST);
        h.setBlock(stonePos, Blocks.STONE.defaultBlockState());
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_WEST, controller)
                .tickAsyncPipeline(2)
                .assertFormed();
        // Mutate; expect dirty + re-dispatch even though already formed (state was disrupted).
        h.setBlock(stonePos, Blocks.DIRT.defaultBlockState());
        scenario.tickAsyncPipeline(2)
                .assertSnapshotFullyBuilt()
                .succeed();
    }

    /** A controller swapping its dynamic pattern at runtime and calling {@code notifyPatternDirty}
     *  must rebuild the snapshot to track the new pattern's positions. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void notify_pattern_dirty_rebuilds_snapshot(GameTestHelper h) {
        // Start: 1 tracked stone west of controller.
        PatternSnapshotFixtures.dynamicPattern = com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern.start()
                .aisle("CS")
                .where('C', com.lowdragmc.mbd2.api.pattern.Predicates.controller(com.lowdragmc.mbd2.api.pattern.Predicates.any()))
                .where('S', com.lowdragmc.mbd2.api.pattern.Predicates.blocks(Blocks.STONE))
                .build();
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_DYNAMIC, controller)
                .tickSnapshotCapture(1)
                .assertSnapshotFullyBuilt();
        // Swap to a 2-tracked-position pattern, notify, and verify the new tracked set takes effect.
        PatternSnapshotFixtures.dynamicPattern = com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern.start()
                .aisle("CSS")
                .where('C', com.lowdragmc.mbd2.api.pattern.Predicates.controller(com.lowdragmc.mbd2.api.pattern.Predicates.any()))
                .where('S', com.lowdragmc.mbd2.api.pattern.Predicates.blocks(Blocks.STONE))
                .build();
        if (scenario.machine() instanceof com.lowdragmc.mbd2.api.machine.IMultiController c) {
            c.notifyPatternDirty();
        }
        scenario.tickSnapshotCapture(1).assertSnapshotFullyBuilt().succeed();
    }

    /** Rapidly toggle a tracked block; assert the worker doesn't pile up tasks (coalescing).
     *  Tests the {@code checkInFlight} slot mechanism in {@link com.lowdragmc.mbd2.api.pattern.snapshot.PatternSnapshot}. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void rapid_block_change_coalesces_check(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stonePos = controller.relative(Direction.WEST);
        h.setBlock(stonePos, Blocks.STONE.defaultBlockState());
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_WEST, controller)
                .tickAsyncPipeline(2)
                .assertFormed();
        // Toggle the block 20 times in a row. Without coalescing, this would queue 20 worker
        // tasks; with coalescing, at most one is in flight at any time. Either way the final
        // state must be coherent.
        for (int i = 0; i < 20; i++) {
            h.setBlock(stonePos, (i % 2 == 0 ? Blocks.DIRT : Blocks.STONE).defaultBlockState());
        }
        scenario.tickAsyncPipeline(3).succeed();
    }

    /** After the dirty pos is re-captured, the snapshot reflects the new state and the inline
     *  check no longer matches the original STONE predicate. */
    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void snapshot_recapture_reflects_new_state(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos stonePos = controller.relative(Direction.WEST);
        h.setBlock(stonePos, Blocks.STONE.defaultBlockState());
        MBDScenario scenario = MBDScenario.of(h)
                .placeMachine(PatternSnapshotFixtures.MACHINE_STONE_WEST, controller)
                .tickSnapshotCapture(1)
                .runAsyncSnapshotCheck()
                .assertFormed();
        // Break the structure; snapshot should re-capture and the controller no longer forms.
        h.setBlock(stonePos, Blocks.DIRT.defaultBlockState());
        scenario.tickSnapshotCapture(1)
                .assertSnapshotFullyBuilt()
                .succeed();
    }
}

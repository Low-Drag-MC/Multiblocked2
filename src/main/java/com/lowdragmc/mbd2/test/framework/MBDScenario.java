package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.api.pattern.snapshot.PatternSnapshot;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Fluent DSL for writing MBD2 gametests.
 *
 * <pre>{@code
 * MBDScenario.of(helper)
 *     .placeMachine(MBD2.id("test_smelter"), new BlockPos(1, 2, 1))
 *     .insertItem(0, new ItemStack(Items.IRON_ORE, 4))
 *     .insertEnergy(10_000)
 *     .runTicks(200)
 *     .assertItem(1, new ItemStack(Items.IRON_INGOT, 4))
 *     .succeed();
 * }</pre>
 *
 * Each method returns {@code this} (except {@link #succeed()} / {@link #failWith(String)}).
 * The "current machine" is set by {@link #placeMachine} / {@link #placeMultiblock} and
 * used by ingredient I/O and assertion methods unless overridden with {@link #target}.
 */
public class MBDScenario {

    private final GameTestHelper helper;
    @Nullable private MBDMachine current;
    @Nullable private BlockPos currentRelPos;

    private MBDScenario(GameTestHelper helper) {
        this.helper = helper;
    }

    public static MBDScenario of(GameTestHelper helper) {
        return new MBDScenario(helper);
    }

    // region placement / targeting

    public MBDScenario placeMachine(ResourceLocation definitionId, BlockPos relPos) {
        this.current = MBDTestHelper.placeMachine(helper, definitionId, relPos);
        this.currentRelPos = relPos;
        return this;
    }

    /** Place a machine forced to a specific horizontal facing. */
    public MBDScenario placeMachineFacing(ResourceLocation definitionId, BlockPos relPos, Direction facing) {
        this.current = MBDTestHelper.placeMachineFacing(helper, definitionId, relPos, facing);
        this.currentRelPos = relPos;
        return this;
    }

    /** Switch the "current machine" target for subsequent calls. */
    public MBDScenario target(BlockPos relPos) {
        this.current = MBDTestHelper.getMachine(helper, relPos);
        this.currentRelPos = relPos;
        return this;
    }

    public MBDScenario placeBlock(BlockPos relPos, BlockState state) {
        helper.setBlock(relPos, state);
        return this;
    }

    /** Fill every cell in the cuboid (inclusive of both corners) with {@code state}. */
    public MBDScenario fillVolume(BlockPos min, BlockPos max, BlockState state) {
        MBDTestHelper.fillVolume(helper, min, max, state);
        return this;
    }

    /**
     * Place pattern blocks anchored at {@code originRel}. Indexing matches FactoryBlockPattern conventions:
     * {@code blocks[aisleIndex][rowIndex][colIndex]} maps to offsets {@code (col, -row, aisle)} from origin
     * — but for tests we typically just want manual control, so pass {@code [y][z][x]}: outer = Y layer,
     * middle = Z row, inner = X column.
     */
    public MBDScenario placePatternBlocks(BlockPos originRel, BlockState[][][] blocks) {
        MBDTestHelper.placePatternBlocks(helper, originRel, blocks);
        return this;
    }

    /**
     * Place a multiblock controller at {@code controllerPos} and fill the surrounding region
     * with {@code blocks} (indexed [y][z][x]) relative to {@code originRel}. Does NOT trigger
     * formation — call {@link #assertFormed()} (which forces a check) or {@link #formNow()}
     * afterwards.
     */
    public MBDScenario placeMultiblock(ResourceLocation controllerId, BlockPos controllerPos, BlockPos originRel, BlockState[][][] blocks) {
        MBDTestHelper.placePatternBlocks(helper, originRel, blocks);
        return placeMachine(controllerId, controllerPos);
    }

    // endregion

    // region multiblock

    /** Force a pattern check + formation on the current machine. */
    public MBDScenario formNow() {
        requireCurrent();
        MBDTestHelper.tryForm(current);
        return this;
    }

    public MBDScenario assertFormed() {
        requireCurrent();
        // attempt formation if not already
        if (!isMultiblockFormed()) {
            MBDTestHelper.tryForm(current);
        }
        MBDTestHelper.assertFormed(helper, current);
        return this;
    }

    public MBDScenario assertNotFormed() {
        requireCurrent();
        MBDTestHelper.assertNotFormed(helper, current);
        return this;
    }

    // endregion

    // region ingredient I/O

    public MBDScenario insertItem(int slot, ItemStack stack) {
        requireCurrent();
        MBDTestHelper.insertItem(helper, current, slot, stack);
        return this;
    }

    public MBDScenario insertFluid(FluidStack stack) {
        requireCurrent();
        MBDTestHelper.insertFluid(helper, current, stack);
        return this;
    }

    public MBDScenario insertEnergy(int amount) {
        requireCurrent();
        MBDTestHelper.insertEnergy(helper, current, amount);
        return this;
    }

    // endregion

    // region time

    public MBDScenario runTicks(int ticks) {
        MBDTestHelper.runTicks(helper, ticks);
        return this;
    }


    /**
     * Drive {@code ticks} main-thread snapshot captures on the current level's MWSD. Each tick
     * drains up to {@link PatternSnapshot#CAPTURES_PER_TICK} pending positions per snapshot.
     * Use this when you want deterministic capture progress without relying on
     * {@code LevelTickEvent} firing inside the test harness.
     */
    public MBDScenario tickSnapshotCapture(int ticks) {
        var mwsd = mwsd();
        for (int i = 0; i < ticks; i++) {
            mwsd.tickSnapshots();
        }
        return this;
    }

    /** Run one snapshot-backed async check pass against the current controller, inline on
     *  the current thread. Returns this; assertions follow. */
    public MBDScenario runAsyncSnapshotCheck() {
        requireCurrent();
        if (current instanceof IMultiController controller) {
            mwsd().triggerInlineCheck(controller);
        }
        return this;
    }

    /**
     * Drive the real async pipeline: dispatch pending checks to the worker thread, wait for
     * it to finish, then drain the server's main-thread task queue (which is where the worker
     * posts the form-on-main step). Repeats {@code rounds} times — each level tick can both
     * dispatch new work and the worker can submit form requests back, so two rounds typically
     * suffice for the full handshake.
     */
    public MBDScenario tickAsyncPipeline(int rounds) {
        var mwsd = mwsd();
        var server = ((ServerLevel) helper.getLevel()).getServer();
        for (int i = 0; i < rounds; i++) {
            mwsd.tickSnapshots();
            mwsd.dispatchPendingChecks();
            mwsd.awaitAsyncIdleForTests(2_000);
            // Drain main-thread task queue so the worker's server.execute callbacks run.
            while (server.pollTask()) { /* spin */ }
        }
        return this;
    }

    public MBDScenario assertSnapshotFullyBuilt() {
        var snapshot = snapshot();
        if (snapshot == null) {
            helper.fail("No snapshot for current controller");
        } else if (!snapshot.isFullyBuilt()) {
            helper.fail("Snapshot not fully built: " + snapshot.capturedSize() + "/" + snapshot.trackedSize());
        }
        return this;
    }

    public MBDScenario assertSnapshotEntriesAtLeast(int minCount) {
        var snapshot = snapshot();
        if (snapshot == null) {
            helper.fail("No snapshot for current controller");
            return this;
        }
        if (snapshot.capturedSize() < minCount) {
            helper.fail("Expected >= " + minCount + " captured entries; got " + snapshot.capturedSize());
        }
        return this;
    }

    public MBDScenario assertSnapshotPending(int expectedPending) {
        var snapshot = snapshot();
        if (snapshot == null) {
            helper.fail("No snapshot for current controller");
            return this;
        }
        if (snapshot.pendingSize() != expectedPending) {
            helper.fail("Expected " + expectedPending + " pending; got " + snapshot.pendingSize());
        }
        return this;
    }

    @Nullable
    private PatternSnapshot snapshot() {
        requireCurrent();
        if (current instanceof IMultiController controller) {
            return mwsd().getSnapshot(controller);
        }
        return null;
    }

    private MultiblockWorldSavedData mwsd() {
        return MultiblockWorldSavedData.getOrCreate((ServerLevel) helper.getLevel());
    }

    /**
     * Tick until {@code predicate} returns true on the current machine, or up to
     * {@code maxTicks} ticks. Fails the test if the predicate never holds.
     */
    public MBDScenario runUntil(Predicate<MBDMachine> predicate, int maxTicks) {
        requireCurrent();
        for (int i = 0; i < maxTicks; i++) {
            if (predicate.test(current)) return this;
            MBDTestHelper.runTicks(helper, 1);
        }
        if (!predicate.test(current)) {
            helper.fail("Predicate did not hold within " + maxTicks + " ticks");
        }
        return this;
    }

    // endregion

    // region capability assertions

    public <T> MBDScenario assertExposes(BlockCapability<T, @Nullable Direction> cap, @Nullable Direction side) {
        requireCurrent();
        var value = MBDTestHelper.capability(helper, currentRelPos, cap, side);
        if (value == null) {
            helper.fail("Capability " + cap.name() + " not exposed on " + current.getDefinition().id() + " side=" + side);
        }
        return this;
    }

    public <T> MBDScenario assertExposes(BlockCapability<T, @Nullable Direction> cap, @Nullable Direction side, Predicate<T> check) {
        requireCurrent();
        MBDTestHelper.assertCapability(helper, currentRelPos, cap, side, check);
        return this;
    }

    // endregion

    // region content assertions

    public MBDScenario assertItem(int slot, ItemStack expected) {
        requireCurrent();
        MBDTestHelper.assertItemPresent(helper, current, slot, expected);
        return this;
    }

    /**
     * Asserts the slot has a damageable item of the given type with damage value at least {@code minDamage}.
     * Use this when the exact damage may vary (e.g. durability recipes that may run multiple cycles).
     */
    public MBDScenario assertItemDamageAtLeast(int slot, net.minecraft.world.item.Item item, int minDamage) {
        requireCurrent();
        MBDTestHelper.assertItemDamageAtLeast(helper, current, slot, item, minDamage);
        return this;
    }

    /**
     * Asserts the slot has at least {@code minCount} of {@code item}, ignoring components.
     */
    public MBDScenario assertItemCountAtLeast(int slot, net.minecraft.world.item.Item item, int minCount) {
        requireCurrent();
        MBDTestHelper.assertItemCountAtLeast(helper, current, slot, item, minCount);
        return this;
    }

    public MBDScenario assertFluid(int tank, FluidStack expected) {
        requireCurrent();
        MBDTestHelper.assertFluidPresent(helper, current, tank, expected);
        return this;
    }

    public MBDScenario assertEnergyAtLeast(int amount) {
        requireCurrent();
        MBDTestHelper.assertEnergyAtLeast(helper, current, amount);
        return this;
    }

    public MBDScenario assertState(String expectedState) {
        requireCurrent();
        MBDTestHelper.assertMachineState(helper, current, expectedState);
        return this;
    }

    // endregion

    // region persistence

    /**
     * Save the current machine to NBT, recreate the BE, then re-target the scenario at the
     * reloaded machine. Useful for catching sync/persistence bugs.
     */
    public MBDScenario assertPersistenceRoundTrip() {
        requireCurrent();
        this.current = MBDTestHelper.roundTripPersistence(helper, currentRelPos);
        return this;
    }

    // endregion

    // region auto-IO

    /**
     * Place a vanilla block at the side neighbor of the current machine. Use this together
     * with {@link IItemHandler}/{@link IFluidHandler}/{@link IEnergyStorage}-providing blocks
     * (chests, barrels, etc.) to test auto-IO pulls/pushes.
     */
    public MBDScenario withNeighbor(Direction side, BlockState state) {
        requireCurrent();
        helper.setBlock(currentRelPos.relative(side), state);
        return this;
    }

    /**
     * Place a chest as the neighbor on {@code side} and stuff {@code items} into it. Useful for
     * driving auto-IO pull tests.
     */
    public MBDScenario withNeighborItems(Direction side, ItemStack... items) {
        requireCurrent();
        BlockPos neighbor = currentRelPos.relative(side);
        MBDTestHelper.placeChestWithItems(helper, neighbor, items);
        return this;
    }

    /** Read items currently in the chest at the neighbor on {@code side}. */
    public ItemStack[] neighborItems(Direction side) {
        requireCurrent();
        return MBDTestHelper.readChestItems(helper, currentRelPos.relative(side));
    }

    // endregion

    // region entity

    /**
     * Spawn an entity at {@code relPos}. {@code setup} can mutate the entity (e.g. set custom name).
     * Returns this; use {@link MBDTestHelper#spawnEntity} directly if you need a handle on the
     * spawned entity.
     */
    public <T extends Entity> MBDScenario spawnEntity(EntityType<T> type, BlockPos relPos, Consumer<T> setup) {
        MBDTestHelper.spawnEntity(helper, type, relPos, setup);
        return this;
    }

    // endregion

    // region termination

    public void succeed() {
        helper.succeed();
    }

    public void failWith(String message) {
        helper.fail(message);
    }

    // endregion

    // region internals

    private void requireCurrent() {
        if (current == null || currentRelPos == null) {
            helper.fail("No current machine — call placeMachine(...) or target(...) first");
            throw new AssertionError();
        }
    }

    private boolean isMultiblockFormed() {
        if (current instanceof com.lowdragmc.mbd2.api.machine.IMultiController mc) {
            return mc.isFormed();
        }
        return false;
    }

    public GameTestHelper helper() {
        return helper;
    }

    @Nullable
    public MBDMachine machine() {
        return current;
    }

    // endregion
}

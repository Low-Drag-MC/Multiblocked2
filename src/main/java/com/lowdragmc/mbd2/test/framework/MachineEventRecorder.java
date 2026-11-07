package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the recipe-lifecycle {@link MachineEvent}s that machines at given positions post on the NeoForge bus, as
 * short strings — the trace a golden test compares against.
 *
 * <p>Machines are matched by level and <em>position</em>, not identity, so a trace survives
 * {@link MBDScenario#assertPersistenceRoundTrip()} swapping the block entity. Per-tick events carry no per-tick
 * data (no progress), so {@link #compressed()} can fold a run of them into one {@code "X ×N"} entry.</p>
 *
 * <p>Always close it (try-with-resources): an open recorder keeps listening, and a later test could reuse the
 * same absolute positions.</p>
 */
public final class MachineEventRecorder implements AutoCloseable {
    private static final List<MachineEventRecorder> ACTIVE = new CopyOnWriteArrayList<>();
    private static boolean registered;

    private final Level level;
    private final Map<BlockPos, String> labels = new LinkedHashMap<>();
    private final List<String> entries = new ArrayList<>();
    private boolean withLogics;

    private MachineEventRecorder(Level level) {
        this.level = level;
    }

    /** Record the machine at {@code relPos}; entries are unlabelled. */
    public static MachineEventRecorder start(GameTestHelper helper, BlockPos relPos) {
        return start(helper).watch(helper, relPos, "");
    }

    /** Record nothing yet; add positions with {@link #watch}. */
    public static MachineEventRecorder start(GameTestHelper helper) {
        ensureRegistered();
        var recorder = new MachineEventRecorder(helper.getLevel());
        ACTIVE.add(recorder);
        return recorder;
    }

    /** Also record the machine at {@code relPos}; its entries get {@code label + ":"} in front when the label is not empty. */
    public MachineEventRecorder watch(GameTestHelper helper, BlockPos relPos, String label) {
        labels.put(helper.absolutePos(relPos).immutable(), label);
        return this;
    }

    /**
     * Suffix every entry about a recipe logic with {@code @address} of that logic, or {@code @foreign} when the
     * event names a logic of some other machine.
     */
    public MachineEventRecorder withLogics() {
        this.withLogics = true;
        return this;
    }

    private static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        // HIGHEST, so a later listener cancelling the event cannot hide it from the trace
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, MachineEvent.class, MachineEventRecorder::dispatch);
    }

    private static void dispatch(MachineEvent event) {
        for (var recorder : ACTIVE) {
            recorder.accept(event);
        }
    }

    private void accept(MachineEvent event) {
        var machine = event.machine;
        if (machine.getLevel() != level) return;
        var label = labels.get(machine.getPos());
        if (label == null) return;
        var description = describe(event);
        if (description == null) return;
        if (withLogics && event instanceof RecipeLogicEvent logicEvent) {
            var logic = logicEvent.getRecipeLogic();
            description += logic != null && logic.getMachine() == machine ? "@" + logic.getAddress() : "@foreign";
        }
        entries.add(label.isEmpty() ? description : label + ":" + description);
    }

    /**
     * The trace entry for an event, or {@code null} for events the trace ignores (ticks, interaction, UI...).
     */
    @Nullable
    public static String describe(MachineEvent event) {
        return switch (event) {
            case MachineRecipeModifyEvent.Before e -> "RecipeModifyBefore(" + id(e.recipe) + ")";
            case MachineRecipeModifyEvent.After e -> "RecipeModifyAfter(" + id(e.recipe) + ")";
            case MachineBeforeRecipeWorkingEvent e -> "BeforeRecipeWorking(" + id(e.recipe) + ")";
            case MachineOnRecipeWorkingEvent e -> "OnRecipeWorking(" + id(e.recipe) + ")";
            case MachineOnRecipeWaitingEvent e -> "OnRecipeWaiting(" + id(e.recipe) + ")";
            case MachineAfterRecipeWorkingEvent e -> "AfterRecipeWorking(" + id(e.recipe) + ")";
            case MachineOnConsumeInputsAfterWorkingEvent e -> "OnConsumeInputsAfterWorking(" + id(e.recipe) + ")";
            case MachineOnRecipeFinishEvent e -> "OnRecipeFinish(" + id(e.recipe) + ")";
            case MachineFuelRecipeModifyEvent e -> "FuelRecipeModify(" + id(e.recipe) + ")";
            case MachineFuelBurningFinishEvent e -> "FuelBurningFinish(" + id(e.recipe) + ")";
            case MachineRecipeStatusChangedEvent e -> "RecipeStatusChanged(" + e.oldStatus + "->" + e.newStatus + ")";
            case MachineStateChangedEvent e -> "StateChanged(" + e.oldState + "->" + e.newState + ")";
            case MachineStructureFormedEvent e -> "StructureFormed";
            case MachineStructureInvalidEvent e -> "StructureInvalid";
            default -> null;
        };
    }

    private static String id(@Nullable MBDRecipe recipe) {
        return recipe == null ? "null" : recipe.id.getPath();
    }

    /** Every recorded entry, in order. */
    public List<String> raw() {
        return List.copyOf(entries);
    }

    /** {@link #raw()} with each run of identical consecutive entries folded into {@code "entry ×N"}. */
    public List<String> compressed() {
        var result = new ArrayList<String>();
        String last = null;
        int count = 0;
        for (var entry : entries) {
            if (entry.equals(last)) {
                count++;
                continue;
            }
            if (last != null) result.add(count == 1 ? last : last + " ×" + count);
            last = entry;
            count = 1;
        }
        if (last != null) result.add(count == 1 ? last : last + " ×" + count);
        return result;
    }

    /** Forget what was recorded so far, keep recording. */
    public void clear() {
        entries.clear();
    }

    /** Fail the test unless {@link #compressed()} equals {@code expected}, showing both traces. */
    public void assertTrace(GameTestHelper helper, String... expected) {
        var actual = compressed();
        if (!actual.equals(List.of(expected))) {
            helper.fail("event trace differs\n  expected: " + List.of(expected) + "\n  actual:   " + actual);
        }
    }

    @Override
    public void close() {
        ACTIVE.remove(this);
    }
}

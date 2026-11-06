package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.lowdraglib2.syncdata.mark.IMarkStrategy;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.Consumer;

/**
 * Asserts on how LDLib2's sync tells whether a persisted or synced value changed, asking the {@link MarkStrategies}
 * strategy of the value's class the way its refs do. A class without one gets saved every async period just to be
 * compared, so {@link #of} fails for it.
 */
public final class SyncMarks<T> {
    private final GameTestHelper helper;
    private final T value;
    private final IMarkStrategy<Object, Object> strategy;
    private Object mark;

    private SyncMarks(GameTestHelper helper, T value, IMarkStrategy<Object, Object> strategy) {
        this.helper = helper;
        this.value = value;
        this.strategy = strategy;
        this.mark = strategy.obtainManagedMark(value);
    }

    public static <T> SyncMarks<T> of(GameTestHelper helper, T value) {
        var strategy = MarkStrategies.find(value.getClass());
        if (strategy == null) {
            helper.fail(value.getClass().getName() + " has no mark strategy, so LDLib2 saves it every period to compare it");
        }
        return new SyncMarks<>(helper, value, strategy);
    }

    /** Fails unless the sync notices what {@code action} did. */
    public SyncMarks<T> changes(String what, Consumer<T> action) {
        action.accept(value);
        var next = strategy.updateMark(mark, value);
        if (next == null) {
            helper.fail("the sync missed " + what);
        }
        mark = next;
        return this;
    }

    /** Fails if the sync sees a change after {@code action}. */
    public SyncMarks<T> keeps(String what, Consumer<T> action) {
        action.accept(value);
        if (strategy.updateMark(mark, value) != null) {
            helper.fail("the sync saw a change after " + what);
        }
        return this;
    }
}

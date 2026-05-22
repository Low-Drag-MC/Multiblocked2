package com.lowdragmc.mbd2.common.gui.recipe;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data(staticConstructor = "of")
@KJSBindings
public final class SupplierScrollDataSource<T> implements IDataProvider<T>, ITickable {
    @Getter
    private final Supplier<List<T>> dataSupplier;
    private List<T> data;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private volatile T lastValue;
    @Setter
    @Getter @Accessors(chain = true, fluent = true)
    private int frequency = 20;
    // runtime
    @Nullable
    private T current;
    private int counter = 0;

    private SupplierScrollDataSource(Supplier<List<T>> dataSupplier) {
        this.dataSupplier = dataSupplier;
        this.data = dataSupplier.get();
        this.current = data.isEmpty() ? null : data.getFirst();
    }

    public <D> SupplierScrollDataSource<D> map(Function<T, D> mapper) {
        return SupplierScrollDataSource.of(() -> dataSupplier.get().stream().map(mapper).toList());
    }

    @Override
    public ISubscription registerListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public T getValue() {
        return current;
    }

    public void checkUpdate() {
        T currentValue = getValue();
        if (!Objects.equals(lastValue, currentValue)) {
            lastValue = currentValue;
            listeners.forEach(l -> l.accept(currentValue));
        }
    }

    @Override
    public void tick() {
        var latest = dataSupplier.get();
        var changed = false;
        if (!latest.equals(data)) {
            data = latest;
            changed = true;
        }

        if (frequency > 1) {
            if (++counter % frequency != 0 && !changed) return;
        }

        if (data.isEmpty()) {
            current = null;
        } else {
            int step = counter / frequency;
            current = data.get(step % data.size());
        }
        checkUpdate();

        if (counter > 1_000_000_000) {
            counter = 0;
        }
    }
}
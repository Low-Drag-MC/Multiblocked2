package com.lowdragmc.mbd2.common.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by objects that own a {@link RuntimeValueStorage} — {@link MBDMachine} and
 * {@link com.lowdragmc.mbd2.common.trait.Trait}.
 * <p>
 * The storage is declared as a single persisted field named {@link #RUNTIME_VALUES}:
 * <pre>{@code
 * @Persisted @LazyManaged @Getter
 * protected final RuntimeValueStorage runtimeValues = new RuntimeValueStorage(this);
 * }</pre>
 * Persisted, never synced — see {@link RuntimeValueStorage}. The field name has to match, because
 * {@code markDirty} looks a managed field up by its Java field name.
 * <p>
 * {@code @LazyManaged} keeps the per-tick dirty sweep off it entirely — {@link RuntimeValue#set} calls
 * {@link #markRuntimeValuesDirty()} instead. Without it, LDLib would serialize the whole storage and
 * compare it every tick, for every machine and every trait in the world.
 */
public interface IRuntimeValueHolder extends IManaged {
    /** The managed field name the storage must be declared under, so {@code markDirty} can find it. */
    String RUNTIME_VALUES = "runtimeValues";

    RuntimeValueStorage getRuntimeValues();

    /**
     * The machine slot side effects act on. {@link MBDMachine} returns itself, a trait returns its machine.
     * May be null in the editor preview path, where a trait has no real machine behind it.
     */
    @Nullable
    MBDMachine runtimeValueMachine();

    default void markRuntimeValuesDirty() {
        markDirty(RUNTIME_VALUES);
    }
}

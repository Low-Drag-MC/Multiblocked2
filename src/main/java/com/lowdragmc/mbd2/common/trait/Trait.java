package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.IRuntimeValueHolder;
import com.lowdragmc.mbd2.common.runtime.RuntimeValueStorage;
import lombok.Getter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class Trait implements ITrait, IBlockEntityManaged, IRuntimeValueHolder {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    /**
     * Per-machine overrides of values authored on this trait's shared {@link TraitDefinition}. Declared
     * on the base class so every trait gets one; subclasses register their slots as {@code public final}
     * fields, which run after this initialiser.
     * <p>
     * Saved under {@code trait.<traitName>.runtimeValues} — see
     * {@link MBDMachine#loadAdditionalTraits()}, which sets the persisted prefix. Never synced; see
     * {@link RuntimeValueStorage}.
     * <p>
     * {@code @LazyManaged} only removes the field from LDLib's per-tick dirty sweep; it is still in
     * {@code getPersistedFields()} and still saved. {@link RuntimeValue#set} marks it dirty itself.
     */
    @Persisted
    @LazyManaged
    @Getter
    protected final RuntimeValueStorage runtimeValues = new RuntimeValueStorage(this);
    @Getter
    private final MBDMachine machine;
    @Getter
    private final TraitDefinition definition;
    private final List<Runnable> listeners = new ArrayList<>();

    public Trait(MBDMachine machine, TraitDefinition definition) {
        this.machine = machine;
        this.definition = definition;
    }

    @Override
    public MBDMachine runtimeValueMachine() {
        return machine;
    }

    @Override
    public BlockEntity asBlockEntity() {
        return machine.getHolder();
    }

    /**
     * Notify all listeners that the capability has changed.
     */
    public void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    // ***** for recipe trait ***** //
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}

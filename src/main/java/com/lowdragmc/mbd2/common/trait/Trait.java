package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class Trait implements ITrait, IBlockEntityManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
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

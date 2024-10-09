package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A trait that have recipe handling capability.
 */
public abstract class RecipeCapabilityTrait<CONTENT> implements IRecipeCapabilityTrait<CONTENT>, IEnhancedManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Getter
    private final MBDMachine machine;
    @Getter
    private final RecipeCapabilityTraitDefinition<CONTENT> definition;
    private final List<Runnable> listeners = new ArrayList<>();

    public RecipeCapabilityTrait(MBDMachine machine, RecipeCapabilityTraitDefinition<CONTENT> definition) {
        this.machine = machine;
        this.definition = definition;
    }

    @Override
    public void scheduleRenderUpdate() {
        machine.scheduleRenderUpdate();
    }

    @Override
    public void onChanged() {
        machine.onChanged();
    }

    /**
     * Notify all listeners that the capability has changed.
     */
    public void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public RecipeCapability<CONTENT> getRecipeCapability() {
        return getDefinition().getRecipeCapability();
    }

    @Override
    public IO getHandlerIO() {
        return getDefinition().getRecipeHandlerIO();
    }

    @Override
    public boolean isDistinct() {
        return getDefinition().isDistinct();
    }

    @Override
    public Set<String> getSlotNames() {
        return Set.of(getDefinition().getSlotNames());
    }
}

package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A trait that have recipe handling capability.
 */
public abstract class RecipeCapabilityTrait implements ITrait, IEnhancedManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Getter
    private final MBDMachine machine;
    @Getter
    private final RecipeCapabilityTraitDefinition definition;
    private final List<Runnable> listeners = new ArrayList<>();

    public RecipeCapabilityTrait(MBDMachine machine, RecipeCapabilityTraitDefinition definition) {
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

    // ***** for recipe trait ***** //
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public IO getHandlerIO() {
        return getDefinition().getRecipeHandlerIO();
    }

    public boolean isDistinct() {
        return getDefinition().isDistinct();
    }

    public Set<String> getSlotNames() {
        return Arrays.stream(getDefinition().getSlotNames()).collect(Collectors.toSet());
    }

}

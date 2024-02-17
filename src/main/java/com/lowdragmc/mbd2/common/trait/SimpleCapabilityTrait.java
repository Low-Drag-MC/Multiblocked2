package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.api.trait.IRecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class SimpleCapabilityTrait<T, CONTENT> implements IRecipeCapabilityTrait<CONTENT>, ICapabilityProviderTrait<T>, IEnhancedManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Getter
    private final MBDMachine machine;
    @Getter
    private final SimpleCapabilityTraitDefinition<T, CONTENT> definition;
    private final List<Runnable> listeners = new ArrayList<>();

    public SimpleCapabilityTrait(MBDMachine machine, SimpleCapabilityTraitDefinition<T, CONTENT> definition) {
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
    public Capability<T> getCapability() {
        return getDefinition().getCapability();
    }

    @Override
    public IO getCapabilityIO(@Nullable Direction side) {
        var front = machine.getFrontFacing().orElse(Direction.NORTH);
        if (side == null || front.getAxis() == Direction.Axis.Y) {
            return getDefinition().getCapabilityIO().getInternal();
        }
        if (side == Direction.UP) {
            return getDefinition().getCapabilityIO().getTopIO();
        } else if (side == Direction.DOWN) {
            return getDefinition().getCapabilityIO().getBottomIO();
        } else if (side == front) {
            return getDefinition().getCapabilityIO().getFrontIO();
        } else if (side == front.getOpposite()) {
            return getDefinition().getCapabilityIO().getBackIO();
        } else if (side == front.getClockWise()) {
            return getDefinition().getCapabilityIO().getRightIO();
        } else if (side == front.getCounterClockWise()) {
            return getDefinition().getCapabilityIO().getLeftIO();
        }
        return IO.NONE;
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

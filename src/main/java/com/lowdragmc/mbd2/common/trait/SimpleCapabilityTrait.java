package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleCapabilityTrait<T, CONTENT> extends RecipeCapabilityTrait<CONTENT> implements ICapabilityProviderTrait<T> {

    public SimpleCapabilityTrait(MBDMachine machine, SimpleCapabilityTraitDefinition<T, CONTENT> definition) {
        super(machine, definition);
    }

    @Override
    public SimpleCapabilityTraitDefinition<T, CONTENT> getDefinition() {
        return (SimpleCapabilityTraitDefinition<T, CONTENT>)super.getDefinition();
    }

    @Override
    public Capability<? super T> getCapability() {
        return getDefinition().getCapability();
    }

    @Override
    public IO getCapabilityIO(@Nullable Direction side) {
        var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
        return getDefinition().getCapabilityIO().getIO(front, side);
    }

}

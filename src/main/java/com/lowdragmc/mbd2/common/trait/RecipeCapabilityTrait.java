package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A trait that have recipe handling capability.
 */
public abstract class RecipeCapabilityTrait extends Trait {

    public RecipeCapabilityTrait(MBDMachine machine, RecipeCapabilityTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public RecipeCapabilityTraitDefinition getDefinition() {
        return (RecipeCapabilityTraitDefinition) super.getDefinition();
    }

    public IO getHandlerIO() {
        return getDefinition().getRecipeHandlerIO();
    }

    public boolean isDistinct() {
        return getDefinition().isDistinct();
    }

    public Set<String> getSlotNames() {
        return new HashSet<>(getDefinition().getSlotNames());
    }
}

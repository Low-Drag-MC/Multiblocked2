package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import lombok.Getter;

import java.util.Set;

public abstract class RecipeHandlerTrait<CONTENT> implements IRecipeHandlerTrait<CONTENT> {
    public final RecipeCapabilityTrait trait;
    @Getter
    public final RecipeCapability<CONTENT> recipeCapability;

    public RecipeHandlerTrait(RecipeCapabilityTrait trait, RecipeCapability<CONTENT> recipeCapability) {
        this.trait = trait;
        this.recipeCapability = recipeCapability;
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        return trait.addChangedListener(listener);
    }

    @Override
    public IO getHandlerIO() {
        return trait.getHandlerIO();
    }

    @Override
    public boolean isDistinct() {
        return trait.isDistinct();
    }

    @Override
    public Set<String> getSlotNames() {
        return trait.getSlotNames();
    }
}

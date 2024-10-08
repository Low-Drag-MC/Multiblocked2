package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * It is used to append slot names to the recipe handler.
 */
public record RecipeHandlerSlotsProxy<T>(IRecipeHandler<T> proxy, Set<String> slotNames) implements IRecipeHandler<T> {

    @Override
    public List<T> handleRecipeInner(IO io, MBDRecipe recipe, List<T> left, @Nullable String slotName, boolean simulate) {
        return proxy.handleRecipeInner(io, recipe, left, slotName, simulate);
    }

    @Override
    public Set<String> getSlotNames() {
        return slotNames;
    }

    @Override
    public boolean isDistinct() {
        return proxy.isDistinct();
    }

    @Override
    public RecipeCapability<T> getRecipeCapability() {
        return proxy.getRecipeCapability();
    }

    @Override
    public T copyContent(Object content) {
        return proxy.copyContent(content);
    }

    @Override
    public List<T> handleRecipe(IO io, MBDRecipe recipe, List<?> left, @Nullable String slotName, boolean simulate) {
        return proxy.handleRecipe(io, recipe, left, slotName, simulate);
    }

    @Override
    public void preWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        proxy.preWorking(holder, io, recipe);
    }

    @Override
    public void postWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        proxy.postWorking(holder, io, recipe);
    }
}

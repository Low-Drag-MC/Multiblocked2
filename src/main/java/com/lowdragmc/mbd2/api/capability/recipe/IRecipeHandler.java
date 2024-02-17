package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote IRecipeHandler
 */
public interface IRecipeHandler<K> {

    /**
     * matching or handling the given recipe.
     * <br/>
     * Note: it's not always thread-safe.
     * In general, it will be called in the main thread if and only if simulate is true.
     *
     * @param io       the IO type of this recipe. always be one of the {@link IO#IN} or {@link IO#OUT}
     * @param recipe   recipe.
     * @param left     left contents for to be handled.
     * @param slotName specific slot name.
     * @param simulate simulate.
     * @return left contents for continue handling by other proxies.
     * <br>
     * null - nothing left. handling successful/finish. you should always return null as a handling-done mark.
     */
    List<K> handleRecipeInner(IO io, MBDRecipe recipe, List<K> left, @Nullable String slotName, boolean simulate);

    /**
     * Slot name, it makes sense if recipe contents specify a slot name.
     */
    default Set<String> getSlotNames() {
        return Collections.emptySet();
    }

    /**
     * Whether the content of same capability can only be handled distinct.
     */
    default boolean isDistinct() {
        return false;
    }

    /**
     * Refer to the recipe capability.
     */
    RecipeCapability<K> getRecipeCapability();

    /**
     * Copy the content. (deep copy)
     */
    @SuppressWarnings("unchecked")
    default K copyContent(Object content) {
        return getRecipeCapability().copyInner((K)content);
    }

    /**
     * Handle the recipe. you don't need to override/call this method.
     */
    default List<K> handleRecipe(IO io, MBDRecipe recipe, List<?> left, @Nullable String slotName, boolean simulate) {
        return handleRecipeInner(io, recipe, left.stream().map(this::copyContent).collect(Collectors.toList()), slotName, simulate);
    }

    /**
     * It will be executed once {@link RecipeLogic#getStatus()} is entering working.
     * e.g.
     * <br/>
     * idle -> working.
     * <br/>
     * waiting -> working.
     * <br/>
     * ...
     */
    default void preWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
    }

    /**
     * It will be executed once {@link RecipeLogic#getStatus()} is leaving working.
     * e.g.
     * <br/>
     * working -> idle.
     * <br/>
     * working -> waiting.
     * <br/>
     * ...
     */
    default void postWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
    }


}

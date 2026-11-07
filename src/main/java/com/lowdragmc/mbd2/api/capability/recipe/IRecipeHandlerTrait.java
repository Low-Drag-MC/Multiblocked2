package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;

import java.util.Set;

/**
 * @author KilaBash
 * @date 2023/2/25
 * @implNote IRecipeHandlerTrait
 */
public interface IRecipeHandlerTrait<K> extends IRecipeHandler<K> {
    /**
     * Get the IO direction while handing recipe logic
     * {@link RecipeLogic#findAndHandleRecipe()},
     * {@link RecipeLogic#handleTickRecipe(MBDRecipe)} and
     * {@link RecipeLogic#onRecipeFinish()}.
     */
    IO getHandlerIO();

    /**
     * Whether the trait can handle the recipe.
     * @param recipeIO can only be either {@link  IO#IN} or {@link IO#OUT}.
     */
    default boolean compatibleWith(IO recipeIO) {
        return getHandlerIO().support(recipeIO);
    }

    /**
     * Add listener for notification when its internal content changed.
     */
    ISubscription addChangedListener(Runnable listener);

    /**
     * Which recipe logics of the machine may use this handler: logic names (every thread of that logic) or
     * {@code name#thread} addresses. Empty, the default, means all of them.
     *
     * @see RecipeLogic#isInScope(java.util.Collection)
     */
    default Set<String> getRecipeLogicScope() {
        return Set.of();
    }

    /**
     * Whether every change of what this handler holds reaches {@link #addChangedListener} listeners. A recipe logic
     * whose handlers all do stops polling for recipes and searches when told something changed; a single handler
     * answering {@code false} — the default — keeps it polling.
     */
    default boolean notifiesAllChanges() {
        return false;
    }
}

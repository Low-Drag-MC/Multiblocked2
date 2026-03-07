package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;

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
}

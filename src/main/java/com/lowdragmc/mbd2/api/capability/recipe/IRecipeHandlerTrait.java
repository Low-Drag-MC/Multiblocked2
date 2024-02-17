package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
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
     * Add listener for notification when its internal content changed.
     */
    ISubscription addChangedListener(Runnable listener);
}

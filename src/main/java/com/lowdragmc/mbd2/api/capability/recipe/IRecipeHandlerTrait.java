package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;

/**
 * @author KilaBash
 * @date 2023/2/25
 * @implNote IRecipeHandlerTrait
 */
public interface IRecipeHandlerTrait<K> extends IRecipeHandler<K> {
    IO getHandlerIO();

    /**
     * add listener for notification when it changed.
     */
    ISubscription addChangedListener(Runnable listener);
}

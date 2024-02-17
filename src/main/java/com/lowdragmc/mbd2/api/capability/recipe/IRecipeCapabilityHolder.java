package com.lowdragmc.mbd2.api.capability.recipe;

import com.google.common.collect.Table;

import javax.annotation.Nonnull;
import java.util.List;


public interface IRecipeCapabilityHolder {

    default boolean hasProxies() {
        return !getCapabilitiesProxy().isEmpty() && !getCapabilitiesProxy().isEmpty();
    }

    /**
     * Get the recipe capability proxies.
     */
    @Nonnull
    Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getCapabilitiesProxy();

    /**
     * Get Tier for chance boost.
     * if -1, all chanced outputs are voided.
     */
    default int getChanceTier() {
        return 0;
    }

}

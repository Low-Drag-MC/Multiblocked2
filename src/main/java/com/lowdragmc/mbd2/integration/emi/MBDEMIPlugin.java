package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class MBDEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MultiblockInfoEmiCategory.CATEGORY);
        MBD2.LOGGER.info("EMI register");
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registry.addCategory(MBDRecipeTypeEmiCategory.CATEGORIES.apply(recipeType));
            }
        }
        // recipes
        try {
            MultiblockInfoEmiCategory.registerDisplays(registry);
        } catch (NullPointerException ignored){
        }
        MBDRecipeTypeEmiCategory.registerDisplays(registry);
        // workstations
        MultiblockInfoEmiCategory.registerWorkStations(registry);
        MBDRecipeTypeEmiCategory.registerWorkStations(registry);
    }
}

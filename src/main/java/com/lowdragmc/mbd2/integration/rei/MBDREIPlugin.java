package com.lowdragmc.mbd2.integration.rei;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@REIPluginClient
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MBDREIPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
//        registry.add(new MultiblockInfoDisplayCategory());
        MBD2.LOGGER.info("REI register categories");
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            if (recipeType.isXEIVisible()) {
                registry.add(new MBDRecipeTypeDisplayCategory(recipeType));
            }
        }
        MBDRecipeTypeDisplayCategory.registerWorkStations(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        MBD2.LOGGER.info("REI register displays");
        MBDRecipeTypeDisplayCategory.registerDisplays(registry);
//        MultiblockInfoDisplayCategory.registerDisplays(registry);
    }

}

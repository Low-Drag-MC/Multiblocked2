package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.recipe.*;
import com.lowdragmc.mbd2.integration.create.CreateRotationCondition;
import com.lowdragmc.mbd2.integration.mekanism.MEKTemperatureCondition;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCTemperatureCondition;
import net.neoforged.fml.ModLoader;

public final class MBDRecipeConditions {

    private MBDRecipeConditions() {}

    public static void init() {
        MBDRegistries.RECIPE_CONDITIONS.unfreeze();
        register(BiomeCondition.INSTANCE);
        register(DimensionCondition.INSTANCE);
        register(PositionYCondition.INSTANCE);
        register(RainingCondition.INSTANCE);
        register(ThunderCondition.INSTANCE);
        register(MachineLevelCondition.INSTANCE);
        register(MachineNBTCondition.INSTANCE);
        register(BlockCondition.INSTANCE);
        register(DayLightCondition.INSTANCE);
        register(RedstoneSignalCondition.INSTANCE);
        if (MBD2.isCreateLoaded()) {
            register(CreateRotationCondition.INSTANCE);
        }
        if (MBD2.isMekanismLoaded()) {
            register(MEKTemperatureCondition.INSTANCE);
        }
        if (MBD2.isPneumaticCraftLoaded()) {
            register(PNCTemperatureCondition.INSTANCE);
        }
        ModLoader.postEvent(new MBDRegistryEvent.RecipeCondition());
        MBDRegistries.RECIPE_CONDITIONS.freeze();
    }

    public static void register(RecipeCondition recipeCondition) {
        MBDRegistries.RECIPE_CONDITIONS.register(recipeCondition.getType(), recipeCondition);
    }
}

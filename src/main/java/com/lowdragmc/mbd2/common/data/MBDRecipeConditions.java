package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.recipe.*;

public final class MBDRecipeConditions {

    private MBDRecipeConditions() {}

    public static void init() {
        MBDRegistries.RECIPE_CONDITIONS.unfreeze();
        MBDRegistries.RECIPE_CONDITIONS.register(BiomeCondition.INSTANCE.getType(), BiomeCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(DimensionCondition.INSTANCE.getType(), DimensionCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(PositionYCondition.INSTANCE.getType(), PositionYCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(RainingCondition.INSTANCE.getType(), RainingCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(ThunderCondition.INSTANCE.getType(), ThunderCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.freeze();
    }
}

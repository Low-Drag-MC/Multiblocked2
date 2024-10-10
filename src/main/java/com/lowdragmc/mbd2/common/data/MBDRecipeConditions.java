package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.recipe.*;
import net.minecraftforge.fml.ModLoader;

public final class MBDRecipeConditions {

    private MBDRecipeConditions() {}

    public static void init() {
        MBDRegistries.RECIPE_CONDITIONS.unfreeze();
        MBDRegistries.RECIPE_CONDITIONS.register(BiomeCondition.INSTANCE.getType(), BiomeCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(DimensionCondition.INSTANCE.getType(), DimensionCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(PositionYCondition.INSTANCE.getType(), PositionYCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(RainingCondition.INSTANCE.getType(), RainingCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(ThunderCondition.INSTANCE.getType(), ThunderCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(MachineLevelCondition.INSTANCE.getType(), MachineLevelCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(MachineCustomDataCondition.INSTANCE.getType(), MachineCustomDataCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(BlockCondition.INSTANCE.getType(), BlockCondition.class);
        ModLoader.get().postEvent(new MBDRegistryEvent.RecipeCondition());
        MBDRegistries.RECIPE_CONDITIONS.freeze();
    }
}

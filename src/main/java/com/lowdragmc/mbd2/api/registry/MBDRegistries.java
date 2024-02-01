package com.lowdragmc.mbd2.api.registry;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;

public class MBDRegistries {
    public static final MBDRegistry.RL<MBDMachineDefinition> MACHINE_DEFINITIONS = new MBDRegistry.RL<>(MBD2.id("machine_definition"));
    public static final MBDRegistry.RL<MBDRecipeType> RECIPE_TYPES = new MBDRegistry.RL<>(MBD2.id("recipe_type"));
    public static final MBDRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new MBDRegistry.String<>(MBD2.id("recipe_capability"));
    public static final MBDRegistry.String<Class<? extends RecipeCondition>> RECIPE_CONDITIONS = new MBDRegistry.String<>(MBD2.id("recipe_condition"));

}

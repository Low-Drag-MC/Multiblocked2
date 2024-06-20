package com.lowdragmc.mbd2.common.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class MBDRegistryEvent extends Event implements IModBusEvent {
    public static class Machine extends MBDRegistryEvent {
        /**
         * Register a machine definition.
         */
        public void register(MBDMachineDefinition definition) {
            MBDRegistries.MACHINE_DEFINITIONS.register(definition.id(), definition);
        }
    }

    public static class MBDRecipeType extends MBDRegistryEvent {
        /**
         * Register a recipe type.
         */
        public void register(com.lowdragmc.mbd2.api.recipe.MBDRecipeType recipeType) {
            ForgeRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
            ForgeRegistries.RECIPE_SERIALIZERS.register(recipeType.registryName, new MBDRecipeSerializer());
            MBDRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
        }
    }

    public static class RecipeCondition extends MBDRegistryEvent {
        /**
         * Register a recipe condition.
         */
        public void register(String id, Class<? extends com.lowdragmc.mbd2.api.recipe.RecipeCondition> condition) {
            MBDRegistries.RECIPE_CONDITIONS.register(id, condition);
        }
    }

    public static class RecipeCapability extends MBDRegistryEvent {
        /**
         * Register a recipe capability.
         */
        public void register(String id, com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability<?> capability) {
            MBDRegistries.RECIPE_CAPABILITIES.register(id, capability);
        }
    }

}

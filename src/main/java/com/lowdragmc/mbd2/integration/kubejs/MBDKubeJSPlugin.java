package com.lowdragmc.mbd2.integration.kubejs;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.kubejs.events.*;
import com.lowdragmc.mbd2.integration.kubejs.recipe.MBDRecipeSchema;
import dev.latvian.mods.kubejs.core.RecipeManagerKJS;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.TypeWrapperRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.Map;

public class MBDKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.lowdragmc.mbd2");
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        MBDServerEvents.init();
        if (LDLib2.isClient()) {
            MBDClientEvents.init();
        }
        registry.register(MBDStartupEvents.REGISTRY_EVENTS);
        registry.register(MBDRecipeTypeEvents.MBD_RECIPE_TYPE_EVENTS);
        registry.register(MBDMachineEvents.MBD_MACHINE_EVENTS);
    }

    @Override
    public void registerTypeWrappers(TypeWrapperRegistry registry) {
//        registry.register(MBDRecipeSchema.FluidIngredientJS.class, MBDRecipeSchema.FluidIngredientJS::of);
//        registry.register(MBDRecipeSchema.EntityIngredientJS.class, MBDRecipeSchema.EntityIngredientJS::of);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            registry.register(recipeType.getRegistryName(), MBDRecipeSchema.SCHEMA);
        }
    }

    @Override
    public void injectRuntimeRecipes(RecipesKubeEvent event, RecipeManagerKJS manager, Map<ResourceLocation, RecipeHolder<?>> recipesByName) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            recipeType.onRecipeManagerLoadedKjs(recipesByName);
        }
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Shapes", Shapes.class);
    }
}

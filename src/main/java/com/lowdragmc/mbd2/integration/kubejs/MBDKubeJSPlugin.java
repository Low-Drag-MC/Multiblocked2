package com.lowdragmc.mbd2.integration.kubejs;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.trait.CapabilityIO;
import com.lowdragmc.mbd2.integration.create.machine.CreateMachineState;
import com.lowdragmc.mbd2.integration.kubejs.events.*;
import com.lowdragmc.mbd2.integration.kubejs.recipe.MBDRecipeSchema;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.Map;

public class MBDKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        super.registerClasses(type, filter);
        filter.allow("com.lowdragmc.mbd2");
    }

    @Override
    public void registerEvents() {
        super.registerEvents();
        MBDServerEvents.init();
        if (LDLib.isClient()) {
            MBDClientEvents.init();
        }
        MBDStartupEvents.REGISTRY_EVENTS.register();
        MBDMachineEvents.MBD_MACHINE_EVENTS.register();
        MBDRecipeTypeEvents.MBD_RECIPE_TYPE_EVENTS.register();
    }

    @Override
    public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        super.registerTypeWrappers(type, typeWrappers);
        typeWrappers.registerSimple(MBDRecipeSchema.FluidIngredientJS.class, MBDRecipeSchema.FluidIngredientJS::of);
        typeWrappers.registerSimple(MBDRecipeSchema.EntityIngredientJS.class, MBDRecipeSchema.EntityIngredientJS::of);
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        super.registerRecipeSchemas(event);
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            event.register(recipeType.getRegistryName(), MBDRecipeSchema.SCHEMA);
        }
    }

    @Override
    public void injectRuntimeRecipes(RecipesEventJS event, RecipeManager manager, Map<ResourceLocation, Recipe<?>> recipesByName) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            recipeType.onRecipeManagerLoadedKjs(recipesByName);
        }
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);
        event.add("MachineState", MachineState.class);
        if (MBD2.isCreateLoaded()) {
            event.add("CreateMachineState", CreateMachineState.class);
        }
        event.add("MBDRegistries", MBDRegistries.class);
        event.add("Shapes", Shapes.class);
        event.add("ConfigBlockProperties", ConfigBlockProperties.class);
        event.add("IO", IO.class);
        event.add("CapabilityIO", CapabilityIO.class);
        event.add("ContentModifier", ContentModifier.class);
    }


}

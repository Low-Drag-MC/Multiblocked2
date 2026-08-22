package com.lowdragmc.mbd2.integration.kubejs;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.kubejs.events.*;
import com.lowdragmc.mbd2.integration.kubejs.recipe.MBDRecipeSchema;
import com.lowdragmc.mbd2.integration.kubejs.wrapper.EntityIngredientWrapper;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.TypeWrapperRegistry;
import net.minecraft.world.phys.shapes.Shapes;

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
            registry.register(MBDClientEvents.MBD_CLIENT_EVENTS);
        }
        registry.register(MBDStartupEvents.REGISTRY_EVENTS);
        registry.register(MBDRecipeTypeEvents.MBD_RECIPE_TYPE_EVENTS);
        registry.register(MBDMachineEvents.MBD_MACHINE_EVENTS);
    }

    @Override
    public void registerTypeWrappers(TypeWrapperRegistry registry) {
        registry.register(EntityIngredient.class, EntityIngredientWrapper::wrap);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        for (var recipeType : MBDRegistries.RECIPE_TYPES) {
            registry.register(recipeType.getRegistryName(), MBDRecipeSchema.SCHEMA);
        }
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Shapes", Shapes.class);
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        // called once per script type. Only the client pass may touch MBDClientEvents, it pulls in Dist.CLIENT
        // classes that a dedicated server cannot load.
        if (LDLib2.isClient() && manager.scriptType == ScriptType.CLIENT) {
            MBDClientEvents.reloadCustomRenderers();
        }
    }
}

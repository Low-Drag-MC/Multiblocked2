package com.lowdragmc.mbd2.common.event;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.CommonProxy;
import com.lowdragmc.mbd2.common.data.MBDMachineDefinitionTypes;
import com.lowdragmc.mbd2.common.data.MBDTraitDefinitionTypes;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import net.minecraft.nbt.NbtIo;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;

public class MBDRegistryEvent extends Event implements IModBusEvent {

    public static class Machine extends MBDRegistryEvent {
        /**
         * Register a machine definition.
         */
        public void register(MBDMachineDefinition definition) {
            MBDRegistries.MACHINE_DEFINITIONS.register(definition.id(), definition);
        }

        /**
         * Register a machine definition from a file.
         */
        public void registerFromFile(String type, Path file) {
            var definitionType = MBDRegistries.MACHINE_DEFINITION_TYPES.get(type);
            if (definitionType == null) {
                LDLib2.LOGGER.error("error could not find the definition type {} from file {}", type, path);
                return;
            }
            try {
                var tag = NbtIo.read(file);
                if (tag == null) throw new Exception("tag is null");
                register(definitionType.creator().get().loadProductiveTag(null, tag, CommonProxy.getPostTask()));
            } catch (Exception e) {
                LDLib2.LOGGER.error("error could not load the project from file {}", path, e);
            }
        }

        /**
         * Register a machine definition from a resource.
         */
        public void registerFromResource(Class<?> source, String type, String projectFile) {
            var definitionType = MBDRegistries.MACHINE_DEFINITION_TYPES.get(type);
            if (definitionType == null) {
                LDLib2.LOGGER.error("error could not find the definition type {} from resource {}", type, projectFile);
                return;
            }

            var inputstream = source.getResourceAsStream(String.format("/assets/%s", projectFile));
            if (inputstream == null) {
                LDLib2.LOGGER.error("error could not find the project from resource {}", projectFile);
                return;
            }
            try {
                var tag = NbtIo.read(new DataInputStream(inputstream));
                register(definitionType.creator().get().loadProductiveTag(null, tag, CommonProxy.getPostTask()));
            } catch (Exception e) {
                LDLib2.LOGGER.error("error could not load the project from resource {}", projectFile, e);
            }
        }
    }

    public static class MBDRecipeType extends MBDRegistryEvent {
        /**
         * Register a recipe type.
         */
        public void register(com.lowdragmc.mbd2.api.recipe.MBDRecipeType recipeType) {
            MBDRegistries.RECIPE_TYPES.register(recipeType.getRegistryName(), recipeType);
        }

        /**
         * Register a recipe type from a file.
         */
        public void registerFromFile(File file) {
            try {
                var tag = NbtIo.read(file.toPath());
                if (tag == null) throw new Exception("tag is null");
                register(com.lowdragmc.mbd2.api.recipe.MBDRecipeType.createDefault().loadProductiveTag(null, tag, CommonProxy.getPostTask()));
            } catch (Exception e) {
                LDLib2.LOGGER.error("error could not load the project from file {}", file, e);
            }
        }

        /**
         * Register a machine definition from a resource.
         */
        public void registerFromResource(Class<?> source, String projectFile) {
            var inputstream = source.getResourceAsStream(String.format("/assets/%s", projectFile));
            if (inputstream == null) {
                LDLib2.LOGGER.error("error could not find the project from resource {}", projectFile);
                return;
            }
            try {
                var tag = NbtIo.read(new DataInputStream(inputstream));
                register(com.lowdragmc.mbd2.api.recipe.MBDRecipeType.createDefault().loadProductiveTag(null, tag, CommonProxy.getPostTask()));
            } catch (Exception e) {
                LDLib2.LOGGER.error("error could not load the project from resource {}", projectFile, e);
            }
        }
    }

    public static class RecipeCondition extends MBDRegistryEvent {
        /**
         * Register a recipe condition.
         */
        public void register(String id, com.lowdragmc.mbd2.api.recipe.RecipeCondition condition) {
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


    public static class MachineDefinitionType extends MBDRegistryEvent {
        /**
         * Register a machine definition type.
         */
        public <T extends MBDMachineDefinition> void register(Class<T> clazz, Supplier<T> creator) {
            MBDMachineDefinitionTypes.register(clazz, creator);
        }
    }

    public static class TraitType extends MBDRegistryEvent {
        /**
         * Register a trait definition.
         */
        public void register(Class<? extends TraitDefinition> clazz) {
            MBDTraitDefinitionTypes.register(clazz);
        }
    }

}

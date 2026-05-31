package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.create.machine.ConfigKineticMachineSettings;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class CreateKineticMachineFixtures implements TestFixtureProvider {
    public static final ResourceLocation GENERATOR_MACHINE_ID = MBD2.id("test_create_kinetic_generator");
    public static final ResourceLocation CONSUMER_MACHINE_ID = MBD2.id("test_create_kinetic_consumer");
    public static final ResourceLocation SMALL_COG_CONSUMER_ID = MBD2.id("test_create_kinetic_small_cog");
    public static final ResourceLocation LARGE_COG_CONSUMER_ID = MBD2.id("test_create_kinetic_large_cog");
    public static final ResourceLocation GENERATOR_RECIPE_TYPE_ID = MBD2.id("test_create_kinetic_generator_recipes");
    public static final ResourceLocation CONSUMER_RECIPE_TYPE_ID = MBD2.id("test_create_kinetic_consumer_recipes");

    public static MBDRecipeType generatorRecipeType;
    public static MBDRecipeType consumerRecipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        generatorRecipeType = TestRecipeTypeBuilder.of(GENERATOR_RECIPE_TYPE_ID)
                // 1 dirt -> 256 stress output (generator-side recipe)
                .recipe("kinetic_dirt_to_stress", b -> b
                        .inputItems(Items.DIRT)
                        .output(CreateRotationRecipeCapability.CAP, CreateRotation.stress(256f))
                        .duration(20))
                .register(event);
        consumerRecipeType = TestRecipeTypeBuilder.of(CONSUMER_RECIPE_TYPE_ID)
                // 32 RPM input -> 1 dirt output (consumer-side recipe)
                .recipe("kinetic_rpm_to_dirt", b -> b
                        .input(CreateRotationRecipeCapability.CAP, CreateRotation.rpm(32f))
                        .outputItems(Items.DIRT)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // kineticMachineSettings must be set on the Builder first (parent setters return parent type and lose the kinetic-specific setter)
        var generatorBuilder = CreateKineticMachineDefinition.builder()
                .kineticMachineSettings(ConfigKineticMachineSettings.builder()
                        .isGenerator(true).torque(8f).maxRPM(256).build());
        generatorBuilder
                .id(GENERATOR_MACHINE_ID)
                .rootState(StateMachine.createDefault(MachineState::baseBuilder))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .machineSettings(() -> ConfigMachineSettings.builder().build())
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().recipeType(GENERATOR_RECIPE_TYPE_ID).build())
                .partSettings(() -> ConfigPartSettings.builder().build());
        event.register(generatorBuilder.build());

        var consumerBuilder = CreateKineticMachineDefinition.builder()
                .kineticMachineSettings(ConfigKineticMachineSettings.builder()
                        .isGenerator(false).torque(4f).maxRPM(256).build());
        consumerBuilder
                .id(CONSUMER_MACHINE_ID)
                .rootState(StateMachine.createDefault(MachineState::baseBuilder))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .machineSettings(() -> ConfigMachineSettings.builder().build())
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().recipeType(CONSUMER_RECIPE_TYPE_ID).build())
                .partSettings(() -> ConfigPartSettings.builder().build());
        event.register(consumerBuilder.build());

        var smallCogBuilder = CreateKineticMachineDefinition.builder()
                .kineticMachineSettings(ConfigKineticMachineSettings.builder()
                        .isGenerator(false).torque(4f).maxRPM(256)
                        .connectionType(ConfigKineticMachineSettings.ConnectionType.SMALL_COGWHEEL).build());
        smallCogBuilder
                .id(SMALL_COG_CONSUMER_ID)
                .rootState(StateMachine.createDefault(MachineState::baseBuilder))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .machineSettings(() -> ConfigMachineSettings.builder().build())
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().recipeType(CONSUMER_RECIPE_TYPE_ID).build())
                .partSettings(() -> ConfigPartSettings.builder().build());
        event.register(smallCogBuilder.build());

        var largeCogBuilder = CreateKineticMachineDefinition.builder()
                .kineticMachineSettings(ConfigKineticMachineSettings.builder()
                        .isGenerator(false).torque(4f).maxRPM(256)
                        .connectionType(ConfigKineticMachineSettings.ConnectionType.LARGE_COGWHEEL).build());
        largeCogBuilder
                .id(LARGE_COG_CONSUMER_ID)
                .rootState(StateMachine.createDefault(MachineState::baseBuilder))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .machineSettings(() -> ConfigMachineSettings.builder().build())
                .recipeLogicSettings(ConfigRecipeLogicSettings.builder().recipeType(CONSUMER_RECIPE_TYPE_ID).build())
                .partSettings(() -> ConfigPartSettings.builder().build());
        event.register(largeCogBuilder.build());
    }
}

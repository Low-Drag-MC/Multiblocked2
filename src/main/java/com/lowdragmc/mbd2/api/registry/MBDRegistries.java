package com.lowdragmc.mbd2.api.registry;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import lombok.Getter;
import net.minecraft.world.phys.shapes.Shapes;

public class MBDRegistries {
    @Getter(lazy = true)
    private final static MBDMachineDefinition FAKE_MACHINE = createFakeMachine();
    private static MBDMachineDefinition createFakeMachine() {
        return MBDMachineDefinition.builder()
                .id(MBD2.id("fake_machine"))
                .stateMachine(new StateMachine(MachineState.builder()
                        .name("base")
                        .renderer(IRenderer.EMPTY)
                        .shape(Shapes.block())
                        .lightLevel(0)
                        .build()))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .build();
    }
    public static final MBDRegistry.RL<MBDMachineDefinition> MACHINE_DEFINITIONS = new MBDRegistry.RL<>(MBD2.id("machine_definition"));
    public static final MBDRegistry.RL<MBDRecipeType> RECIPE_TYPES = new MBDRegistry.RL<>(MBD2.id("recipe_type"));
    public static final MBDRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new MBDRegistry.String<>(MBD2.id("recipe_capability"));
    public static final MBDRegistry.String<Class<? extends RecipeCondition>> RECIPE_CONDITIONS = new MBDRegistry.String<>(MBD2.id("recipe_condition"));

}

package com.lowdragmc.mbd2.api.registry;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.common.item.MBDGadgetsItem;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleCreativeTab;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.phys.shapes.Shapes;

public class MBDRegistries {
    @Getter(lazy = true)
    @Accessors(fluent = true)
    private static final MBDGadgetsItem GADGETS_ITEM = createGadgetsItem();
    @Getter(lazy = true)
    @Accessors(fluent = true)
    private static final MBDMachineDefinition FAKE_MACHINE = createFakeMachine();
    private static MBDGadgetsItem createGadgetsItem() {
        return new MBDGadgetsItem();
    }
    private static MBDMachineDefinition createFakeMachine() {
        return MBDMachineDefinition.builder()
                .id(MBD2.id("fake_machine"))
                .rootState(MachineState.builder()
                        .name("base")
                        .renderer(IRenderer.EMPTY)
                        .shape(Shapes.block())
                        .lightLevel(0)
                        .build())
                .blockProperties(ConfigBlockProperties.builder().rotationState(RotationState.ALL).build())
                .itemProperties(ConfigItemProperties.builder()
                        .creativeTab(new ToggleCreativeTab())
                        .build())
                .build();
    }

    public static final MBDRegistry.RL<MBDMachineDefinition> MACHINE_DEFINITIONS = new MBDRegistry.RL<>(MBD2.id("machine_definition"));
    public static final MBDRegistry.RL<MBDRecipeType> RECIPE_TYPES = new MBDRegistry.RL<>(MBD2.id("recipe_type"));
    public static final MBDRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new MBDRegistry.String<>(MBD2.id("recipe_capability"));
    public static final MBDRegistry.String<AnnotationDetector.Wrapper<LDLRegister, ? extends TraitDefinition>> TRAIT_DEFINITIONS = new MBDRegistry.String<>(MBD2.id("trait_definition"));
    public static final MBDRegistry.String<Class<? extends RecipeCondition>> RECIPE_CONDITIONS = new MBDRegistry.String<>(MBD2.id("recipe_condition"));

}

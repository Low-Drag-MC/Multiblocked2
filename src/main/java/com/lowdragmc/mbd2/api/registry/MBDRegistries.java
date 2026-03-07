package com.lowdragmc.mbd2.api.registry;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@KJSBindings
public class MBDRegistries {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MBD2.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MBD2.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MBD2.MOD_ID);

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
                .rootState(MachineState.baseBuilder()
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

    public static MBDMachineDefinition getFakeMachineDefinition() {
        return FAKE_MACHINE();
    }

    public final static AutoRegistry.LDLibRegister<PatternPredicate, Supplier<PatternPredicate>> PATTERN_PREDICATES = AutoRegistry.LDLibRegister
            .create(MBD2.id("pattern_predicate"), PatternPredicate.class, AutoRegistry::noArgsCreator);
    public final static AutoRegistry.LDLibRegister<RecipeCondition, Supplier<RecipeCondition>> RECIPE_CONDITIONS = AutoRegistry.LDLibRegister
            .create(MBD2.id("recipe_condition"), RecipeCondition.class, AutoRegistry::noArgsCreator);
    public final static AutoRegistry.LDLibRegister<MBDMachineDefinition, Supplier<MBDMachineDefinition>> MACHINE_DEFINITION_TYPES = AutoRegistry.LDLibRegister
            .create(MBD2.id("machine_definition_type"), MBDMachineDefinition.class, AutoRegistry::noArgsCreator);
    public final static AutoRegistry.LDLibRegister<TraitDefinition, Supplier<TraitDefinition>> TRAIT_DEFINITION_TYPES = AutoRegistry.LDLibRegister
            .create(MBD2.id("trait_definition_type"), TraitDefinition.class, AutoRegistry::noArgsCreator);

    public static final MBDRegistry.RL<MBDMachineDefinition> MACHINE_DEFINITIONS = new MBDRegistry.RL<>(MBD2.id("machine_definition"));
    public static final MBDRegistry.RL<MBDRecipeType> RECIPE_TYPES = new MBDRegistry.RL<>(MBD2.id("recipe_type"));
    public static final MBDRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new MBDRegistry.String<>(MBD2.id("recipe_capability"));

    public static void init() {
        PATTERN_PREDICATES.register("air", AutoRegistry.Holder.of(
                PatternPredicate.AIR.getClass().getAnnotation(LDLRegister.class),
                PatternPredicate.AIR.getClass(),
                () -> PatternPredicate.AIR)
        );
        PATTERN_PREDICATES.register("any", AutoRegistry.Holder.of(
                PatternPredicate.ANY.getClass().getAnnotation(LDLRegister.class),
                PatternPredicate.ANY.getClass(),
                () -> PatternPredicate.ANY)
        );
        TRAIT_DEFINITION_TYPES.register("empty", AutoRegistry.Holder.of(
                TraitDefinition.EMPTY.getClass().getAnnotation(LDLRegister.class),
                TraitDefinition.EMPTY.getClass(),
                () -> TraitDefinition.EMPTY)
        );
    }
}

package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MachineDefinitionType;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigRecipeLogicSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.api.stress.BlockStressValues;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public class CreateKineticMachineDefinition extends MBDMachineDefinition {
    @LDLRegister(name = "create_machine", registry = "mbd2:machine_definition_type", modID = "create")
    public static final MachineDefinitionType<CreateKineticMachineDefinition> TYPE =
            new MachineDefinitionType<>("create_machine", "create_machine", ".cm") {
                @Override
                public CreateKineticMachineDefinition createDefinition() {
                    return CreateKineticMachineDefinition.createDefault();
                }

                @Override
                public ProjectType getEditorProjectType() {
                    return CreateKineticMachineProject.TYPE;
                }
            };

    @Configurable(name = "config.definition.kinetic_machine_settings", subConfigurable = true,
            tips = "config.definition.kinetic_machine_settings.tooltip", collapse = false)
    protected final ConfigKineticMachineSettings kineticMachineSettings;

    protected CreateKineticMachineDefinition(ResourceLocation id, MachineState rootState,
                                             @Nullable ConfigBlockProperties blockProperties,
                                             @Nullable ConfigItemProperties itemProperties,
                                             @Nullable ConfigMachineSettingsFactory machineSettings,
                                             @Nullable ConfigRecipeLogicSettings recipeLogicSettings,
                                             @Nullable ConfigPartSettingsFactory partSettings,
                                             @Nullable ConfigKineticMachineSettings kineticMachineSettings) {
        super(id, rootState, blockProperties, itemProperties, machineSettings, recipeLogicSettings, partSettings);
        this.kineticMachineSettings = kineticMachineSettings == null ? ConfigKineticMachineSettings.builder().build() : kineticMachineSettings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CreateKineticMachineDefinition createDefault() {
        return new CreateKineticMachineDefinition(
                MBD2.id("dummy"),
                StateMachine.createDefault(CreateMachineState::builder),
                ConfigBlockProperties.builder().build(),
                ConfigItemProperties.builder().build(),
                () -> ConfigMachineSettings.builder().build(),
                ConfigRecipeLogicSettings.builder().build(),
                () -> ConfigPartSettings.builder().build(),
                ConfigKineticMachineSettings.builder().build());
    }

    @Override
    public MachineState createDefaultRootState() {
        return StateMachine.createDefault(CreateMachineState::builder);
    }

    @Override
    public void loadFactory() {
        super.loadFactory();
        // Auto-include the mandatory CreateRotationTrait so users never have to add it manually,
        // and can't add it twice. Mandatory + !allowMultiple is enforced in MachineTraitView's menu.
        var defs = machineSettings().traitDefinitions();
        boolean present = false;
        for (var t : defs) {
            if (t instanceof CreateRotationTrait.CreateRotationTraitDefinition) { present = true; break; }
        }
        if (!present) {
            machineSettings().addTraitDefinition(CreateRotationTrait.DEFINITION);
        }
    }

    @Override
    public MBDMachineDefinition loadProductiveTag(@org.jetbrains.annotations.Nullable java.io.File file,
                                                  net.minecraft.nbt.CompoundTag projectTag,
                                                  java.util.Deque<Runnable> postTask) {
        super.loadProductiveTag(file, projectTag, postTask);
        // kineticMachineSettings must be loaded immediately (not in postTask) because createBlock()
        // reads isGenerator/torque to register Create stress values during block registration.
        var definitionTag = projectTag.getCompound("data").getCompound("definition");
        kineticMachineSettings.deserializeNBT(com.lowdragmc.lowdraglib2.Platform.getFrozenRegistry(),
                definitionTag.getCompound("kineticMachineSettings"));
        return this;
    }

    @Override
    public Block createBlock() {
        var block = new MBDKineticMachineBlock(blockProperties().apply(stateMachine(), BlockBehaviour.Properties.of()), this);
        if (kineticMachineSettings.isGenerator) {
            BlockStressValues.CAPACITIES.register(block, kineticMachineSettings::getCapacity);
            BlockStressValues.RPM.register(block, new BlockStressValues.GeneratedRpm(kineticMachineSettings.maxRPM, true));
        } else {
            BlockStressValues.IMPACTS.register(block, kineticMachineSettings::getImpact);
        }
        return block;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MBDKineticMachineBlockEntity(this, blockEntityType(), pos, state, this::createMachine);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initRenderer(EntityRenderersEvent.RegisterRenderers event) {
        super.initRenderer(event);
        @SuppressWarnings("unchecked")
        BlockEntityType<MBDKineticMachineBlockEntity> beType = (BlockEntityType<MBDKineticMachineBlockEntity>) blockEntityType();
        SimpleBlockEntityVisualizer.builder(beType)
                .factory((mgr, be, pt) -> new MBDKineticInstance(mgr, be, pt, getRotationPartialModel()))
                .skipVanillaRender(be -> false)
                .apply();
    }

    /**
     * Resolve the user-configured rotation model from the root {@link CreateMachineState} when present,
     * falling back to {@link AllPartialModels#SHAFT_HALF}.
     */
    @OnlyIn(Dist.CLIENT)
    public PartialModel getRotationPartialModel() {
        if (stateMachine().getRootState() instanceof CreateMachineState createState) {
            return createState.getRotationPartialModel();
        }
        return AllPartialModels.SHAFT_HALF;
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {
        protected ConfigKineticMachineSettings kineticMachineSettings;

        protected Builder() {
        }

        public CreateKineticMachineDefinition build() {
            return new CreateKineticMachineDefinition(id, rootState, blockProperties, itemProperties,
                    machineSettings, recipeLogicSettings, partSettings, kineticMachineSettings);
        }
    }
}

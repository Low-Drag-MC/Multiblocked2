package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.api.stress.BlockStressValues;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Deque;

@Getter
@Accessors(fluent = true)
@LDLRegister(name = "create_machine", group = "machine_definition", modID = "create")
public class CreateKineticMachineDefinition extends MBDMachineDefinition {
    @Configurable(name = "config.definition.kinetic_machine_settings", subConfigurable = true, tips = "config.definition.kinetic_machine_settings.tooltip", collapse = false)
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

    @Override
    public MachineState createDefaultRootState() {
        return StateMachine.createDefault(CreateMachineState::builder);
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
    public MBDMachineDefinition loadProductiveTag(@Nullable File file, CompoundTag projectTag, Deque<Runnable> postTask) {
        super.loadProductiveTag(file, projectTag, postTask);
        kineticMachineSettings.deserializeNBT(projectTag.getCompound("definition").getCompound("kineticMachineSettings"));
        return this;
    }

    @Override
    public Block createBlock() {
        var block = new MBDKineticMachineBlock(blockProperties.apply(stateMachine, BlockBehaviour.Properties.of()), this);
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
        if (kineticMachineSettings.useFlywheel) {
            var model = getRotationPartialModel();
            SimpleBlockEntityVisualizer.builder((BlockEntityType<MBDKineticMachineBlockEntity>) blockEntityType())
                    .factory((materialManager, be, pt) -> new MBDKineticInstance(materialManager, be, pt, model))
                    .skipVanillaRender(be -> false)
                    .apply();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private PartialModel getRotationPartialModel() {
        var model = AllPartialModels.SHAFT_HALF;
        if (stateMachine.getRootState() instanceof CreateMachineState state) {
            var rotationRenderer = state.getRotationRenderer();
            while (rotationRenderer instanceof UIResourceRenderer uiResourceRenderer) {
                rotationRenderer = uiResourceRenderer.getRenderer();
            }
            if (rotationRenderer instanceof IModelRenderer modelRenderer) {
                model = PartialModel.of(modelRenderer.getModelLocation());
            }
        }
        return model;
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {
        protected ConfigKineticMachineSettings kineticMachineSettings;

        protected Builder() {
        }

        public CreateKineticMachineDefinition build() {
            return new CreateKineticMachineDefinition(id, rootState, blockProperties, itemProperties, machineSettings, recipeLogicSettings, partSettings, kineticMachineSettings);
        }

    }
}

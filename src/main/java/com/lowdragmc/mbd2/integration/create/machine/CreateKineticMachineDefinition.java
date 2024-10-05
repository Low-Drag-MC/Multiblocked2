package com.lowdragmc.mbd2.integration.create.machine;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.core.PartialModel;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.foundation.utility.Couple;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Deque;

@Getter
@Accessors(fluent = true)
public class CreateKineticMachineDefinition extends MBDMachineDefinition {
    @Configurable(name = "config.definition.kinetic_machine_settings", subConfigurable = true, tips = "config.definition.kinetic_machine_settings.tooltip", collapse = false)
    protected final ConfigKineticMachineSettings kineticMachineSettings;

    protected CreateKineticMachineDefinition(ResourceLocation id, MachineState rootState,
                                             @Nullable ConfigBlockProperties blockProperties,
                                             @Nullable ConfigItemProperties itemProperties,
                                             @Nullable ConfigMachineSettingsFactory machineSettings,
                                             @Nullable ConfigPartSettingsFactory partSettings,
                                             @Nullable ConfigKineticMachineSettings kineticMachineSettings) {
        super(id, rootState, blockProperties, itemProperties, machineSettings, partSettings);
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
                () -> ConfigPartSettings.builder().build(),
                ConfigKineticMachineSettings.builder().build());
    }

    @Override
    public MBDMachineDefinition loadProductiveTag(File file, CompoundTag projectTag, Deque<Runnable> postTask) {
        super.loadProductiveTag(file, projectTag, postTask);
        kineticMachineSettings.deserializeNBT(projectTag.getCompound("definition").getCompound("kineticMachineSettings"));
        return this;
    }

    @Override
    public Block createBlock() {
        return new MBDKineticMachineBlock(blockProperties.apply(stateMachine, BlockBehaviour.Properties.of()), this);
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
            InstancedRenderRegistry.configure((BlockEntityType<MBDKineticMachineBlockEntity>) blockEntityType())
                    .factory((materialManager, be) -> new MBDKineticInstance(materialManager, be, model))
                    .skipRender((be) -> false)
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
                model = new PartialModel(modelRenderer.getModelLocation());
            }
        }
        return model;
    }

    public static void registerStressProvider() {
        BlockStressValues.registerProvider(MBD2.MOD_ID, new BlockStressValues.IStressValueProvider() {
            @Override
            public double getImpact(Block block) {
                if (block instanceof MBDKineticMachineBlock machineBlock) {
                    var definition = machineBlock.getDefinition();
                    if (!definition.kineticMachineSettings.isGenerator) {
                        return definition.kineticMachineSettings.getImpact();
                    }
                }
                return 0;
            }

            @Override
            public double getCapacity(Block block) {
                if (block instanceof MBDKineticMachineBlock machineBlock) {
                    var definition = machineBlock.getDefinition();
                    if (definition.kineticMachineSettings.isGenerator) {
                        return definition.kineticMachineSettings.getCapacity();
                    }
                }
                return 0;
            }

            @Override
            public boolean hasImpact(Block block) {
                if (block instanceof MBDKineticMachineBlock machineBlock) {
                    var definition = machineBlock.getDefinition();
                    return !definition.kineticMachineSettings.isGenerator;
                }
                return false;
            }

            @Override
            public boolean hasCapacity(Block block) {
                if (block instanceof MBDKineticMachineBlock machineBlock) {
                    var definition = machineBlock.getDefinition();
                    return definition.kineticMachineSettings.isGenerator;
                }
                return false;
            }

            @Nullable
            @Override
            public Couple<Integer> getGeneratedRPM(Block block) {
                if (block instanceof MBDKineticMachineBlock machineBlock) {
                    var definition = machineBlock.getDefinition();
                    return definition.kineticMachineSettings.isGenerator ? Couple.create(0, definition.kineticMachineSettings.maxRPM) : null;
                }
                return null;
            }
        });
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {
        protected ConfigKineticMachineSettings kineticMachineSettings;

        protected Builder() {
        }

        public CreateKineticMachineDefinition build() {
            return new CreateKineticMachineDefinition(id, rootState, blockProperties, itemProperties, machineSettings, partSettings, kineticMachineSettings);
        }

    }
}

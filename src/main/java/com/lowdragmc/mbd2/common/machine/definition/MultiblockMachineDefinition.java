package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.PredicateResource;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject.createBlockPattern;

/**
 * Multiblock machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMultiblockMachine#getDefinition()} behaviours.
 */
@Getter
@Accessors(fluent = true)
public class MultiblockMachineDefinition extends MBDMachineDefinition {
    public static final Map<Block, Set<MultiblockMachineDefinition>> CATALYST_CANDIDATES = Collections.synchronizedMap(new HashMap<>());

    @Configurable(name = "config.definition.multiblock_settings", subConfigurable = true, tips = "config.definition.multiblock_settings.tooltip", collapse = false)
    protected final ConfigMultiblockSettings multiblockSettings;

    // runtime
    @Setter
    private Function<MBDMultiblockMachine, BlockPattern> blockPatternFactory;
    @Setter
    private Function<MultiblockMachineDefinition, MultiblockShapeInfo[]> shapeInfoFactory;

    public MultiblockMachineDefinition(ResourceLocation id,
                                       StateMachine<?> stateMachine,
                                       ConfigBlockProperties blockProperties,
                                       ConfigItemProperties itemProperties,
                                       ConfigMachineSettings machineSettings,
                                       ConfigMultiblockSettings multiblockSettings) {
        super(id, stateMachine, blockProperties, itemProperties, machineSettings, null);
        this.multiblockSettings = multiblockSettings;
    }

    public static MultiblockMachineDefinition createDefault() {
        return new MultiblockMachineDefinition(
                MBD2.id("dummy"),
                StateMachine.createDefault(MachineState::builder),
                ConfigBlockProperties.builder().build(),
                ConfigItemProperties.builder().build(),
                ConfigMachineSettings.builder().build(),
                ConfigMultiblockSettings.builder().build());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ConfigMachineEvents createMachineEvents() {
        return super.createMachineEvents().registerEventGroup("MachineEvent.Multiblock");
    }

    @Override
    public MBDMultiblockMachine createMachine(IMachineBlockEntity blockEntity) {
        return new MBDMultiblockMachine(blockEntity, this);
    }

    @Override
    public MultiblockMachineDefinition loadProductiveTag(File file, CompoundTag projectTag, Deque<Runnable> postTask) {
        super.loadProductiveTag(file, projectTag, postTask);
        postTask.add(() -> {
            // load multiblock settings
            multiblockSettings.deserializeNBT(projectTag.getCompound("definition").getCompound("multiblockSettings"));
            // setup catalyst candidates
            if (multiblockSettings.catalyst().isEnable() && multiblockSettings.catalyst().getCandidates().isEnable()) {
                for (var block : multiblockSettings.catalyst().getCandidates().getValue()) {
                    CATALYST_CANDIDATES.computeIfAbsent(block, b -> new HashSet<>()).add(this);
                }
            }
            // setup block pattern
            var predicateResource = new PredicateResource();
            predicateResource.deserializeNBT(projectTag.getCompound("resources").getCompound(PredicateResource.RESOURCE_NAME));
            var placeholders = MultiblockMachineProject.deserializeBlockPlaceholders(projectTag.getCompound("placeholders"), predicateResource);
            var layerAxis = Direction.Axis.valueOf(projectTag.getString("layer_axis"));
            var aisleLength = switch (layerAxis) {
                case X -> placeholders.length;
                case Y -> placeholders[0].length;
                case Z -> placeholders[0][0].length;
            };
            var aisleRepetitions = new int[aisleLength][2];
            var repetitions = projectTag.getIntArray("aisle_repetitions");
            for (int i = 0; i < aisleLength; i++) {
                aisleRepetitions[i][0] = repetitions[i * 2];
                aisleRepetitions[i][1] = repetitions[i * 2 + 1];
            }
            var blockPattern = createBlockPattern(placeholders, layerAxis, aisleRepetitions, this);
            blockPatternFactory(controller -> blockPattern);
            // setup shape info
            var shapeInfos = new ArrayList<>(projectTag.getList("shape_infos", Tag.TAG_COMPOUND).stream()
                    .map(CompoundTag.class::cast)
                    .map(MultiblockShapeInfo::loadFromTag)
                    .toList());
            if (shapeInfos.isEmpty()) {
                // generate builtin shape info from pattern
                var repetition = Arrays.stream(aisleRepetitions).mapToInt(range -> range[0]).toArray();
                shapeInfos.add(new MultiblockShapeInfo(blockPattern.getPreview(repetition)));
                for (int layer = 0; layer < aisleRepetitions.length; layer++) {
                    var range = aisleRepetitions[layer];
                    for (int i = range[0] + 1; i <= range[1]; i++) {
                        repetition[layer] = i;
                        shapeInfos.add(new MultiblockShapeInfo(blockPattern.getPreview(repetition)));
                        repetition[layer] = range[0];
                    }
                }
            }
            var shapes = shapeInfos.toArray(new MultiblockShapeInfo[0]);
            shapeInfoFactory(definition -> shapes);
        });
        return this;
    }

    public BlockPattern getPattern(MBDMultiblockMachine controller) {
        return blockPatternFactory.apply(controller);
    }

    public void sortParts(List<IMultiPart> parts) {
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {
        protected ConfigMultiblockSettings multiblockSettings;

        protected Builder() {
        }

        public MultiblockMachineDefinition build() {
            return new MultiblockMachineDefinition(id, stateMachine, blockProperties, itemProperties, machineSettings, multiblockSettings);
        }
    }
}

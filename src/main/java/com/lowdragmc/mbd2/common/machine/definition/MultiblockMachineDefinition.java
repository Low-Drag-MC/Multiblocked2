package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.PredicateResource;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Multiblock machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMultiblockMachine#getDefinition()} behaviours.
 */
@Getter
@Accessors(fluent = true)
public class MultiblockMachineDefinition extends MBDMachineDefinition {
    @LDLRegister(name = "multiblock", registry = "mbd2:machine_definition_type")
    public static final MachineDefinitionType<MultiblockMachineDefinition> TYPE = new MachineDefinitionType<>("multiblock", "multiblock", ".mb") {
        @Override
        public MultiblockMachineDefinition createDefinition() {
            return MultiblockMachineDefinition.createDefault();
        }
    };

    public static final Map<Block, Set<MultiblockMachineDefinition>> CATALYST_CANDIDATES = Collections.synchronizedMap(new HashMap<>());
    @FunctionalInterface
    public interface ConfigMultiblockSettingsFactory extends Supplier<ConfigMultiblockSettings> { }

    @Configurable(name = "config.definition.multiblock_settings", subConfigurable = true, tips = "config.definition.multiblock_settings.tooltip", collapse = false)
    protected ConfigMultiblockSettings multiblockSettings;

    // runtime
    protected ConfigMultiblockSettingsFactory multiblockSettingsFactory;

    @Setter
    private Function<MBDMultiblockMachine, BlockPattern> blockPatternFactory;
    @Setter
    private Function<MultiblockMachineDefinition, MultiblockShapeInfo[]> shapeInfoFactory;

    public MultiblockMachineDefinition(ResourceLocation id,
                                       @Nullable MachineState rootState,
                                       @Nullable ConfigBlockProperties blockProperties,
                                       @Nullable ConfigItemProperties itemProperties,
                                       @Nullable ConfigMachineSettingsFactory machineSettingsFactory,
                                       @Nullable ConfigRecipeLogicSettings recipeLogicSettings,
                                       @Nullable ConfigMultiblockSettingsFactory multiblockSettingsFactory) {
        super(id, rootState, blockProperties, itemProperties, machineSettingsFactory, recipeLogicSettings, null);
        this.multiblockSettingsFactory = multiblockSettingsFactory == null ? () -> ConfigMultiblockSettings.builder().build() : multiblockSettingsFactory;
    }

    @Override
    public boolean allowPartSettings() {
        return false;
    }

    public static MultiblockMachineDefinition createDefault() {
        return new MultiblockMachineDefinition(
                MBD2.id("dummy"),
                StateMachine.createDefault(MachineState::baseBuilder),
                ConfigBlockProperties.builder().build(),
                ConfigItemProperties.builder().build(),
                () -> ConfigMachineSettings.builder().build(),
                ConfigRecipeLogicSettings.builder().build(),
                () -> ConfigMultiblockSettings.builder().build());
    }

    public static Builder builder() {
        return new Builder();
    }

//    @Override
//    public ConfigMachineEvents createMachineEvents() {
//        return super.createMachineEvents().registerEventGroup("MachineEvent.Multiblock");
//    }

    @Override
    public void loadFactory() {
        super.loadFactory();
        multiblockSettings = multiblockSettingsFactory.get();
    }

    @Override
    public MBDMultiblockMachine createMachine(IMachineBlockEntity blockEntity) {
        return new MBDMultiblockMachine(blockEntity, this);
    }

    @Override
    public MultiblockMachineDefinition loadProductiveTag(@Nullable File file, CompoundTag projectTag, Deque<Runnable> postTask) {
        super.loadProductiveTag(file, projectTag, postTask);
        var dataTag = projectTag.getCompound("data");
        var definitionTag = dataTag.getCompound("definition");
        postTask.add(() -> {
            multiblockSettings.deserializeNBT(Platform.getFrozenRegistry(), definitionTag.getCompound("multiblockSettings"));
            if (multiblockSettings.catalyst().isEnable() && multiblockSettings.catalyst().getCandidates().isEnable()) {
                for (var block : multiblockSettings.catalyst().getCandidates().getValue()) {
                    CATALYST_CANDIDATES.computeIfAbsent(block, b -> new HashSet<>()).add(this);
                }
            }
            var placeholders = MultiblockMachineProject.deserializeBlockPlaceholders(dataTag.getCompound("placeholders"));
            var layerAxis = dataTag.contains("layer_axis") ? Direction.Axis.valueOf(dataTag.getString("layer_axis")) : Direction.Axis.Y;
            var aisleLength = switch (layerAxis) {
                case X -> placeholders.length;
                case Y -> placeholders[0].length;
                case Z -> placeholders[0][0].length;
            };
            var aisleRepetitions = new int[aisleLength][2];
            for (int i = 0; i < aisleLength; i++) {
                aisleRepetitions[i][0] = 1;
                aisleRepetitions[i][1] = 1;
            }
            var repetitions = dataTag.getIntArray("aisle_repetitions");
            for (int i = 0; i < aisleLength && i * 2 + 1 < repetitions.length; i++) {
                aisleRepetitions[i][0] = repetitions[i * 2];
                aisleRepetitions[i][1] = repetitions[i * 2 + 1];
            }
            var blockPattern = createBlockPattern(placeholders, layerAxis, aisleRepetitions, this);
            blockPatternFactory(controller -> blockPattern);
            shapeInfoFactory(Util.memoize(definition -> {
                var shapeInfos = new ArrayList<>(dataTag.getList("shape_infos", Tag.TAG_COMPOUND).stream()
                        .map(CompoundTag.class::cast)
                        .map(tag -> MultiblockShapeInfo.loadFromTag(Platform.getFrozenRegistry(), tag))
                        .toList());
                if (shapeInfos.isEmpty()) {
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
                return shapeInfos.toArray(new MultiblockShapeInfo[0]);
            }));
        });
        return this;
    }

    public static BlockPattern createBlockPattern(BlockPlaceholder[][][] blockPlaceholders,
                                                  Direction.Axis layerAxis,
                                                  int[][] aisleRepetitions,
                                                  MultiblockMachineDefinition definition) {
        return createBlockPattern(blockPlaceholders, layerAxis, aisleRepetitions, definition, false);
    }

    /**
     * Create a block pattern from block placeholders.
     * @param blockPlaceholders the block placeholders
     * @param layerAxis the layer axis
     * @param aisleRepetitions the aisle repetitions
     * @param definition the machine definition
     * @param shapeInfo whether to create shape info with controller predicate
     * @return the block pattern
     */
    public static BlockPattern createBlockPattern(BlockPlaceholder[][][] blockPlaceholders,
                                                  Direction.Axis layerAxis,
                                                  int[][] aisleRepetitions,
                                                  MultiblockMachineDefinition definition,
                                                  boolean shapeInfo) {
        var aisleLength = switch (layerAxis) {
            case X -> blockPlaceholders.length;
            case Y -> blockPlaceholders[0].length;
            case Z -> blockPlaceholders[0][0].length;
        };
        var aisleHeight = switch (layerAxis) {
            case X -> blockPlaceholders[0].length;
            case Y -> blockPlaceholders.length;
            case Z -> blockPlaceholders.length;
        };
        var rowWidth = switch (layerAxis) {
            case X -> blockPlaceholders[0][0].length;
            case Y -> blockPlaceholders[0][0].length;
            case Z -> blockPlaceholders[0].length;
        };
        var predicate = new TraceabilityPredicate[aisleLength][aisleHeight][rowWidth];
        BlockPlaceholder controller = null;
        var x = 0;
        var min = 0;
        var max = 0;
        var centerOffset = new int[5];
        for (BlockPlaceholder[][] xSlice : blockPlaceholders) {
            var y = 0;
            for (BlockPlaceholder[] ySlice : xSlice) {
                var z = 0;
                for (BlockPlaceholder placeholder : ySlice) {
                    var traceabilityPredicate = placeholder.getPredicates().stream()
                            .map(path -> PredicateResource.getINSTANCE().getResourceInstance().getResource(path))
                            .filter(Objects::nonNull)
                            .map(TraceabilityPredicate::new)
                            .reduce(TraceabilityPredicate::or)
                            .orElse(new TraceabilityPredicate());
                    if (placeholder.isController())  {
                        controller = placeholder;
                        if (Direction.Axis.X == layerAxis) {
                            centerOffset = new int[]{z, y, x, min, max};
                        } else if (Direction.Axis.Y == layerAxis) {
                            centerOffset = new int[]{z, x, y, min, max};
                        } else {
                            centerOffset = new int[]{y, x, z, min, max};
                        }
                        if (shapeInfo) {
                            traceabilityPredicate = new TraceabilityPredicate(new PatternPredicate(state ->
                                    state.getBlockState().getBlock() == MBDRegistries.FAKE_MACHINE().block(), () -> new BlockInfo[]{new ControllerBlockInfo()}));
                        } else {
                            if (definition.multiblockSettings().catalyst().isEnable() && definition.multiblockSettings().catalyst().getCandidates().isEnable()) {
                                for (var block : definition.multiblockSettings().catalyst().getCandidates().getValue()) {
                                    traceabilityPredicate = new TraceabilityPredicate(new PredicateBlocks(block)).or(traceabilityPredicate);
                                }
                            }
                            traceabilityPredicate = new TraceabilityPredicate(new PredicateBlocks(definition.block())).or(traceabilityPredicate);
                        }
                    }
                    if (Direction.Axis.X == layerAxis) {
                        predicate[x][y][z] = traceabilityPredicate;
                    } else if (Direction.Axis.Y == layerAxis) {
                        predicate[y][x][z] = traceabilityPredicate;
                    } else {
                        predicate[z][x][y] = traceabilityPredicate;
                    }
                    if (layerAxis == Direction.Axis.Z) {
                        min += aisleRepetitions[z][0];
                        max += aisleRepetitions[z][1];
                    }
                    z++;
                }
                if (layerAxis == Direction.Axis.Y) {
                    min += aisleRepetitions[y][0];
                    max += aisleRepetitions[y][1];
                } else if (layerAxis == Direction.Axis.Z) {
                    min = 0;
                    max = 0;
                }
                y++;
            }
            if (layerAxis == Direction.Axis.X) {
                min += aisleRepetitions[x][0];
                max += aisleRepetitions[x][1];
            } else if (layerAxis == Direction.Axis.Y){
                min = 0;
                max = 0;
            }
            x++;
        }
        var controllerFace = controller.getFacing().getAxis() == Direction.Axis.Y ? Direction.NORTH : controller.getFacing();
        var structureDir = new RelativeDirection[3];
        structureDir[0] = RelativeDirection.getSliceYDirection(layerAxis, controllerFace);
        structureDir[1] = RelativeDirection.getSliceXDirection(layerAxis, controllerFace);
        structureDir[2] = RelativeDirection.getAisleDirection(layerAxis, controllerFace);
        return new BlockPattern(predicate, structureDir, aisleRepetitions, centerOffset);
    }

    protected void bindMachineUI(MBDMachine machine, UIElement ui) {
//        // proxy part ui
//        if (machine instanceof MBDMultiblockMachine multiblock) {
//            var prefix = "part:";
//            var midTag = "@ui:";
//            for (Widget widget : ui.getWidgetsById("part:.*?@ui:")) {
//                var id = widget.getId();
//                if (id.startsWith(prefix)) {
//                    int atIndex = id.indexOf(midTag);
//                    if (atIndex != -1) {
//                        var traitName = id.substring(prefix.length(), atIndex);
//                        var uiName = "ui:" + id.substring(atIndex + midTag.length());
//                        for (IMultiPart part : multiblock.getParts()) {
//                            if (part instanceof MBDMachine mbdMachine) {
//                                var trait = mbdMachine.getTraitByName(traitName);
//                                if (trait != null && trait.getDefinition() instanceof IUIProviderTrait provider && uiName.startsWith(provider.uiPrefixName())) {
//                                    widget.setId(uiName);
//                                    provider.initTraitUI(trait, ui);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    public BlockPattern getPattern(MBDMultiblockMachine controller) {
        return blockPatternFactory.apply(controller);
    }

    public void sortParts(List<IMultiPart> parts) {
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {
        protected ConfigMultiblockSettingsFactory multiblockSettings;

        protected Builder() {
        }

        public MultiblockMachineDefinition build() {
            return new MultiblockMachineDefinition(id, rootState, blockProperties, itemProperties, machineSettings, recipeLogicSettings, multiblockSettings);
        }
    }
}

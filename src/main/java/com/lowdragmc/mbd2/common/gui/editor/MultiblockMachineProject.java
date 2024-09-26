package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.BlockPlaceholder;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockAreaPanel;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockPatternPanel;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockShapeInfoPanel;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.*;

@Getter
@LDLRegister(name = "mb", group = "editor.machine")
@NoArgsConstructor
public class MultiblockMachineProject extends MachineProject {
    protected BlockPlaceholder[][][] blockPlaceholders;
    protected Direction.Axis layerAxis = Direction.Axis.Y;
    protected int[][] aisleRepetitions;
    protected PredicateResource predicateResource;
    protected List<MultiblockShapeInfo> multiblockShapeInfos = new ArrayList<>();

    public MultiblockMachineProject(Resources resources, MultiblockMachineDefinition definition, WidgetGroup ui) {
        this.resources = resources;
        this.definition = definition;
        this.ui = ui;
        this.blockPlaceholders = new BlockPlaceholder[1][1][1];
        if (resources.resources.get(PredicateResource.RESOURCE_NAME) instanceof PredicateResource resource) {
            this.predicateResource = resource;
            this.blockPlaceholders[0][0][0] = BlockPlaceholder.controller(predicateResource);
            setBlockPlaceholders(blockPlaceholders);
        }
    }

    @Override
    protected Map<String, Resource<?>> createResources() {
        var resources = super.createResources();
        // predicate
        var predicate = new PredicateResource();
        resources.put(PredicateResource.RESOURCE_NAME, predicate);
        return resources;
    }

    @Override
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    protected MultiblockMachineDefinition createDefinition() {
        // use vanilla furnace model as an example
        var builder = MultiblockMachineDefinition.builder();
        builder.id(MBD2.id("new_machine"))
                .stateMachine(StateMachine.createMultiblockDefault(MachineState::builder, FURNACE_RENDERER))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build());
        builder.multiblockSettings(ConfigMultiblockSettings.builder().build());
        return builder.build();
    }

    public void setLayerAxis(Direction.Axis layerAxis) {
        this.layerAxis = layerAxis;
        var aisleLength = switch (layerAxis) {
            case X -> blockPlaceholders.length;
            case Y -> blockPlaceholders[0].length;
            case Z -> blockPlaceholders[0][0].length;
        };
        aisleRepetitions = new int[aisleLength][2];
        for (int[] aisleRepetition : aisleRepetitions) {
            aisleRepetition[0] = 1;
            aisleRepetition[1] = 1;
        }
    }

    public void setBlockPlaceholders(BlockPlaceholder[][][] blockPlaceholders) {
        this.blockPlaceholders = blockPlaceholders;
        setLayerAxis(this.layerAxis);
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
            case Y -> blockPlaceholders[0][0].length;
            case Z -> blockPlaceholders.length;
        };
        var rowWidth = switch (layerAxis) {
            case X -> blockPlaceholders[0].length;
            case Y -> blockPlaceholders.length;
            case Z -> blockPlaceholders[0][0].length;
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
                            .map(placeholder.getPredicateResource()::getResource)
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
                            traceabilityPredicate = new TraceabilityPredicate(new SimplePredicate(state ->
                                    state.getBlockState().getBlock() == MBDRegistries.getFAKE_MACHINE().block(), () -> new BlockInfo[]{new ControllerBlockInfo()}));
                        } else {
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

    @Override
    public MultiblockMachineProject newEmptyProject() {
        return new MultiblockMachineProject(new Resources(createResources()), createDefinition(), createDefaultUI());
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "multiblock");
    }

    public CompoundTag serializeNBT() {
        var tag = super.serializeNBT();
        tag.put("placeholders", serializeBlockPlaceholders(blockPlaceholders));
        tag.putString("layer_axis", layerAxis.name());
        tag.putIntArray("aisle_repetitions", Arrays.stream(aisleRepetitions).flatMapToInt(Arrays::stream).toArray());
        var shapeInfoList = new ListTag();
        for (var shapeInfo : getMultiblockShapeInfos()) {
            shapeInfoList.add(shapeInfo.serializeNBT());
        }
        tag.put("shape_infos", shapeInfoList);
        return tag;
    }

    public static CompoundTag serializeBlockPlaceholders(BlockPlaceholder[][][] blockPlaceholders){
        var placeholders = new ArrayList<BlockPlaceholder>();
        var placeHolderMap = new HashMap<BlockPlaceholder, Integer>();
        var placeHolderIndex = new ArrayList<Integer>();
        for (BlockPlaceholder[][] blockPlaceholder : blockPlaceholders) {
            for (BlockPlaceholder[] value : blockPlaceholder) {
                for (BlockPlaceholder holder : value) {
                    if (holder != null) {
                        if (!placeHolderMap.containsKey(holder)) {
                            placeHolderMap.put(holder, placeholders.size());
                            placeholders.add(holder);
                        }
                        placeHolderIndex.add(placeHolderMap.get(holder));
                    } else {
                        placeHolderIndex.add(-1);
                    }
                }
            }
        }
        var placeHoldersTag = new CompoundTag();
        var placeHoldersListTag = new ListTag();
        for (BlockPlaceholder placeholder : placeholders) {
            placeHoldersListTag.add(placeholder.serializeNBT());
        }
        placeHoldersTag.put("holders", placeHoldersListTag);
        placeHoldersTag.putInt("x", blockPlaceholders.length);
        placeHoldersTag.putInt("y", blockPlaceholders[0].length);
        placeHoldersTag.putInt("z", blockPlaceholders[0][0].length);
        placeHoldersTag.putIntArray("pattern", placeHolderIndex.stream().mapToInt(i -> i).toArray());
        return placeHoldersTag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        if (resources.resources.get(PredicateResource.RESOURCE_NAME) instanceof PredicateResource resource) {
            this.predicateResource = resource;
        }
        var placeHoldersTag = tag.getCompound("placeholders");
        var x = placeHoldersTag.getInt("x");
        var y = placeHoldersTag.getInt("y");
        var z = placeHoldersTag.getInt("z");
        this.blockPlaceholders = deserializeBlockPlaceholders(placeHoldersTag, predicateResource);
        this.layerAxis = Direction.Axis.valueOf(tag.getString("layer_axis"));
        var aisleLength = switch (layerAxis) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
        this.aisleRepetitions = new int[aisleLength][2];
        var repetitions = tag.getIntArray("aisle_repetitions");
        for (int i = 0; i < aisleLength; i++) {
            this.aisleRepetitions[i][0] = repetitions[i * 2];
            this.aisleRepetitions[i][1] = repetitions[i * 2 + 1];
        }
        this.multiblockShapeInfos.clear();
        var shapeInfoList = tag.getList("shape_infos", Tag.TAG_COMPOUND);
        this.multiblockShapeInfos.addAll(shapeInfoList.stream().map(CompoundTag.class::cast).map(MultiblockShapeInfo::loadFromTag).toList());
    }

    public static BlockPlaceholder[][][] deserializeBlockPlaceholders(CompoundTag placeHoldersTag, PredicateResource predicateResource) {
        var placeHoldersListTag = placeHoldersTag.getList("holders", Tag.TAG_COMPOUND);
        var x = placeHoldersTag.getInt("x");
        var y = placeHoldersTag.getInt("y");
        var z = placeHoldersTag.getInt("z");
        var pattern = placeHoldersTag.getIntArray("pattern");
        var blockPlaceholders = new BlockPlaceholder[x][y][z];
        for (int i = 0; i < pattern.length; i++) {
            var index = pattern[i];
            var holder = index == -1 ? BlockPlaceholder.create(predicateResource, "any") : BlockPlaceholder.fromTag(predicateResource, placeHoldersListTag.getCompound(index));
            blockPlaceholders[i / (y * z)][(i / z) % y][i % z] = holder;
        }
        return blockPlaceholders;
    }

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            var multiblockPatternPanel = createMultiblockPatternPanel(machineEditor);
            var multiblockAreaPanel = createMultiblockAreaPanel(multiblockPatternPanel);
            var MultiblockShapeInfoPanel = createMultiblockShapeInfoPanel(machineEditor);
            tabContainer.addTab("editor.machine.multiblock_area", multiblockAreaPanel, multiblockAreaPanel::onPanelSelected, multiblockAreaPanel:: onPanelDeselected);
            tabContainer.addTab("editor.machine.multiblock_pattern", multiblockPatternPanel, multiblockPatternPanel::onPanelSelected, multiblockPatternPanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.multiblock.multiblock_shape_info", MultiblockShapeInfoPanel, MultiblockShapeInfoPanel::onPanelSelected, MultiblockShapeInfoPanel::onPanelDeselected);
        }
    }

    public MultiblockPatternPanel createMultiblockPatternPanel(MachineEditor editor) {
        return new MultiblockPatternPanel(editor, this);
    }

    public MultiblockAreaPanel createMultiblockAreaPanel(MultiblockPatternPanel multiblockPatternPanel) {
        return new MultiblockAreaPanel(this, multiblockPatternPanel);
    }

    public MultiblockShapeInfoPanel createMultiblockShapeInfoPanel(MachineEditor editor) {
        return new MultiblockShapeInfoPanel(editor, this);
    }

}

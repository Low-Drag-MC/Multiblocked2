package com.lowdragmc.mbd2.common.gui.editor.multiblock.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockShapeInfoPanel;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

@Getter
public class ShapeInfoList extends DraggableScrollableWidgetGroup {
    private final MultiblockShapeInfoPanel panel;
    @Nullable
    private MultiblockShapeInfo selectedShapeInfo;

    public ShapeInfoList(MultiblockShapeInfoPanel panel, Size size) {
        super(0, 0, size.width, size.height);
        this.panel = panel;
        setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        reloadShapeInfos();
    }

    public void reloadShapeInfos() {
        clearAllWidgets();
        var level = panel.getLevel();
        level.clear();
        var offset = new BlockPos(0, 0, 0);
        var shapes = new ArrayList<>(panel.getProject().getMultiblockShapeInfos());
        var isBuiltin = shapes.isEmpty();
        if(isBuiltin) {
            var blockPattern = MultiblockMachineProject.createBlockPattern(
                    panel.getProject().getBlockPlaceholders(),
                    panel.getProject().getLayerAxis(),
                    panel.getProject().getAisleRepetitions(),
                    panel.getProject().getDefinition(),
                    true);
            var repetition = Arrays.stream(panel.getProject().getAisleRepetitions()).mapToInt(range -> range[0]).toArray();
            shapes.add(new MultiblockShapeInfo(blockPattern.getPreview(repetition)));
            for (int layer = 0; layer < panel.getProject().getAisleRepetitions().length; layer++) {
                var range = panel.getProject().getAisleRepetitions()[layer];
                for (int i = range[0] + 1; i <= range[1]; i++) {
                    repetition[layer] = i;
                    shapes.add(new MultiblockShapeInfo(blockPattern.getPreview(repetition)));
                    repetition[layer] = range[0];
                }
            }
        }
        for (int i = 0; i < shapes.size(); i++) {
            var shapeInfo = shapes.get(i);
            int yOffset = 3 + widgets.size() * 82;
            var selectableWidgetGroup = new SelectableWidgetGroup(0, yOffset, getSizeWidth() - 2, 95);
            // scene
            var scene = new SceneEditorWidget((selectableWidgetGroup.getSizeWidth() - 80) / 2, 0, 80, 80, null);
            scene.setRenderFacing(false);
            scene.setRenderSelect(false);
            scene.setIntractable(false);
            scene.createScene(level);
            scene.getRenderer().setOnLookingAt(null); // for better performance
            scene.useCacheBuffer();
            var renderPositions = new ArrayList<BlockPos>();
            var blocks = shapeInfo.getBlocks();
            for (int x = 0; x < blocks.length; x++) {
                for (int y = 0; y < blocks[x].length; y++) {
                    for (int z = 0; z < blocks[x][y].length; z++) {
                        var pos = offset.offset(x, y, z);
                        renderPositions.add(pos);
                        var blockInfo = blocks[x][y][z];
                        if (blockInfo instanceof ControllerBlockInfo controllerBlockInfo) {
                            blockInfo = new BlockInfo(MBDRegistries.getFAKE_MACHINE().block().defaultBlockState()
                                    .setValue(MBDRegistries.getFAKE_MACHINE().blockProperties().rotationState().property.orElseThrow(), controllerBlockInfo.getFacing()), blockEntity -> {
                                if (blockEntity instanceof MachineBlockEntity machineBlockEntity) {
                                    var controllerMachine = panel.getProject().getDefinition().createMachine(machineBlockEntity);
                                    machineBlockEntity.setMachine(controllerMachine);
                                    controllerMachine.loadAdditionalTraits();
                                }
                            });
                        }
                        level.addBlock(pos, blockInfo);
                    }
                }
            }
            scene.setRenderedCore(renderPositions);

            selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
            selectableWidgetGroup.setOnSelected(group -> {
                panel.loadShapeInfo(isBuiltin ? null : shapeInfo, renderPositions);
                selectedShapeInfo = shapeInfo;
            });
            selectableWidgetGroup.addWidget(scene);
            // label
            selectableWidgetGroup.addWidget(new ImageWidget(0, 80, selectableWidgetGroup.getSizeWidth(), 15,
                    new TextTexture(isBuiltin ? "auto-built": "page: " + i)
                            .setColor(isBuiltin ? ColorPattern.GRAY.color : ColorPattern.WHITE.color)
                            .setType(TextTexture.TextType.ROLL)
                            .setWidth(selectableWidgetGroup.getSizeWidth())));
            offset = offset.offset(1000, 0, 1000);
            addWidget(selectableWidgetGroup);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && selectedShapeInfo != null && isMouseOverElement(mouseX, mouseY) && !panel.getProject().getMultiblockShapeInfos().isEmpty()) {
            var menu = TreeBuilder.Menu.start().leaf(Icons.REMOVE, "editor.machine.multiblock.multiblock_shape_info.remove", () -> {
                panel.getProject().getMultiblockShapeInfos().remove(selectedShapeInfo);
                panel.clearShapeInfo();
                reloadShapeInfos();
            });
            panel.getEditor().openMenu(mouseX, mouseY, menu);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}

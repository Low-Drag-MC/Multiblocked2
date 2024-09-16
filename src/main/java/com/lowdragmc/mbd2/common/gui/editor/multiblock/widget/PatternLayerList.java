package com.lowdragmc.mbd2.common.gui.editor.multiblock.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.BlockPlaceholder;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockPatternPanel;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3i;

import java.util.List;
import java.util.Objects;

@Getter
public class PatternLayerList extends WidgetGroup {
    private final MultiblockPatternPanel panel;
    private final DraggableScrollableWidgetGroup layerContainer;

    public PatternLayerList(MultiblockPatternPanel panel, Size size) {
        super(0, 0, size.width, size.height);
        this.panel = panel;
        // init layer container
        layerContainer = new DraggableScrollableWidgetGroup(0, 14, size.width, size.height - 14);
        layerContainer.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        addWidget(layerContainer);
        // init axis buttons
        addWidget(new ImageWidget(2, 1, size.width - 2 - 40, 14,
                new TextTexture("editor.machine.multiblock.multiblock_pattern.layer_direction")
                        .setWidth(size.width - 2 - 40)
                        .setType(TextTexture.TextType.LEFT)));
        addWidget(new ImageWidget(size.width - 40, 1, 12, 12, () -> panel.getProject().getLayerAxis() == Direction.Axis.X ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40, 1, 12, 12, new TextTexture("x"), cd -> {
            panel.getProject().setLayerAxis(Direction.Axis.X);
            reloadLayers();
        }));
        addWidget(new ImageWidget(size.width - 40 + 13, 1, 12, 12, () -> panel.getProject().getLayerAxis() == Direction.Axis.Y ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40 + 13, 1, 12, 12, new TextTexture("y"), cd -> {
            panel.getProject().setLayerAxis(Direction.Axis.Y);
            reloadLayers();
        }));
        addWidget(new ImageWidget(size.width - 40 + 26, 1, 12, 12, () -> panel.getProject().getLayerAxis() == Direction.Axis.Z ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40 + 26, 1, 12, 12, new TextTexture("z"), cd -> {
            panel.getProject().setLayerAxis(Direction.Axis.Z);
            reloadLayers();
        }));
        reloadLayers();
    }

    public void reloadLayers() {
        layerContainer.clearAllWidgets();
        var placeholders = panel.getProject().getBlockPlaceholders();
        var axis = panel.getProject().getLayerAxis();
        var layerCount = switch (axis) {
            case X -> placeholders.length;
            case Y -> placeholders[0].length;
            case Z -> placeholders[0][0].length;
        };
        var container = new WidgetGroup(0, 0, layerContainer.getSizeWidth(), 16 * layerCount);
        container.setLayout(Layout.VERTICAL_LEFT);
        container.setLayoutPadding(2);
        for (int i = 0; i < layerCount; i++) {
            container.addWidget(createLayerGroup(axis, i));
        }
        container.setDynamicSized(true);
        layerContainer.addWidget(container);
    }

    private WidgetGroup createLayerGroup(Direction.Axis axis, int index) {
        var placeholders = panel.getProject().getBlockPlaceholders();
        var slice = getSliceByAxis(placeholders, axis, index);
        var totalAmount = slice.length * slice[0].length;
        var group = new WidgetGroup(2, 0, layerContainer.getSizeWidth() - 6, 28);
        var children = new WidgetGroup(2, 28, layerContainer.getSizeWidth() - 6, 16 * totalAmount);
        var hasController = false;
        // prepare block info in this layer
        var blockIndex = 0;
        var text = switch (axis) {
            case X -> "Y: %d, Z: %d";
            case Y -> "X: %d, Z: %d";
            case Z -> "X: %d, Y: %d";
        };
        for (int i = 0; i < slice.length; i++) {
            for (int j = 0; j < slice[i].length; j++) {
                var placeholder = slice[i][j];
                hasController |= placeholder.isController();
                var pos = switch (axis) {
                    case X -> new Vector3i(index, i, j);
                    case Y -> new Vector3i(i, index, j);
                    case Z -> new Vector3i(i, j, index);
                };
                var blockInfo = new WidgetGroup(10, 16 * blockIndex, children.getSizeWidth() - 10, 14);
                blockInfo.addWidget(new ImageWidget(2, 0, 14, 14,
                        () -> new ItemStackTexture(placeholder.getPredicates().stream()
                                .map(panel.getProject().getPredicateResource()::getResource)
                                .filter(Objects::nonNull)
                                .map(SimplePredicate::getCandidates)
                                .flatMap(List::stream)
                                .filter(itemStack -> !itemStack.isEmpty())
                                .toArray(ItemStack[]::new))));
                blockInfo.addWidget(new ImageWidget(18, 1, blockInfo.getSizeWidth() - 18, 14,
                        new TextTexture(text.formatted(i, j)).setWidth(blockInfo.getSizeWidth() - 18).setType(TextTexture.TextType.ROLL)));
                children.addWidget(blockInfo);
                children.addWidget(new ImageWidget(0, 16 * blockIndex, children.getSizeWidth(), 14,
                        () -> panel.isBlockSelected(pos) ? ColorPattern.T_GREEN.rectTexture() : IGuiTexture.EMPTY));
                children.addWidget(new ButtonWidget(0, 16 * blockIndex, children.getSizeWidth(), 14, (cd) -> {
                    if (cd.isCtrlClick || cd.isShiftClick) {
                        if (panel.isBlockSelected(pos)) {
                            panel.removeSelectedBlock(pos);
                        } else {
                            panel.addSelectedBlock(pos);
                        }
                    } else {
                        panel.addSelectedBlock(pos, true);
                    }
                }));
                blockIndex++;
            }
        }
        children.setVisible(false);
        // prepare layer button
        var layerButton = new SwitchWidget(1, 1, 14, 14, (cd, pressed) -> {
            children.setVisible(pressed);
            group.setSizeHeight(pressed ? 28 + 16 * totalAmount : 28);
        }).setTexture(Icons.RIGHT.copy().scale(0.8f), Icons.DOWN.copy().scale(0.8f)).setSupplier(children::isVisible);
        var layerLabel = new ImageWidget(16, 2, group.getSizeWidth() - 4 - 32, 14,
                new TextTexture((Direction.Axis.X == axis ? "X: " : Direction.Axis.Y == axis ? "Y: " : "Z: ") + index)
                        .setWidth(group.getSizeWidth() - 4 - 32)
                        .setType(TextTexture.TextType.ROLL));
        var visibleButton = new SwitchWidget(group.getSizeWidth() - 4 - 16, 1, 14, 14,
                (cd, pressed) -> panel.setVisibleLayer(pressed ? index : -1))
                .setTexture(Icons.EYE.copy().scale(0.8f), Icons.EYE.copy().scale(0.8f).setColor(ColorPattern.T_GREEN.color))
                .setSupplier(() -> panel.getVisibleLayer() == index);
        // prepare repetition button
        if (hasController) {
            group.addWidget(new ImageWidget(2, 14, group.getSizeWidth() - 4, 10,
                    new TextTexture("editor.machine.multiblock.multiblock_pattern.repetition_controller").setWidth(group.getSizeWidth() - 4).setType(TextTexture.TextType.ROLL)));
        } else {
            var repetition = panel.getProject().getAisleRepetitions()[index];
            group.addWidget(new ImageWidget(2, 14, 20, 10, new TextTexture("min")));
            group.addWidget(new ImageWidget(24, 14, 40, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
            group.addWidget(new TextFieldWidget(24 + 3, 14, 40 - 3, 10, () -> repetition[0] + "",
                    s -> {
                        repetition[0] = Integer.parseInt(s);
                        if (repetition[0] > repetition[1]) {
                            repetition[1] = repetition[0];
                        }})
                    .setCurrentString(repetition[0] + "")
                    .setBordered(false)
                    .setNumbersOnly(1, 100)
                    .setWheelDur(1));
            group.addWidget(new ImageWidget(66, 14, 20, 10, new TextTexture("max")));
            group.addWidget(new ImageWidget(88, 14, 40, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
            group.addWidget(new TextFieldWidget(88 + 3, 14, 40 - 3, 10, () -> repetition[1] + "",
                    s -> {
                        repetition[1] = Integer.parseInt(s);
                        if (repetition[0] > repetition[1]) {
                            repetition[0] = repetition[1];
                        }})
                    .setCurrentString(repetition[1] + "")
                    .setBordered(false)
                    .setNumbersOnly(1, 100)
                    .setWheelDur(1));
            group.addWidget(new ImageWidget(130, 13, 11, 11, Icons.HELP)
                    .setHoverTooltips("editor.machine.multiblock.multiblock_pattern.repetition"));
        }

        group.setBackground(ColorPattern.T_GRAY.borderTexture(-2));
        group.addWidget(layerButton);
        group.addWidget(layerLabel);
        group.addWidget(visibleButton);
        group.addWidget(children);
        return group;
    }

    private BlockPlaceholder[][] getSliceByAxis(BlockPlaceholder[][][] placeholders, Direction.Axis axis, int index) {
        return switch (axis) {
            case X -> placeholders[index];
            case Y -> {
                var blocks = new BlockPlaceholder[placeholders.length][];
                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = placeholders[i][index];
                }
                yield blocks;
            }
            case Z -> {
                var blocks = new BlockPlaceholder[placeholders.length][];
                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = new BlockPlaceholder[placeholders[i].length];
                    for (int j = 0; j < blocks[i].length; j++) {
                        blocks[i][j] = placeholders[i][j][index];
                    }
                }
                yield blocks;
            }
        };
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && button == 1) {
            panel.openMenu(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}

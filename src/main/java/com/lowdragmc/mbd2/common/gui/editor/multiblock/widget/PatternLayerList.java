package com.lowdragmc.mbd2.common.gui.editor.multiblock.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

@Getter
public class PatternLayerList extends WidgetGroup {
    private final MachineEditor editor;
    private final DraggableScrollableWidgetGroup layerContainer;
    @Setter
    private Direction.Axis axis = Direction.Axis.Y;

    public PatternLayerList(MachineEditor editor, Size size) {
        super(0, 0, size.width, size.height);
        this.editor = editor;
        // init layer container
        layerContainer = new DraggableScrollableWidgetGroup(0, 14, size.width, size.height - 14);
        layerContainer.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        addWidget(layerContainer);
        // init axis buttons
        addWidget(new ImageWidget(2, 0, size.width - 2 - 40, 14,
                new TextTexture("editor.machine.multiblock.multiblock_pattern.layer_direction")
                        .setWidth(size.width - 2 - 40)
                        .setType(TextTexture.TextType.LEFT)));
        addWidget(new ImageWidget(size.width - 40, 1, 12, 12, () -> axis == Direction.Axis.X ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40, 1, 12, 12, new TextTexture("x"), cd -> setAxis(Direction.Axis.X)));
        addWidget(new ImageWidget(size.width - 40 + 13, 1, 12, 12, () -> axis == Direction.Axis.Y ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40 + 13, 1, 12, 12, new TextTexture("y"), cd -> setAxis(Direction.Axis.Y)));
        addWidget(new ImageWidget(size.width - 40 + 26, 1, 12, 12, () -> axis == Direction.Axis.Z ?
                ColorPattern.T_GREEN.rectTexture().setRadius(2) : ColorPattern.T_GRAY.rectTexture().setRadius(2)));
        addWidget(new ButtonWidget(size.width - 40 + 26, 1, 12, 12, new TextTexture("z"), cd -> setAxis(Direction.Axis.Z)));
        if (editor.getCurrentProject() instanceof MultiblockMachineProject project) {

        }
    }

    public void reloadLayers() {

    }
}

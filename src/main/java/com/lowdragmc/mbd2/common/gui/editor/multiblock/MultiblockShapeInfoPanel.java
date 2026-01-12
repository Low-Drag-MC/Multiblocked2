package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.widget.ShapeInfoList;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

public class MultiblockShapeInfoPanel extends WidgetGroup {
    @Getter
    protected final MachineEditor editor;
    @Getter
    protected final MultiblockMachineProject project;
    @Getter
    protected final TrackedDummyWorld level;
    @Getter
    protected final SceneEditorWidget scene;
    protected final WidgetGroup buttonGroup;

    public MultiblockShapeInfoPanel(MachineEditor editor, MultiblockMachineProject project) {
        super(0, MenuPanel.HEIGHT + 16, Editor.INSTANCE.getSize().getWidth() - ConfigPanel.WIDTH, Editor.INSTANCE.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
        this.project = project;
        addWidget(scene = new SceneEditorWidget(0, 0, this.getSize().width, this.getSize().height, null));
        addWidget(buttonGroup = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height));
        scene.disableTransformGizmo();
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level = new TrackedDummyWorld());
        scene.useCacheBuffer();
        prepareButtonGroup();
        buttonGroup.setSize(new Size(Math.max(0, buttonGroup.widgets.size() * 25 - 5), 20));
        buttonGroup.setSelfPosition(new Position(this.getSize().width - buttonGroup.getSize().width - 25, 25));
    }

    public void clearShapeInfo() {
        scene.setRenderedCore(Collections.emptyList());
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
    }

    public void loadShapeInfo(@Nullable IConfigurable configurable, Collection<BlockPos> blocks) {
        scene.setRenderedCore(blocks);
        if (configurable != null) {
            editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, configurable);
        } else {
            editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        }
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators();
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().setTitle("editor.machine.multiblock.multiblock_shape_info");
        editor.getToolPanel().addNewToolBox("editor.machine.multiblock.multiblock_shape_info", Icons.WIDGET_CUSTOM, size -> new ShapeInfoList(this, size));
        if (editor.getToolPanel().inAnimate()) {
            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
        } else {
            editor.getToolPanel().show();
        }
        clearShapeInfo();
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getToolPanel().setTitle("ldlib.gui.editor.group.tool_box");
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators();
    }

    /**
     * prepare the button group, you can add buttons / switches here.
     */
    protected void prepareButtonGroup() {
    }
}

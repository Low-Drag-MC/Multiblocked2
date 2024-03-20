package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import lombok.Getter;

public class RecipeTypePanel extends WidgetGroup {
    @Getter
    protected final MachineEditor editor;

    public RecipeTypePanel(MachineEditor editor) {
        super(0, MenuPanel.HEIGHT, editor.getSize().getWidth() - ConfigPanel.WIDTH, editor.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().setTitle("editor.machine.recipe_type.recipes");
        editor.getToolPanel().addNewToolBox("editor.machine.recipe_type.recipes.common", Icons.WIDGET_CUSTOM, size -> new RecipeList(editor, size));
        if (editor.getToolPanel().inAnimate()) {
            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
        } else {
            editor.getToolPanel().show();
        }
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getToolPanel().setTitle("ldlib.gui.editor.group.tool_box");
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
    }
}

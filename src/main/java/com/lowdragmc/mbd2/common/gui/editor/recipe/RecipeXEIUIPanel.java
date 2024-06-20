package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.editor.ui.MainPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.tool.WidgetToolBox;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;
import com.lowdragmc.mbd2.common.gui.editor.recipe.widget.RecipeTypeUIFloatView;
import lombok.Getter;

public class RecipeXEIUIPanel extends MainPanel {
    @Getter
    private final RecipeTypeUIFloatView floatView = new RecipeTypeUIFloatView();

    public RecipeXEIUIPanel(MachineEditor editor) {
        super(editor, editor.getCurrentProject() instanceof RecipeTypeProject recipeTypeProject ? recipeTypeProject.getUi() : new WidgetGroup());
    }

    public MachineEditor getEditor() {
        return (MachineEditor) editor;
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators();
        editor.getToolPanel().clearAllWidgets();
        for (WidgetToolBox.Default tab : WidgetToolBox.Default.TABS) {
            editor.getToolPanel().addNewToolBox("ldlib.gui.editor.group." + tab.groupName, tab.icon, tab::createToolBox);
        }
        if (editor.getToolPanel().inAnimate()) {
            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
        } else {
            editor.getToolPanel().show();
        }
        editor.getFloatView().addWidgetAnima(floatView,  new Transform().duration(200).scale(0.2f));
        floatView.reloadList();
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators();
        editor.getFloatView().removeWidget(floatView);
    }
}

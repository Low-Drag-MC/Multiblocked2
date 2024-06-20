package com.lowdragmc.mbd2.common.gui.editor.machine;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.editor.ui.MainPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.tool.WidgetToolBox;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.widget.TraitUIFloatView;
import lombok.Getter;

public class MachineUIPanel extends MainPanel {

    @Getter
    private final TraitUIFloatView floatView = new TraitUIFloatView();

    public MachineUIPanel(MachineEditor editor) {
        super(editor, editor.getCurrentProject() instanceof MachineProject machineProject ? machineProject.getUi() : new WidgetGroup());
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
        floatView.reloadTrait();
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

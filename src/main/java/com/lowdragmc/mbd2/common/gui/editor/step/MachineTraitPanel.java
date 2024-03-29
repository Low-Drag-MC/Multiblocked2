package com.lowdragmc.mbd2.common.gui.editor.step;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.widget.TraitList;

public class MachineTraitPanel extends MachineScenePanel {

    public MachineTraitPanel(MachineEditor editor) {
        super(editor);
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().setTitle("editor.machine.machine_traits");
        editor.getToolPanel().addNewToolBox("editor.machine.machine_traits.list", Icons.WIDGET_CUSTOM, size -> new TraitList(editor, size));
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

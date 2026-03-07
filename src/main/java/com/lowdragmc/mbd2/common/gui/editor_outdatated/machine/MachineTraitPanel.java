//package com.lowdragmc.mbd2.common.gui.editor_outdatated.machine;
//
//import com.lowdragmc.lowdraglib2.gui.editor.Icons;
//import com.lowdragmc.lowdraglib2.gui.widget.SceneWidget;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.MBDEditor;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.machine.widget.TraitList;
//
//import javax.annotation.Nullable;
//
//public class MachineTraitPanel extends MachineScenePanel {
//    @Nullable
//    private TraitList traitList;
//
//    public MachineTraitPanel(MBDEditor editor) {
//        super(editor);
//    }
//
//    /**
//     * Called when the panel is selected/switched to.
//     */
//    public void onPanelSelected() {
//        editor.getConfigPanel().clearAllConfigurators();
//        editor.getToolPanel().clearAllWidgets();
//        editor.getToolPanel().setTitle("editor.machine.machine_traits");
//        editor.getToolPanel().addNewToolBox("editor.machine.machine_traits.list", Icons.WIDGET_CUSTOM, size -> traitList = new TraitList(editor, size));
//        if (editor.getToolPanel().inAnimate()) {
//            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
//        } else {
//            editor.getToolPanel().show();
//        }
//    }
//
//    /**
//     * Called when the panel is deselected/switched from.
//     */
//    public void onPanelDeselected() {
//        editor.getToolPanel().setTitle("ldlib2.gui.editor.group.tool_box");
//        editor.getToolPanel().hide();
//        editor.getToolPanel().clearAllWidgets();
//        editor.getConfigPanel().clearAllConfigurators();
//    }
//
//    @Override
//    public void renderAfterWorld(SceneWidget sceneWidget) {
//        super.renderAfterWorld(sceneWidget);
//        if (traitList != null && traitList.getSelected() != null) {
//            var definition = traitList.getSelected();
//            definition.renderAfterWorldInTraitPanel(this);
//        }
//    }
//}

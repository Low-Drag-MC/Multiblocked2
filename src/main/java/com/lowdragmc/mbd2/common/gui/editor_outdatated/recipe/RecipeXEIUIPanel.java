//package com.lowdragmc.mbd2.common.gui.editor_outdatated.recipe;
//
//import com.lowdragmc.lowdraglib2.gui.animation.Transform;
//import com.lowdragmc.lowdraglib2.gui.editor.ui.MainPanel;
//import com.lowdragmc.lowdraglib2.gui.editor.ui.tool.WidgetToolBox;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.MBDEditor;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.recipe.widget.RecipeTypeUIFloatView;
//import lombok.Getter;
//
//public class RecipeXEIUIPanel extends MainPanel {
//    @Getter
//    private final RecipeTypeUIFloatView floatView;
//
//    public RecipeXEIUIPanel(MBDEditor editor, WidgetGroup root, boolean isFuel) {
//        super(editor, root);
//        floatView = new RecipeTypeUIFloatView(isFuel);
//    }
//
//    public MBDEditor getEditor() {
//        return (MBDEditor) editor;
//    }
//
//    /**
//     * Called when the panel is selected/switched to.
//     */
//    public void onPanelSelected() {
//        editor.getConfigPanel().clearAllConfigurators();
//        editor.getToolPanel().clearAllWidgets();
//        for (WidgetToolBox.Default tab : WidgetToolBox.Default.TABS) {
//            editor.getToolPanel().addNewToolBox("ldlib2.gui.editor.group." + tab.groupName, tab.icon, tab::createToolBox);
//        }
//        if (editor.getToolPanel().inAnimate()) {
//            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
//        } else {
//            editor.getToolPanel().show();
//        }
//        editor.getFloatView().addWidgetAnima(floatView,  new Transform().duration(200).scale(0.2f));
//        floatView.reloadList();
//    }
//
//    /**
//     * Called when the panel is deselected/switched from.
//     */
//    public void onPanelDeselected() {
//        editor.getToolPanel().hide();
//        editor.getToolPanel().clearAllWidgets();
//        editor.getConfigPanel().clearAllConfigurators();
//        editor.getFloatView().removeWidget(floatView);
//    }
//}

package com.lowdragmc.mbd2.common.gui.editor.machine;

import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.graphprocessor.MachineEventGraphView;
import lombok.Getter;

import javax.annotation.Nullable;

@Getter
public class MachineEventsPanel extends WidgetGroup {
    @Getter
    private final MachineEditor editor;
    @Nullable
    private BaseGraph currentGraph;

    public MachineEventsPanel(MachineEditor editor) {
        super(0, MenuPanel.HEIGHT + 16, editor.getSize().getWidth() - ConfigPanel.WIDTH, editor.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
    }

    public void openEventGraphEditor(BaseGraph graph) {
        clearAllWidgets();
        currentGraph = graph;
        addWidget(new MachineEventGraphView(graph, 0, 0, getSizeWidth(), getSizeHeight()));
        editor.getResourcePanel().hide();
    }

    public void closeEventGraphEditor() {
        clearAllWidgets();
        currentGraph = null;
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        if (editor.getCurrentProject() instanceof MachineProject project) {
            editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, project.getDefinition().machineEvents());
        }
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getConfigPanel().clearAllConfigurators();
    }
}

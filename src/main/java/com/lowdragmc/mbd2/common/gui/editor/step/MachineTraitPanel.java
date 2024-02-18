package com.lowdragmc.mbd2.common.gui.editor.step;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.widget.TraitList;
import lombok.Getter;
import lombok.NonNull;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachineTraitPanel extends MachineScenePanel {
    @Getter
    @NonNull
    protected final TraitList traitList;

    public MachineTraitPanel(MachineEditor editor) {
        super(editor);
        this.traitList = new TraitList(editor);
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().addNewToolBox("editor.machine.machine_traits.list", Icons.WIDGET_CUSTOM, traitList);
        editor.getToolPanel().show();
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
    }
}

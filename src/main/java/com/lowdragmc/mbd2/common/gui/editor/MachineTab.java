package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.ui.menu.MenuTab;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;

@LDLRegister(name = "machine_tab", group = "editor.machine")
public class MachineTab extends MenuTab {

    protected TreeBuilder.Menu createMenu() {
        return TreeBuilder.Menu.start();
    }

    @Override
    public String getTranslateKey() {
        return "%s.%s".formatted(group(), name());
    }
}

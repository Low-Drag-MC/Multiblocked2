package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.ui.*;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@LDLRegister(name = "editor.machine", group = "editor")
@OnlyIn(Dist.CLIENT)
public class MachineEditor extends Editor {
    public static final ConfigPanel.Tab BASIC = ConfigPanel.Tab.WIDGET;
    public static final ConfigPanel.Tab MACHINE_STATE = ConfigPanel.Tab.createTab(Icons.FILE, Component.translatable("editor.config_panel.machine_state"));
    public static final ConfigPanel.Tab RESOURCE = ConfigPanel.Tab.RESOURCE;

    public MachineEditor() {
        super(MBD2.getLocation());
    }

    @Override
    public MachineProject getCurrentProject() {
        return (MachineProject)super.getCurrentProject();
    }

    public void initEditorViews() {
        this.toolPanel = new ToolPanel(this);
        this.configPanel = new ConfigPanel(this, List.of(BASIC, MACHINE_STATE, RESOURCE));
        this.tabPages = new StringTabContainer(this);
        this.resourcePanel = new ResourcePanel(this);
        this.menuPanel = new MenuPanel(this);
        this.floatView = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height);

        this.addWidget(this.tabPages);
        this.addWidget(this.toolPanel);
        this.addWidget(this.configPanel);
        this.addWidget(this.resourcePanel);
        this.addWidget(this.menuPanel);
        this.addWidget(this.floatView);
    }

    @Override
    public void loadProject(IProject project) {
        if (project == null || project instanceof MachineProject) {
            super.loadProject(project);
        } else {
            throw new IllegalArgumentException("Invalid project type");
        }
    }
}

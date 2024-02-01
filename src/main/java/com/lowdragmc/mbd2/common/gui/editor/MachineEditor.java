package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.ui.*;
import com.lowdragmc.lowdraglib.gui.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;

@LDLRegister(name = "editor.machine", group = "editor")
@OnlyIn(Dist.CLIENT)
public class MachineEditor extends Editor {

    public MachineEditor(File workSpace) {
        super(workSpace);
    }

    public void initEditorViews() {
        this.addWidget(this.tabPages = new TabContainer(0, 0, this.getSize().width, this.getSize().height));
        this.addWidget(this.toolPanel = new ToolPanel(this));
        this.addWidget(this.configPanel = new ConfigPanel(this));
        this.addWidget(this.resourcePanel = new ResourcePanel(this));
        this.addWidget(this.menuPanel = new MenuPanel(this));
        this.addWidget(this.floatView = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height));

        addWidget(particleScene = new ParticleScene(this));
        addWidget(toolPanel = new ToolPanel(this));
        addWidget(configPanel = new ConfigPanel(this));
        addWidget(resourcePanel = new ResourcePanel(this));
        addWidget(menuPanel = new MenuPanel(this));
        addWidget(floatView = new WidgetGroup(0, 0, getSize().width, getSize().height));
        if (menuPanel.getTabs().get("view") instanceof ViewMenu viewMenu) {
            viewMenu.openView(new ParticleInfoView());
        }
        this.effect = new EditorEffect(this);
        particleScene.resetScene();
    }
}

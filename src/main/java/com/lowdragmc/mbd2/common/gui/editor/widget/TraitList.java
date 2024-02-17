package com.lowdragmc.mbd2.common.gui.editor.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.ToolPanel;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.ldlib.MBDLDLibPlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class TraitList extends DraggableScrollableWidgetGroup {
    private final MachineEditor editor;
    @Nullable
    private TraitDefinition selected;

    public TraitList(MachineEditor editor) {
        super(0, 0, ToolPanel.WIDTH, 100);
        this.editor = editor;
        setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        editor.getCurrentProject().getDefinition().machineSettings().traitDefinitions().forEach(this::addDefinition);
    }

    public void addDefinition(TraitDefinition definition) {
        int yOffset = 3 + widgets.size() * 20;
        var selectableWidgetGroup = new SelectableWidgetGroup(0, yOffset, ToolPanel.WIDTH - 2, 18);
        selectableWidgetGroup.addWidget(new ImageWidget(1, 0, 18, 18, definition.getIcon()));
        selectableWidgetGroup.addWidget(new ImageWidget(20, 0, ToolPanel.WIDTH - 20, 18,
                new TextTexture().setSupplier(definition::getName).setType(TextTexture.TextType.HIDE)));
        selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
        selectableWidgetGroup.setOnSelected(group -> {
            editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, definition);
            selected = definition;
        });
        addWidget(selectableWidgetGroup);
    }

    public void removeDefinition(TraitDefinition definition) {
        int index = editor.getCurrentProject().getDefinition().machineSettings().traitDefinitions().indexOf(definition);
        if (index >= 0) {
            editor.getCurrentProject().getDefinition().machineSettings().removeTraitDefinition(definition);
            widgets.remove(index);
            for (int i = 0; i < widgets.size(); i++) {
                if (i >= index) {
                    widgets.get(i).addSelfPosition(0, - 15);
                }
            }
        }
        if (this.selected == definition) {
            this.selected = null;
            editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && button == 1) {
            var menu = TreeBuilder.Menu.start()
                    .branch(Icons.ADD_FILE, "editor.machine.machine_traits.add_trait", m -> {
                        for (var wrapper : MBDLDLibPlugin.REGISTER_TRAIT_DEFINITIONS.values()) {
                            m.leaf(wrapper.annotation().name(), () -> {
                                var traitDefinition = wrapper.creator().get();
                                var name = traitDefinition.getName();
                                var index = 0;
                                while (editor.getCurrentProject().getDefinition().machineSettings().traitDefinitions().stream().anyMatch(e -> e.getName().equals(traitDefinition.getName()))) {
                                    traitDefinition.setName(name + "(%d)".formatted(index));
                                    index++;
                                }
                                editor.getCurrentProject().getDefinition().machineSettings().addTraitDefinition(traitDefinition);
                                addDefinition(traitDefinition);
                            });
                        }
                    });
            if (selected != null) {
                menu.crossLine();
                menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> {
                    var name = selected.getName();
                    var tag = selected.serializeNBT();
                    var definition = TraitDefinition.deserializeDefinition(tag);
                    if (definition != null) {
                        definition.setName(name + " copied");
                        editor.getCurrentProject().getDefinition().machineSettings().addTraitDefinition(definition);
                        addDefinition(definition);
                    }
                });
                menu.leaf(Icons.REMOVE_FILE, "editor.machine.machine_traits.remove_trait", () -> removeDefinition(selected));
            }
            editor.openMenu(mouseX, mouseY, menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}

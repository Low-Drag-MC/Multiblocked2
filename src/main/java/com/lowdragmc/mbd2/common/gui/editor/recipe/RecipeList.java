package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;

public class RecipeList extends DraggableScrollableWidgetGroup {
    private final MachineEditor editor;

    public RecipeList(MachineEditor editor, Size size) {
        super(0, 0, size.width, size.height);
        this.editor = editor;
        setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        if (editor.getCurrentProject() instanceof RecipeTypeProject project) {
            project.getRecipeTypeDefinition().getRecipes().forEach(this::addRecipe);
        }
    }

    public void addRecipe(MBDRecipe recipe) {
        int yOffset = 3 + widgets.size() * 20;
        var selectableWidgetGroup = new SelectableWidgetGroup(0, yOffset, getSizeWidth() - 2, 18);
        selectableWidgetGroup.addWidget(new ImageWidget(1, 0, 18, 18, definition.getIcon()));
        selectableWidgetGroup.addWidget(new ImageWidget(20, 0, getSizeWidth() - 20, 18,
                new TextTexture().setSupplier(definition::getName).setType(TextTexture.TextType.HIDE)));
        selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
        selectableWidgetGroup.setOnSelected(group -> {
            editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, definition);
            selected = definition;
        });
        addWidget(selectableWidgetGroup);
        // check if it is ITraitUIProvider
        var traitUIFloatView = getTraitUIFloatView();
        if (traitUIFloatView != null) {
            traitUIFloatView.reloadTrait();
        }
    }

    public void removeRecipe(MBDRecipe recipe) {
        if (!(editor.getCurrentProject() instanceof MachineProject project)) return;
        int index = project.getDefinition().machineSettings().traitDefinitions().indexOf(definition);
        if (index >= 0) {
            project.getDefinition().machineSettings().removeTraitDefinition(definition);
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
        // check if it is ITraitUIProvider
        var traitUIFloatView = getTraitUIFloatView();
        if (traitUIFloatView != null) {
            traitUIFloatView.reloadTrait();
        }
    }
}

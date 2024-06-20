package com.lowdragmc.mbd2.common.gui.editor.recipe.widget;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.WidgetTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;

public class RecipeXEIPreviewFloatView extends FloatViewWidget {

    public RecipeXEIPreviewFloatView() {
        super(200, 200, 200, 120, false);
    }

    @Override
    public String name() {
        return "recipe_xei_preview";
    }

    @Override
    public String group() {
        return "editor.machine";
    }

    public IGuiTexture getIcon() {
        return new ProgressTexture();
    }

    @Override
    public IGuiTexture getHoverIcon() {
        return Icons.REMOVE;
    }

    public MachineEditor getEditor() {
        return (MachineEditor) editor;
    }

    private MBDRecipe recipe;
    private JsonElement lastData;

    public void clearRecipe() {
        content.clearAllWidgets();
    }

    public void loadRecipe(MBDRecipe recipe) {
        clearRecipe();
        if (recipe == null) return;
        if (editor.getCurrentProject() instanceof RecipeTypeProject project) {
            this.recipe = recipe;;
            lastData = MBDRecipeSerializer.SERIALIZER.toJson(recipe);
            var tag = IConfigurableWidget.serializeNBT(project.getUi(), project.getResources(), true);
            var ui = new WidgetGroup();
            ui.setClientSideWidget();
            IConfigurableWidget.deserializeNBT(ui, tag, project.getResources(), true);
            project.getRecipeType().bindXEIRecipeUI(ui, recipe);
            content.addWidget(new ImageWidget(0, 0, content.getSizeWidth(), content.getSizeHeight(), new WidgetTexture(ui).setMouse(0, 0)));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (recipe != null) {
            var data = MBDRecipeSerializer.SERIALIZER.toJson(recipe);
            if (!data.equals(lastData)) {
                loadRecipe(recipe);
            }
        }
    }
}

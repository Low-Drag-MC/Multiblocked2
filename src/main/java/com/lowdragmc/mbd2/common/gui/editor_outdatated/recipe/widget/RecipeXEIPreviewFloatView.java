//package com.lowdragmc.mbd2.common.gui.editor_outdatated.recipe.widget;
//
//import com.lowdragmc.lowdraglib2.gui.editor.ColorPattern;
//import com.lowdragmc.lowdraglib2.gui.editor.Icons;
//import com.lowdragmc.lowdraglib2.gui.editor.configurator.IConfigurableWidget;
//import com.lowdragmc.lowdraglib2.gui.editor.ui.view.FloatViewWidget;
//import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ProgressTexture;
//import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
//import com.lowdragmc.lowdraglib2.utils.Size;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.MBDEditor;
//import com.lowdragmc.mbd2.common.gui.editor_outdatated.RecipeTypeProject;
//import net.minecraft.nbt.CompoundTag;
//
//public class RecipeXEIPreviewFloatView extends FloatViewWidget {
//
//    public RecipeXEIPreviewFloatView() {
//        super(200, 200, 200, 120, false);
//    }
//
//    @Override
//    public String name() {
//        return "recipe_xei_preview";
//    }
//
//    @Override
//    public String group() {
//        return "editor.machine";
//    }
//
//    public IGuiTexture getIcon() {
//        return new ProgressTexture();
//    }
//
//    @Override
//    public IGuiTexture getHoverIcon() {
//        return Icons.REMOVE;
//    }
//
//    public MBDEditor getEditor() {
//        return (MBDEditor) editor;
//    }
//
//    private boolean isFuel;
//    private MBDRecipe recipe;
//    private CompoundTag lastData;
//
//    public void clearRecipe() {
//        content.clearAllWidgets();
//    }
//
//    public void loadRecipe(boolean isFuel, MBDRecipe recipe) {
//        clearRecipe();
//        this.isFuel = isFuel;
//        if (recipe == null) return;
//        if (editor.getCurrentProject() instanceof RecipeTypeProject project) {
//            this.recipe = recipe;;
//            lastData = MBDRecipeSerializer.SERIALIZER.toNBT(recipe);
//            var tag = IConfigurableWidget.serializeNBT(isFuel ? project.getFuelUI() : project.getUi(), project.getResources(), true);
//            var ui = new WidgetGroup();
//            ui.setClientSideWidget();
//            IConfigurableWidget.deserializeNBT(ui, tag, project.getResources(), true);
//            project.getRecipeType().bindXEIRecipeUI(ui, recipe);
//            ui.setSelfPosition(0, 0);
//            resetSeize(ui.getSizeWidth(), ui.getSizeHeight());
//            content.addWidget(ui);
//        }
//    }
//
//    public void resetSeize(int width, int height) {
//        setSize(width, height + 15);
//        clearAllWidgets();
//        initWidget();
//        if (isCollapse) {
//            title.setSize(new Size(15, 15));
//            title.setBackground(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(5f), ColorPattern.GRAY.borderTexture(-1).setRadius(5f)));
//            content.setVisible(false);
//            content.setActive(false);
//        } else {
//            title.setSize(new Size(getSize().width, 15));
//            title.setBackground(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setTopRadius(5f), ColorPattern.GRAY.borderTexture(-1).setTopRadius(5f)));
//            content.setVisible(true);
//            content.setActive(true);
//        }
//    }
//
//    @Override
//    public void updateScreen() {
//        super.updateScreen();
//        if (recipe != null) {
//            var data = MBDRecipeSerializer.SERIALIZER.toNBT(recipe);
//            if (!data.equals(lastData)) {
//                loadRecipe(isFuel, recipe);
//            }
//        }
//    }
//}

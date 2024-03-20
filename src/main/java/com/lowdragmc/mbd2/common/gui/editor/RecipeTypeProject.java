package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.ColorsResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.EntriesResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.TexturesResource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeTypePanel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@LDLRegister(name = "rproj", group = "editor.machine")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RecipeTypeProject implements IProject {
    protected Resources resources;
    protected MBDRecipeType recipeType;
    protected WidgetGroup ui;

    public RecipeTypeProject(Resources resources, MBDRecipeType recipeType, WidgetGroup ui) {
        this.resources = resources;
        this.recipeType = recipeType;
        this.ui = ui;
    }

    protected Map<String, Resource<?>> createResources() {
        Map<String, Resource<?>> resources = new LinkedHashMap<>();
        // entries
        var entries = new EntriesResource();
        entries.buildDefault();
        resources.put(EntriesResource.RESOURCE_NAME, entries);
        // texture
        var texture = new TexturesResource();
        resources.put(TexturesResource.RESOURCE_NAME, texture);
        // color
        var color = new ColorsResource();
        color.buildDefault();
        resources.put(ColorsResource.RESOURCE_NAME, color);
        return resources;
    }


    protected WidgetGroup createDefaultUI() {
        var group = new WidgetGroup(150, 50, 176, 180);
        group.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        return group;
    }

    @Override
    public RecipeTypeProject newEmptyProject() {
        return new RecipeTypeProject(new Resources(createResources()), new MBDRecipeType(MBD2.id("recipe_type")), createDefaultUI());
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        tag.put("ui", IConfigurableWidget.serializeNBT(this.ui, resources, true));
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.ui = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.ui, tag.getCompound("ui"), resources, true);
    }

    @Override
    public void saveProject(File file) {
        try {
            NbtIo.write(serializeNBT(), file);
        } catch (IOException ignored) { }
    }

    @Nullable
    @Override
    public IProject loadProject(File file) {
        try {
            var tag = NbtIo.read(file);
            if (tag != null) {
                var proj = new RecipeTypeProject();
                proj.deserializeNBT(tag);
                return proj;
            }
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            IProject.super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            var recipeTypePanel = new RecipeTypePanel(machineEditor);
            tabContainer.addTab("editor.machine.recipe_type", recipeTypePanel, recipeTypePanel::onPanelSelected, recipeTypePanel::onPanelDeselected);
        }
    }
}

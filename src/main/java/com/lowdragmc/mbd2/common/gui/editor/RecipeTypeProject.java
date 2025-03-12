package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.Platform;
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
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeTypePanel;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeXEIUIPanel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@LDLRegister(name = "rt", group = "editor.machine")
@NoArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RecipeTypeProject implements IProject {
    protected Resources resources;
    protected MBDRecipeType recipeType;
    protected WidgetGroup ui;
    protected WidgetGroup fuelUI;

    public RecipeTypeProject(Resources resources, MBDRecipeType recipeType, WidgetGroup ui, WidgetGroup fuelUI) {
        this.resources = resources;
        this.recipeType = recipeType;
        this.ui = ui;
        this.fuelUI = fuelUI;
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
        var group = new WidgetGroup(200, 50, 176, 100);
        group.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        return group;
    }

    protected MBDRecipeType createDefaultRecipeType() {
        return new MBDRecipeType(MBD2.id("recipe_type"));
    }

    @Override
    public RecipeTypeProject newEmptyProject() {
        return new RecipeTypeProject(new Resources(createResources()), createDefaultRecipeType(), createDefaultUI(), createDefaultUI());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT(provider));
        tag.put("ui", IConfigurableWidget.serializeNBT(this.ui, resources, true, provider));
        tag.put("fuelUI", IConfigurableWidget.serializeNBT(this.fuelUI, resources, true, provider));
        tag.put("recipe_type", recipeType.serializeNBT(provider));
        return tag;
    }

    @Override
    public Resources loadResources(CompoundTag tag) {
        var resources = new Resources(createResources());
        resources.deserializeNBT(tag, Platform.getFrozenRegistry());
        return resources;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.ui = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.ui, tag.getCompound("ui"), resources, true, provider);
        if (tag.contains("fuelUI")) {
            this.fuelUI = new WidgetGroup();
            IConfigurableWidget.deserializeNBT(this.fuelUI, tag.getCompound("fuelUI"), resources, true, provider);
            if (this.fuelUI.getBackgroundTexture() == null) {
                this.fuelUI.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
            }
        } else {
            this.fuelUI = createDefaultUI();
        }
        this.recipeType = createDefaultRecipeType();
        UIResourceTexture.setCurrentResource((Resource)resources.resources.get(TexturesResource.RESOURCE_NAME), true);
        this.recipeType.deserializeNBT(provider, tag.getCompound("recipe_type"));
        UIResourceTexture.clearCurrentResource();
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "recipe_type");
    }

    @Override
    public void saveProject(Path file) {
        try {
            NbtIo.write(serializeNBT(Platform.getFrozenRegistry()), file);
        } catch (IOException ignored) { }
    }

    @Nullable
    @Override
    public IProject loadProject(Path file) {
        try {
            var tag = NbtIo.read(file);
            if (tag != null) {
                var proj = new RecipeTypeProject();
                proj.deserializeNBT(Platform.getFrozenRegistry(), tag);
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
            var recipeTypePanel = new RecipeTypePanel(recipeType, machineEditor);
            var recipeXEIUIPanel = new RecipeXEIUIPanel(machineEditor, getUi(), false);
            var fuelRecipeXEIUIPanel = new RecipeXEIUIPanel(machineEditor, getFuelUI(), true);
            tabContainer.addTab("editor.machine.recipe_type", recipeTypePanel, recipeTypePanel::onPanelSelected, recipeTypePanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.recipe_xei_ui", recipeXEIUIPanel, recipeXEIUIPanel::onPanelSelected, recipeXEIUIPanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.recipe_xei_fuel_ui", fuelRecipeXEIUIPanel, fuelRecipeXEIUIPanel::onPanelSelected, fuelRecipeXEIUIPanel::onPanelDeselected);
        }
    }
}

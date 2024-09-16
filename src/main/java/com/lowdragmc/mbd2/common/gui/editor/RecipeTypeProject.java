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
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeTypePanel;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeXEIUIPanel;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Getter
@LDLRegister(name = "rt", group = "editor.machine")
@NoArgsConstructor
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
        var group = new WidgetGroup(200, 50, 176, 100);
        group.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        return group;
    }

    protected MBDRecipeType createDefaultRecipeType() {
        return new MBDRecipeType(MBD2.id("recipe_type"));
    }

    /**
     * Create definition from project tag for product usage.
     * @param tag project tag.
     * @param postTask Called when the mod is loaded completed. To make sure all resources are available.
     *                 <br/> e.g. items, blocks and other registries are ready.
     */
    public static MBDRecipeType createProductFromProject(CompoundTag tag, ConcurrentLinkedDeque<Runnable> postTask) {
        var registryName = tag.getCompound("recipe_type").getString("registryName");
        if (!registryName.isEmpty() && ResourceLocation.isValidResourceLocation(registryName)) {
            var recipeType = new MBDRecipeType(new ResourceLocation(registryName));
            postTask.add(postLoading(recipeType, tag));
            return recipeType;
        } else {
            return new MBDRecipeType(MBD2.id("recipe_type"));
        }
    }

    /**
     * Post loading task for machine definition.
     */
    public static Runnable postLoading(MBDRecipeType recipeType, CompoundTag tag) {
        return () -> {
            var texturesResource = new TexturesResource();
            texturesResource.deserializeNBT(tag.getCompound("resources").getCompound(TexturesResource.RESOURCE_NAME));
            UIResourceTexture.setCurrentResource(texturesResource, false);
            recipeType.deserializeNBT(tag.getCompound("recipe_type"));
            UIResourceTexture.clearCurrentResource();
            var ui = new WidgetGroup();
            var uiTag = tag.getCompound("ui");
            IConfigurableWidget.deserializeNBT(ui, uiTag, texturesResource, true);
            recipeType.setUiSize(ui.getSize());
            recipeType.setUiCreator(recipe -> {
                var recipeUI = new WidgetGroup();
                recipeUI.setClientSideWidget();
                IConfigurableWidget.deserializeNBT(recipeUI, uiTag, texturesResource, false);
                recipeType.bindXEIRecipeUI(recipeUI, recipe);
                recipeUI.setSelfPosition(0, 0);
                recipeUI.setBackground(IGuiTexture.EMPTY);
                return recipeUI;
            });
        };
    }

    @Override
    public RecipeTypeProject newEmptyProject() {
        return new RecipeTypeProject(new Resources(createResources()), createDefaultRecipeType(), createDefaultUI());
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        tag.put("ui", IConfigurableWidget.serializeNBT(this.ui, resources, true));
        tag.put("recipe_type", recipeType.serializeNBT());
        return tag;
    }

    @Override
    public Resources loadResources(CompoundTag tag) {
        var resources = new Resources(createResources());
        resources.deserializeNBT(tag);
        return resources;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.ui = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.ui, tag.getCompound("ui"), resources, true);
        this.recipeType = createDefaultRecipeType();
        this.recipeType.deserializeNBT(tag.getCompound("recipe_type"));
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "recipe_type");
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
            var recipeTypePanel = new RecipeTypePanel(recipeType, machineEditor);
            var recipeXEIUIPanel = new RecipeXEIUIPanel(machineEditor);
            tabContainer.addTab("editor.machine.recipe_type", recipeTypePanel, recipeTypePanel::onPanelSelected, recipeTypePanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.recipe_xei_ui", recipeXEIUIPanel, recipeXEIUIPanel::onPanelSelected, recipeXEIUIPanel::onPanelDeselected);
        }
    }
}

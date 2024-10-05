package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.*;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigPanel;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineEventsPanel;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineUIPanel;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@LDLRegister(name = "sm", group = "editor.machine")
@NoArgsConstructor
public class MachineProject implements IProject {
    public static final IRenderer FURNACE_RENDERER = new IModelRenderer(new ResourceLocation("block/furnace"));

    protected Resources resources;
    protected MBDMachineDefinition definition;
    protected WidgetGroup ui;

    public MachineProject(Resources resources, MBDMachineDefinition definition, WidgetGroup ui) {
        this.resources = resources;
        this.definition = definition;
        this.ui = ui;
        if (this.definition != null) {
            this.definition.loadFactory();
        }
    }

    protected Map<String, Resource<?>> createResources() {
        Map<String, Resource<?>> resources = new LinkedHashMap<>();
        // entries
        var entries = new EntriesResource();
        entries.buildDefault();
        resources.put(EntriesResource.RESOURCE_NAME, entries);
        // renderer
        var renderer = new IRendererResource();
        resources.put(IRendererResource.RESOURCE_NAME, renderer);
        // texture
        var texture = new TexturesResource();
        resources.put(TexturesResource.RESOURCE_NAME, texture);
        // color
        var color = new ColorsResource();
        color.buildDefault();
        resources.put(ColorsResource.RESOURCE_NAME, color);
        return resources;
    }

    protected MBDMachineDefinition createDefinition() {
        // use vanilla furnace model as an example
        return MBDMachineDefinition.builder()
                .id(MBD2.id("new_machine"))
                .rootState(StateMachine.createSingleDefault(MachineState::builder, FURNACE_RENDERER))
                .build();
    }

    protected WidgetGroup createDefaultUI() {
        var group = new WidgetGroup(150, 50, 176, 180);
        group.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        var inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(new Position((group.getSize().width - inventory.getSize().width) / 2,
                group.getSize().height - 2 - inventory.getSize().height));
        group.addWidget(inventory);
        return group;
    }

    @Override
    public MachineProject newEmptyProject() {
        return new MachineProject(new Resources(createResources()), createDefinition(), createDefaultUI());
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "machine");
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        UIResourceRenderer.setCurrentResource((Resource<IRenderer>) resources.resources.get(IRendererResource.RESOURCE_NAME), true);
        tag.put("definition", definition.serializeNBT());
        UIResourceTexture.clearCurrentResource();
        tag.put("ui", IConfigurableWidget.serializeNBT(this.ui, resources, true));
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
        if (this.definition == null) {
            this.definition = createDefinition();
            this.definition.loadFactory();
        }
        UIResourceRenderer.setCurrentResource((Resource<IRenderer>) resources.resources.get(IRendererResource.RESOURCE_NAME), true);
        this.definition.deserializeNBT(tag.getCompound("definition"));
        UIResourceTexture.clearCurrentResource();
        this.ui = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.ui, tag.getCompound("ui"), resources, true);
    }

    @Override
    public void saveProject(File file) {
        try {
            NbtIo.write(serializeNBT(), file);
        } catch (IOException ignored) { }
    }

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            IProject.super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            var machineConfigPanel = createMachineConfigPanel(machineEditor);
            var machineTraitPanel = createMachineTraitPanel(machineEditor);
            var machineEventsPanel = createMachineEventsPanel(machineEditor);
            var machineUIPanel = createMachineUIPanel(machineEditor);
            tabContainer.addTab("editor.machine.basic_settings", machineConfigPanel, machineConfigPanel::onPanelSelected);
            tabContainer.addTab("editor.machine.machine_traits", machineTraitPanel, machineTraitPanel::onPanelSelected, machineTraitPanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.machine_events", machineEventsPanel, machineEventsPanel::onPanelSelected, machineEventsPanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.machine_ui", machineUIPanel, machineUIPanel::onPanelSelected, machineUIPanel::onPanelDeselected);
        }
    }

    protected MachineConfigPanel createMachineConfigPanel(MachineEditor editor) {
        return new MachineConfigPanel(editor);
    }

    protected MachineTraitPanel createMachineTraitPanel(MachineEditor editor) {
        return new MachineTraitPanel(editor);
    }

    protected MachineEventsPanel createMachineEventsPanel(MachineEditor editor) {
        return new MachineEventsPanel(editor);
    }

    protected MachineUIPanel createMachineUIPanel(MachineEditor editor) {
        return new MachineUIPanel(editor);
    }

    @Override
    public void onClosed(Editor editor) {
        editor.getFloatView().clearAllWidgets();
    }
}

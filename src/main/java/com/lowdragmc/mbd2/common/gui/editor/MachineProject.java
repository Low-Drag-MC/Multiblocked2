package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.*;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.step.MachineConfigPanel;
import com.lowdragmc.mbd2.common.gui.editor.step.MachineTraitPanel;
import com.lowdragmc.mbd2.common.gui.editor.step.MachineUIPanel;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleInteger;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@LDLRegister(name = "mproj", group = "editor.machine")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MachineProject implements IProject {
    protected Resources resources;
    protected MBDMachineDefinition definition;
    protected WidgetGroup ui;

    public MachineProject(Resources resources, MBDMachineDefinition definition, WidgetGroup ui) {
        this.resources = resources;
        this.definition = definition;
        this.ui = ui;
    }

    protected Map<String, Resource<?>> createResources() {
        Map<String, Resource<?>> resources = new LinkedHashMap<>();
        // entries
        var entries = new EntriesResource();
        entries.buildDefault();
        resources.put(EntriesResource.RESOURCE_NAME, entries);
        // renderer
        var renderer = new IRendererResource();
        renderer.buildDefault();
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
        var renderer = new IModelRenderer(new ResourceLocation("block/furnace"));
        return MBDMachineDefinition.builder()
                .id(MBD2.id("new_machine"))
                .stateMachine(StateMachine.create(builder -> builder
                        .renderer(new ToggleRenderer(renderer))
                        .shape(new ToggleShape(Shapes.block()))
                        .lightLevel(new ToggleInteger(0))))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
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

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        tag.put("definition", definition.serializeNBT());
        tag.put("ui", IConfigurableWidget.serializeNBT(this.ui, resources, true));
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.definition = MBDMachineDefinition.fromTag(tag.getCompound("definition"));
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
                var proj = new MachineProject();
                proj.deserializeNBT(tag);
                return proj;
            }
        } catch (IOException ignored) {}
        return null;
    }

    // run-time
    private MachineConfigPanel machineConfigPanel;
    private MachineTraitPanel machineTraitPanel;
    private MachineUIPanel machineUIPanel;

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            IProject.super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            machineConfigPanel = new MachineConfigPanel(machineEditor);
            machineTraitPanel = new MachineTraitPanel(machineEditor);
            machineUIPanel = new MachineUIPanel(machineEditor);
            tabContainer.addTab("editor.machine.basic_settings", machineConfigPanel, machineConfigPanel::onPanelSelected);
            tabContainer.addTab("editor.machine.machine_traits", machineTraitPanel, machineTraitPanel::onPanelSelected, machineTraitPanel::onPanelDeselected);
            tabContainer.addTab("editor.machine.machine_ui", machineUIPanel, machineUIPanel::onPanelSelected, machineUIPanel::onPanelDeselected);
        }
    }

    @Override
    public void onClosed(Editor editor) {
        if (machineUIPanel != null && editor instanceof MachineEditor machineEditor) {
            machineEditor.getFloatView().removeWidget(machineUIPanel.getFloatView());
        }
    }
}

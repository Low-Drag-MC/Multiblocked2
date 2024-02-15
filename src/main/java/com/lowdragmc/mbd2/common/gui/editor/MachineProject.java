package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.IProject;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.ColorsResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.EntriesResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.IRendererResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.step.MachineConfigStepPanel;
import com.lowdragmc.mbd2.common.gui.editor.step.MachineStatePanel;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
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

    public MachineProject(Resources resources, MBDMachineDefinition definition) {
        this.resources = resources;
        this.definition = definition;
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
                .stateMachine(new StateMachine(MachineState.builder()
                        .name("base")
                        .renderer(new ToggleRenderer(renderer))
                        .shape(new ToggleShape(Shapes.block()))
                        .lightLevel(new ToggleInteger(0))
                        .child(MachineState.builder()
                                .name("working")
                                .build())
                        .child(MachineState.builder()
                                .name("waiting")
                                .build())
                        .child(MachineState.builder()
                                .name("suspend")
                                .build())
                        .build()))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .build();
    }

    @Override
    public MachineProject newEmptyProject() {
        return new MachineProject(new Resources(createResources()), createDefinition());
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
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

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            IProject.super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            var machineConfigPanel = new MachineConfigStepPanel(machineEditor);
            tabContainer.addTab("editor.machine.basic_settings", machineConfigPanel, machineConfigPanel::onPanelSelected);
            tabContainer.addTab("editor.machine.machine_states", new MachineStatePanel(machineEditor), () -> editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC));
        }
    }
}

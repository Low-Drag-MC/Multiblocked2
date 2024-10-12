package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.StartNode;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineEventsPanel;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import com.lowdragmc.mbd2.common.graphprocessor.MachineEventGraphProcessor;
import com.lowdragmc.mbd2.integration.ldlib.MBDLDLibPlugin;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class ConfigMachineEvents implements IConfigurable, IPersistedSerializable {

    public final Map<String, Class<? extends MachineEvent>> machineEvents = new HashMap<>();

    // graph
    public final Map<Class<? extends MachineEvent>, BaseGraph> eventGraphs = new HashMap<>();

    // runtime
    private final Map<Class<? extends MachineEvent>, MachineEventGraphProcessor> processorCache = new HashMap<>();

    public ConfigMachineEvents registerEventGroup(String group) {
        Optional.ofNullable(MBDLDLibPlugin.REGISTER_MACHINE_EVENTS.get(group))
                .ifPresent(l -> l.forEach(clazz -> machineEvents.put(clazz.getAnnotation(LDLRegister.class).name(), clazz)));
        return this;
    }

    public void postGraphEvent(MachineEvent event) {
        var eventClazz = event.getClass();
        if (!eventGraphs.containsKey(eventClazz)) {
            return;
        }
        if (!processorCache.containsKey(eventClazz)) {
            processorCache.put(eventClazz, new MachineEventGraphProcessor(eventClazz, eventGraphs.get(eventClazz)));
        }
        processorCache.get(eventClazz).postEvent(event);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        var eventGraphsTag = new CompoundTag();
        for (var entry : eventGraphs.entrySet()) {
            if (entry.getKey().isAnnotationPresent(LDLRegister.class)) {
                var name = entry.getKey().getAnnotation(LDLRegister.class).name();
                var graph = entry.getValue();
                if (graph != null) {
                    eventGraphsTag.put(name, entry.getValue().serializeNBT());
                }
            }
        }
        tag.put("eventGraphs", eventGraphsTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        eventGraphs.clear();
        processorCache.clear();
        IPersistedSerializable.super.deserializeNBT(tag);
        var eventGraphsTag = tag.getCompound("eventGraphs");
        for (String name : eventGraphsTag.getAllKeys()) {
            var clazz = machineEvents.get(name);
            if (clazz != null) {
                var parameters = MachineEvent.getExposedParameters(clazz);
                var graph = new BaseGraph(parameters);
                try {
                    graph.deserializeNBT(eventGraphsTag.getCompound(name));
                    eventGraphs.put(clazz, graph);
                } catch (Exception e) {
                    LDLib.LOGGER.error("Failed to deserialize event graph for %s".formatted(name), e);
                }
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        for (var clazz : machineEvents.values()) {
            var annotation = clazz.getAnnotation(LDLRegister.class);
            var eventName = "%s.%s".formatted(annotation.group(), annotation.name());
            var removeButton = new ButtonWidget(90, 2, 80, 11, null);
            removeButton.setButtonTexture(new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), new TextTexture("config.machine_event.graph.remove")));
            removeButton.setVisible(eventGraphs.containsKey(clazz));
            Runnable updateRemoveButtonStyle = () -> removeButton.setVisible(eventGraphs.containsKey(clazz));
            var panel = Editor.INSTANCE instanceof MachineEditor machineEditor ?
                    machineEditor.getTabPages().tabs.values().stream()
                            .filter(MachineEventsPanel.class::isInstance)
                            .map(MachineEventsPanel.class::cast)
                            .findAny().orElse(null) : null;
            removeButton.setOnPressCallback(cd -> {
                var removed = eventGraphs.remove(clazz);
                updateRemoveButtonStyle.run();
                // close event graph editor if its open
                if (panel != null && panel.getCurrentGraph() == removed) {
                    panel.closeEventGraphEditor();
                }
            });
            var backgroundImage = new ImageWidget(0, 2, 80, 11, () -> {
                if (eventGraphs.containsKey(clazz) ) {
                    return panel != null && panel.getCurrentGraph() == eventGraphs.get(clazz) ?
                            ColorPattern.GREEN.rectTexture().setRadius(5):
                            ColorPattern.YELLOW.rectTexture().setRadius(5);
                } else {
                    return ColorPattern.T_GRAY.rectTexture().setRadius(5);
                }
            });
            var editButton = new ButtonWidget(0, 3, 80, 11, cd -> {
                BaseGraph graph = eventGraphs.get(clazz);
                if (graph == null) {
                    var parameters = MachineEvent.getExposedParameters(clazz);
                    graph = new BaseGraph(parameters);
                    // add a trigger node as default
                    graph.addNode(BaseNode.createFromType(StartNode.class, new Position(0, 0)));
                    eventGraphs.put(clazz, graph);
                }
                updateRemoveButtonStyle.run();
                // open editor
                if (panel != null) {
                    panel.openEventGraphEditor(graph);
                }
            }).setButtonTexture(new TextTexture(() -> {
                if (eventGraphs.containsKey(clazz)) {
                    if (panel != null && panel.getCurrentGraph() == eventGraphs.get(clazz)) {
                        return "config.machine_event.graph.editing";
                    }
                    return "config.machine_event.graph.edit";
                } else {
                    return "config.machine_event.graph.add";
                }
            }));
            
            var wrapper = new WrapperConfigurator(eventName, new WidgetGroup(0, 0, 180, 11)
                    .addWidget(backgroundImage)
                    .addWidget(editButton)
                    .addWidget(removeButton));
            wrapper.setTips(eventName + ".tips");
            father.addConfigurators(wrapper);
        }
    }
}

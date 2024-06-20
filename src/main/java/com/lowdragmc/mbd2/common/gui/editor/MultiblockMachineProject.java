package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleInteger;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;

@Getter
@LDLRegister(name = "mb", group = "editor.machine")
@NoArgsConstructor
public class MultiblockMachineProject extends MachineProject {

    BlockPattern pattern;

    public MultiblockMachineProject(Resources resources, MultiblockMachineDefinition definition, WidgetGroup ui) {
        this.resources = resources;
        this.definition = definition;
        this.ui = ui;
    }

    @Override
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    protected MultiblockMachineDefinition createDefinition() {
        // use vanilla furnace model as an example
        var renderer = new IModelRenderer(new ResourceLocation("block/furnace"));
        var builder = MultiblockMachineDefinition.builder();
        builder.id(MBD2.id("new_machine"))
                .stateMachine(StateMachine.create(b -> b
                        .renderer(new ToggleRenderer(renderer))
                        .shape(new ToggleShape(Shapes.block()))
                        .lightLevel(new ToggleInteger(0))))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build());
        return builder.build();
    }

    @Override
    public MultiblockMachineProject newEmptyProject() {
        return new MultiblockMachineProject(new Resources(createResources()), createDefinition(), createDefaultUI());
    }
}

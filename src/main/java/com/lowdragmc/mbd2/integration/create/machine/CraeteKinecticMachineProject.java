package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.IRendererResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigPanel;
import com.mojang.datafixers.util.Either;
import com.simibubi.create.Create;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.world.phys.shapes.Shapes;

import java.io.File;
import java.util.Map;

@Getter
@LDLRegister(name = "km", group = "editor.machine", modID = "create")
@NoArgsConstructor
public class CraeteKinecticMachineProject extends MachineProject {
    public static final IRenderer GEARBOX_RENDERER = new IModelRenderer(MBD2.id("block/gearbox"));
    public static final IModelRenderer SHAFT_RENDERER = new IModelRenderer(Create.asResource("block/shaft"));

    @Getter
    @Setter
    private boolean isRotating = true;
    @Getter
    @Setter
    private float stress = 128;

    public CraeteKinecticMachineProject(Resources resources, CreateKineticMachineDefinition definition, WidgetGroup ui) {
        super(resources, definition, ui);
    }

    public float getSpeed() {
        return Math.min(256, stress / Math.max(getDefinition().kineticMachineSettings.torque(), Float.MIN_VALUE));
    }

    @Override
    protected Map<String, Resource<?>> createResources() {
        var resources = super.createResources();
        if (resources.get(IRendererResource.RESOURCE_NAME) instanceof IRendererResource rendererResource) {
            rendererResource.addResource(Either.left("shaft"), new IModelRenderer(Create.asResource("block/shaft")));
        }
        return resources;
    }

    @Override
    public CreateKineticMachineDefinition getDefinition() {
        return (CreateKineticMachineDefinition) super.getDefinition();
    }

    protected CreateKineticMachineDefinition createDefinition() {
        // use vanilla furnace model as an example
        var builder = CreateKineticMachineDefinition.builder();
        builder.id(MBD2.id("new_machine"))
                .rootState(CreateMachineState.builder()
                        .rotationRenderer(SHAFT_RENDERER)
                        .name("base")
                        .renderer(GEARBOX_RENDERER)
                        .shape(Shapes.block())
                        .lightLevel(0)
                        .child(CreateMachineState.builder()
                                .name("working")
                                .child(CreateMachineState.builder()
                                        .name("waiting")
                                        .build())
                                .build())
                        .child(CreateMachineState.builder()
                                .name("suspend")
                                .build()).build());
        return builder.build();
    }

    @Override
    public CraeteKinecticMachineProject newEmptyProject() {
        return new CraeteKinecticMachineProject(new Resources(createResources()), createDefinition(), createDefaultUI());
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "kinetic_machine");
    }

    @Override
    protected MachineConfigPanel createMachineConfigPanel(MachineEditor editor) {
        var panel = super.createMachineConfigPanel(editor);
        panel.addSwitch(Icons.ROTATION, null, "config.create_kinetic_machine.is_preview_rotating", this::isRotating, this::setRotating);
        panel.refreshButtonGroupPosition();
        var buttonGroup = panel.getButtonGroup();
        var stressConfigurator = new NumberConfigurator("config.create_kinetic_machine.preview_stress",
                this::getStress,
                number -> setStress(number.floatValue()),
                getStress(), true);
        stressConfigurator.setRange(0, Float.MAX_VALUE);
        stressConfigurator.setWheel(1);
        stressConfigurator.init(200);
        stressConfigurator.setSelfPosition(buttonGroup.getSizeWidth() - 200, 25);
        buttonGroup.addWidget(stressConfigurator);
        buttonGroup.addWidget(new ImageWidget(buttonGroup.getSizeWidth() - 200 + 3, 50, 100, 10,
                new TextTexture(() -> LocalizationUtils.format("config.create_kinetic_machine.preview_speed", getSpeed())).setType(TextTexture.TextType.LEFT)));
        return panel;
    }
}

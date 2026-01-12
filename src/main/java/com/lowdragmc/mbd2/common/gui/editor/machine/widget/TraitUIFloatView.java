package com.lowdragmc.mbd2.common.gui.editor.machine.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public class TraitUIFloatView extends FloatViewWidget {

    protected final DraggableScrollableWidgetGroup traitList;

    public TraitUIFloatView() {
        super(200, 200, 206, 120, false);
        traitList = new DraggableScrollableWidgetGroup(5, 5, 196, 110);
        traitList.setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1).transform(-0.5f, 0));
    }

    @Override
    public String name() {
        return "trait_ui_view";
    }

    @Override
    public String group() {
        return "editor.machine";
    }

    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.WATER_BUCKET, Items.CHEST);
    }

    @Override
    public IGuiTexture getHoverIcon() {
        return Icons.REMOVE;
    }

    public MachineEditor getEditor() {
        return (MachineEditor) editor;
    }

    @Override
    public void initWidget() {
        super.initWidget();
        content.addWidget(traitList);
        reloadTrait();
    }

    public void reloadTrait() {
        traitList.clearAllWidgets();
        if (getEditor().getCurrentProject() instanceof MachineProject project) {
            // machine name
            addButton(new ImageWidget(0, 0, 18, 18, new TextTexture("N")),
                    () -> "editor.machine.name", () -> {
                        if (WidgetUtils.getFirstWidgetById(project.getUi(), "^ui:machine_name$") == null) {
                            var name = new TextTextureWidget(5, 5, 100, 18,"editor.machine.name");
                            name.textureStyle(style -> style.setType(TextTexture.TextType.LEFT));
                            name.setId("ui:machine_name");
                            project.getUi().addWidget(name);
                        }
                    });
            // add progress bar
            addButton(new ImageWidget(0, 0, 18, 18, new ProgressTexture(
                    new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0, 1, 0.5),
                    new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0.5, 1, 0.5)
            )), () -> "editor.machine.recipe_type_ui_view.progress", () -> {
                var progress = new ProgressWidget(ProgressWidget.JEIProgress, 5, 5, 18, 18, new ProgressTexture(
                        new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0, 1, 0.5),
                        new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0.5, 1, 0.5)
                ));
                progress.setId("ui:progress_bar");
                project.getUi().addWidget(progress);
            });
            // add fuel bar
            addButton(new ImageWidget(0, 0, 18, 18, new ProgressTexture()), () -> "editor.machine.recipe_type_ui_view.fuel_progress", () -> {
                if (WidgetUtils.getFirstWidgetById(project.getUi(), "^ui:fuel_bar$") == null) {
                    var progress = new ProgressWidget(ProgressWidget.JEIProgress, 5, 5, 18, 18, new ProgressTexture());
                    progress.setId("ui:fuel_bar");
                    project.getUi().addWidget(progress);
                }
            });
            // add xei lookup
            addButton(new ButtonWidget(0, 0, 18, 18,
                    new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("XEI").scale(0.8f)), null),
                    () -> "editor.machine.recipe_type_ui_view.xei_lookup", () -> {
                        if (WidgetUtils.getFirstWidgetById(project.getUi(), "^ui:xei_lookup$") == null) {
                            var button = new ButtonWidget(5, 5, 18, 18,
                                    new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("XEI").scale(0.8f)), null);
                            button.setId("ui:xei_lookup");
                            button.setHoverTooltips("editor.machine.recipe_type_ui_view.xei_lookup.hover");
                            project.getUi().addWidget(button);
                        }
            });
            // add traits
            project.getDefinition().machineSettings().traitDefinitions()
                    .stream().filter(IUIProviderTrait.class::isInstance).map(IUIProviderTrait.class::cast)
                    .forEach(this::addUITrait);
        }
    }

    public void addUITrait(IUIProviderTrait provider) {
        addButton(new ImageWidget(0, 0, 18, 18, provider.getDefinition().getIcon()),
                provider.getDefinition()::getName, () -> {
                    if (getEditor().getCurrentProject() instanceof MachineProject project) {
                        provider.createTraitUITemplate(project.getUi());
                    }
                },
                "config.definition.trait.ui.generate.tooltip",
                "config.definition.trait.%s.ui.tooltip".formatted(provider.getDefinition().getTranslateKey()));
    }

    public void addButton(Widget icon, Supplier<String > value, Runnable onClick, String... hoverTooltips) {
        int yOffset = 3 + traitList.getAllWidgetSize() * 20;
        var widgetGroup = new WidgetGroup(0, yOffset, 90, 18);
        icon.setSelfPosition(1, 0);
        widgetGroup.addWidget(icon);
        widgetGroup.addWidget(new ImageWidget(20, 1, 120, 18,
                new TextTexture().setSupplier(value).setType(TextTexture.TextType.ROLL_ALWAYS).setWidth(120)));
        widgetGroup.addWidget(new ButtonWidget(145, 2, 45, 14,
                new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(7),
                        ColorPattern.WHITE.borderTexture(-1).setRadius(7), new TextTexture("editor.machine.recipe_type_ui_view.add")),
                cd -> onClick.run())
                .setHoverTexture(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(7),
                        ColorPattern.GREEN.borderTexture(-1).setRadius(7), new TextTexture("editor.machine.recipe_type_ui_view.add")))
                .setHoverTooltips(hoverTooltips));
        traitList.addWidget(widgetGroup);
    }
}

package com.lowdragmc.mbd2.common.gui.editor.recipe.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.ContentWidget;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;
import com.lowdragmc.mbd2.common.recipe.DimensionCondition;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RecipeTypeUIFloatView extends FloatViewWidget {

    protected final DraggableScrollableWidgetGroup uiList;

    public RecipeTypeUIFloatView() {
        super(200, 200, 206, 120, false);
        uiList = new DraggableScrollableWidgetGroup(5, 5, 196, 110);
        uiList.setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1).transform(-0.5f, 0));
    }

    @Override
    public String name() {
        return "recipe_type_ui_view";
    }

    @Override
    public String group() {
        return "editor.machine";
    }

    public IGuiTexture getIcon() {
        return new ProgressTexture();
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
        content.addWidget(uiList);
        reloadList();
    }

    public void reloadList() {
        uiList.clearAllWidgets();
        if (getEditor().getCurrentProject() instanceof RecipeTypeProject project) {
            // create progress bar
            addButton(new ImageWidget(0, 0, 18, 18, new ProgressTexture(
                    new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0, 1, 0.5),
                    new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0.5, 1, 0.5)
            )), () -> "editor.machine.recipe_type_ui_view.progress", () -> {
                if (WidgetUtils.getFirstWidgetById(project.getUi(), "@progress_bar") == null) {
                    var progress = new ProgressWidget(ProgressWidget.JEIProgress, 5, 5, 18, 18, new ProgressTexture(
                            new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0, 1, 0.5),
                            new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0.5, 1, 0.5)
                    ));
                    progress.setId("@progress_bar");
                    project.getUi().addWidget(progress);
                }
            });

            // create duration label
            addButton(new ImageWidget(0, 0, 18, 18, Icons.FILE), () -> "editor.machine.recipe_type_ui_view.duration", () -> {
                if (WidgetUtils.getFirstWidgetById(project.getUi(), "@duration") == null) {
                    var duration = new LabelWidget(5, 5, Component.translatable("recipe.duration.value", 100));
                    duration.setId("@duration");
                    project.getUi().addWidget(duration);
                }
            });

            // create conditions
            addButton(new ImageWidget(0, 0, 18, 18, DimensionCondition.INSTANCE.getIcon()), () -> "editor.machine.recipe_type_ui_view.condition", () -> {
                if (WidgetUtils.getFirstWidgetById(project.getUi(), "@condition") == null) {
                    var duration = new TextBoxWidget(5, 5, project.getUi().getSizeWidth() - 10, List.of(DimensionCondition.INSTANCE.getTooltips().getString()));
                    duration.isShadow = true;
                    duration.fontColor = -1;
                    duration.setId("@condition");
                    project.getUi().addWidget(duration);
                }
            });

            // create button to generate ui
            Map<RecipeCapability<?>, Integer> maxInputs = new HashMap<>();
            Map<RecipeCapability<?>, Integer> maxOutputs = new HashMap<>();
            for (var recipe : project.getRecipeType().getBuiltinRecipes().values()) {
                for (var entry : recipe.inputs.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        var cap = maxInputs.getOrDefault(entry.getKey(), 0);
                        maxInputs.put(entry.getKey(), Math.max(cap, entry.getValue().size()));
                    }
                }
                for (var entry : recipe.outputs.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        var cap = maxOutputs.getOrDefault(entry.getKey(), 0);
                        maxOutputs.put(entry.getKey(), Math.max(cap, entry.getValue().size()));
                    }
                }
            }
            maxInputs.forEach((cap, maxSize) -> addCap(cap, maxSize, IO.IN));
            maxOutputs.forEach((cap, maxSize) -> addCap(cap, maxSize, IO.OUT));
        }
    }

    public void addButton(Widget icon, Supplier<String > value, Runnable onClick) {
        int yOffset = 3 + uiList.getAllWidgetSize() * 20;
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
                        ColorPattern.GREEN.borderTexture(-1).setRadius(7), new TextTexture("editor.machine.recipe_type_ui_view.add"))));
        uiList.addWidget(widgetGroup);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addCap(RecipeCapability cap, int maxSize, IO io) {
        if (getEditor().getCurrentProject() instanceof RecipeTypeProject project) {
            addButton(cap.createPreviewWidget(cap.createDefaultContent()), () -> {
                var found = 0;
                for (int i = 0; i < maxSize; i++) {
                    var id = "@%s_%s_%d".formatted(cap.name, io.name, i);
                    if (WidgetUtils.getFirstWidgetById(project.getUi(), id) != null) {
                        found++;
                    }
                }
                if (found < maxSize) {
                    return "%s: §e%d§r/ %d".formatted(
                            io.name,
                            found,
                            maxSize);
                } else {
                    return "%s: §2%d§r/ %d".formatted(
                            io.name,
                            found,
                            maxSize);
                }
            }, () -> {
                var ui = project.getUi();
                var x = 5;
                for (int i = 0; i < maxSize; i++) {
                    var id = "@%s_%s_%d".formatted(cap.name, io.name, i);
                    if (WidgetUtils.getFirstWidgetById(ui, id) == null) {
                        var template = cap.createXEITemplate();
                        template.setSelfPosition(x, 5);
                        template.setId(id);
                        x += template.getSize().width;
                        ui.addWidget(template);
                    }
                }
            });
        }
    }

}

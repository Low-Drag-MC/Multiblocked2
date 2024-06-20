package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ConditionContainer extends WidgetGroup {
    private final List<RecipeCondition> conditions;
    private final DraggableScrollableWidgetGroup container;
    @Getter
    @Nullable
    private RecipeCondition selected;

    public ConditionContainer(int x, int y, int width, int height, List<RecipeCondition> conditions) {
        super(x, y, width, height);
        this.conditions = conditions;
        this.container = new DraggableScrollableWidgetGroup(0, 0, width, height);
        this.container.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        this.container.setBackground(ColorPattern.T_WHITE.borderTexture(1));
        addWidget(container);
        reloadConditions();
    }

    private void reloadConditions() {
        container.clearAllWidgets();
        for (var condition : conditions) {
            var conditionLine = createConditionLine(condition);
            conditionLine.setSelfPosition(0, container.getAllWidgetSize() * 20);
            conditionLine.setSelectedTexture(new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture()));
            conditionLine.setOnSelected(group -> {
                selected = condition;
                if (Editor.INSTANCE != null) {
                    Editor.INSTANCE.getConfigPanel().openConfigurator(MachineEditor.SECOND, condition);
                }
            });
            conditionLine.setOnUnSelected(group -> {
                selected = null;
                if (Editor.INSTANCE != null) {
                    Editor.INSTANCE.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
                }
            });
            container.addWidget(conditionLine);
        }
    }

    private SelectableWidgetGroup createConditionLine(RecipeCondition condition) {
        var width = container.getSizeWidth() - 5;
        var conditionLine = new SelectableWidgetGroup(0, 0, width, 20);
        conditionLine.addWidget(new ImageWidget(1, 1, 18, 18, condition.getIcon()));
        conditionLine.addWidget(new ImageWidget(25, 1, width - 25, 18, new TextTexture()
                .setWidth(width - 20)
                .setType(TextTexture.TextType.LEFT_HIDE)
                .setSupplier(() -> condition.getTooltips().getString())));
        return conditionLine;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (container.isMouseOverElement(mouseX, mouseY) && button == 1 && Editor.INSTANCE != null) {
            var menu = TreeBuilder.Menu.start()
                    .branch(Icons.ADD_FILE, "editor.machine.recipe_type.add_condition", m -> {
                        for (var clazz : MBDRegistries.RECIPE_CONDITIONS.values()) {
                            var condition = RecipeCondition.create(clazz);
                            if (condition != null) {
                                m.leaf(condition.getIcon(), condition.getTranslationKey(), () -> {
                                    conditions.add(condition);
                                    reloadConditions();
                                });
                            }
                        }
                    });
            if (selected != null) {
                menu.crossLine();
                menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> {
                    var copied = selected.copy();
                    conditions.add(copied);
                    reloadConditions();
                });
                menu.leaf(Icons.REMOVE_FILE, "editor.machine.recipe_type.remove_condition", () -> {
                    conditions.remove(selected);
                    selected = null;
                    Editor.INSTANCE.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
                    reloadConditions();
                });
            }
            Editor.INSTANCE.openMenu(mouseX, mouseY, menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}

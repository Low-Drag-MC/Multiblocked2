package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentWidget;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RecipeList extends DraggableScrollableWidgetGroup {
    private final RecipeTypePanel recipeTypePanel;
    private final boolean isFuel;
    @Nullable
    private MBDRecipe selected;
    @Setter
    @Nullable
    private Runnable onSelected;

    public RecipeList(RecipeTypePanel recipeTypePanel, Size size, boolean isFuel) {
        super(0, 0, size.width, size.height);
        this.recipeTypePanel = recipeTypePanel;
        this.isFuel = isFuel;
        setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        this.recipeTypePanel.recipeType.getBuiltinRecipes().values().stream().filter(recipe -> recipe.isFuel() == isFuel).forEach(this::addRecipe);
    }

    public void addRecipe(MBDRecipe recipe) {
        int yOffset = 3 + widgets.size() * 32;
        var selectableWidgetGroup = new SelectableWidgetGroup(0, yOffset, getSizeWidth() - 2, 30);
        selectableWidgetGroup.setPrefab(recipe);
        // add id
        selectableWidgetGroup.addWidget(new ImageWidget(1, 0, getSizeWidth() - 2, 10,
                new TextTexture().setSupplier(() -> recipe.getId().toString()).setType(TextTexture.TextType.LEFT_HIDE).setWidth(getSizeWidth() - 2)));
        // add progress
        selectableWidgetGroup.addWidget(new ProgressWidget(ProgressWidget.JEIProgress, (getSizeWidth() - 2) / 2 - 9, 11, 18, 18,
                isFuel ? new ProgressTexture() : new ProgressTexture(
                        new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0, 1, 0.5),
                        new ResourceTexture("mbd2:textures/gui/arrow_bar.png").getSubTexture(0, 0.5, 1, 0.5)
        )));
        // add inputs
        AtomicReference<WidgetGroup> inputs = new AtomicReference<>();
        inputs.set(createContents(recipe.inputs, 0, 20));
        selectableWidgetGroup.addWidget(inputs.get());

        // add outputs
        AtomicReference<WidgetGroup> outputs = new AtomicReference<>();
        if (!isFuel) {
            outputs.set(createContents(recipe.outputs, getSizeWidth() - 24, -20));
            selectableWidgetGroup.addWidget(outputs.get());
        }

        selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
        selectableWidgetGroup.setOnSelected(group -> {
            selected = recipe;
            if (onSelected != null) onSelected.run();
            recipeTypePanel.contentGroup.clearAllWidgets();
            var w = recipeTypePanel.contentGroup.getSizeWidth();
            var h = recipeTypePanel.contentGroup.getSizeHeight() - 20;
            // add tab container for contents, conditions, and data
            var tabContainer = new TabContainer(0, 15, w, h);
            recipeTypePanel.contentGroup.addWidget(tabContainer);
            // add contents tab
            ContentContainer inputsContainer, outputsContainer;
            var container =  new WidgetGroup(0, 0, w, h)
                    .addWidget(new ImageWidget(0, 0, w, 10, new TextTexture(IO.IN.getTooltip()).setWidth(w)))
                    .addWidget(inputsContainer = new ContentContainer(0, 10, w, h / 2 - 18, recipe.inputs, () -> {
                        selectableWidgetGroup.removeWidget(inputs.get());
                        inputs.set(createContents(recipe.inputs, 0, 20));
                        selectableWidgetGroup.addWidget(inputs.get());
                    }));
            var durationWidget = new NumberConfigurator("recipe.duration", () -> recipe.duration, v -> recipe.duration = v.intValue(), 100, true);
            durationWidget.setRange(1, Integer.MAX_VALUE);
            durationWidget.setTips(isFuel ? "recipe.duration.fuel.tooltip" : "recipe.duration.common.tooltip");
            durationWidget.init(100);
            durationWidget.setSelfPosition((w - 200) / 2, h / 2 - 7);
            container.addWidget(durationWidget);
            var priorityWidget = new NumberConfigurator("recipe.priority", () -> recipe.priority, v -> recipe.priority = v.intValue(), 0, true);
            priorityWidget.setRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
            priorityWidget.setTips( "recipe.priority.tooltip");
            priorityWidget.init(100);
            priorityWidget.setSelfPosition((w - 200) / 2 + 100, h / 2 - 7);
            container.addWidget(priorityWidget);
            if (!isFuel) {
                container.addWidget(new ImageWidget(0, h / 2 + 8, w, 10, new TextTexture(IO.OUT.getTooltip()).setWidth(w)))
                        .addWidget(outputsContainer = new ContentContainer(0, h / 2 + 18, w, h / 2 - 18, recipe.outputs, () -> {
                            selectableWidgetGroup.removeWidget(outputs.get());
                            outputs.set(createContents(recipe.outputs, getSizeWidth() - 24, -20));
                            selectableWidgetGroup.addWidget(outputs.get());
                        }));
                inputsContainer.setOnSelected(outputsContainer::clearSelected);
                outputsContainer.setOnSelected(inputsContainer::clearSelected);
                // update recipe preview
                recipeTypePanel.getFloatView().loadRecipe(selected);
            }
            tabContainer.addTab(createTabButton(0, "editor.machine.recipe_list.contents"), container);

            // add condition tab
            tabContainer.addTab(createTabButton(1, "editor.machine.recipe_list.conditions"), new WidgetGroup(0, 0, w, h)
                    .addWidget(new ImageWidget(0, 0, w, 10, new TextTexture("recipe.condition.name").setWidth(w)))
                    .addWidget(new ConditionContainer(0, 10, w, h - 10, recipe.conditions)));


            // add data tab
            tabContainer.addTab(createTabButton(2, "editor.machine.recipe_list.data"), new WidgetGroup(0, 0, w, h));
        });
        selectableWidgetGroup.setOnUnSelected(group -> {
            selected = null;
            recipeTypePanel.contentGroup.clearAllWidgets();
            recipeTypePanel.editor.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
            recipeTypePanel.getFloatView().clearRecipe();
        });
        addWidget(selectableWidgetGroup);
    }

    private TabButton createTabButton(int index, String name) {
        return new TabButton(index * 100, -16, 80, 11)
                .setTexture(
                        new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture().setRadius(15 / 2f),
                                new TextTexture(name).setWidth(80).setDropShadow(false).setType(TextTexture.TextType.ROLL)),
                        new GuiTextureGroup(ColorPattern.T_GREEN.rectTexture().setRadius(15 / 2f),
                                new TextTexture(name).setWidth(80).setDropShadow(false).setType(TextTexture.TextType.ROLL))
                )
                .setHoverTexture(
                        new GuiTextureGroup(ColorPattern.T_WHITE.rectTexture().setRadius(15 / 2f),
                                new TextTexture(name).setWidth(80).setDropShadow(false).setType(TextTexture.TextType.ROLL)));
    }

    private WidgetGroup createContents(Map<RecipeCapability<?>, List<Content>> recipeContents, int x, int offset) {
        WidgetGroup group = new WidgetGroup(0, 0, 0, 0);
        var left = (getSizeWidth() - 22) / 2 / 20;
        for (var entry : recipeContents.entrySet()) {
            if (left > 0) {
                var contents = entry.getValue();
                for (var content : contents) {
                    var widget = new ContentWidget<>(x, 10, entry.getKey(), content);
                    group.addWidget(widget);
                    left--;
                    x += offset;
                    if (left == 0) break;
                }
            }
            if (left == 0) break;
        }
        return group;
    }

    public void removeRecipe(MBDRecipe recipe) {
        this.recipeTypePanel.recipeType.getBuiltinRecipes().remove(recipe.getId(), recipe);
        for (Widget widget : widgets) {
            if (widget instanceof SelectableWidgetGroup selectableWidgetGroup) {
                if (selectableWidgetGroup.getPrefab() == recipe) {
                    var index = widgets.indexOf(selectableWidgetGroup);
                    for (int i = index + 1; i < widgets.size(); i++) {
                        widgets.get(i).addSelfPosition(0, - 30);
                    }
                    widgets.remove(selectableWidgetGroup);
                    break;
                }
            }
        }
        computeMax();
        if (this.selected == recipe) {
            this.selected = null;
            this.recipeTypePanel.contentGroup.clearAllWidgets();
            this.recipeTypePanel.editor.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
            recipeTypePanel.getFloatView().clearRecipe();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && button == 1) {
            var menu = TreeBuilder.Menu.start()
                    .leaf(Icons.ADD_FILE, "editor.machine.recipe_type.add_recipe", () -> {
                        DialogWidget.showStringEditorDialog(this.recipeTypePanel.editor, "editor.machine.recipe_type.add_recipe", "unique_id",
                                s -> true, s -> {
                                    if (s == null || !ResourceLocation.isValidResourceLocation(s)) return;
                                    var id = new ResourceLocation(s);
                                    if (isFuel) {
                                        id = new ResourceLocation(id.getNamespace(), "fuel/" + id.getPath());
                                    }
                                    if (!this.recipeTypePanel.recipeType.getBuiltinRecipes().containsKey(id)) {
                                        addRecipe(this.recipeTypePanel.recipeType.recipeBuilder(id)
                                                .isFuel(isFuel)
                                                .duration(100)
                                                .saveAsBuiltinRecipe());
                                    }
                                });
                    })
                    .leaf(Icons.ADD_FILE, "editor.machine.recipe_type.add_recipe_auto_id", () -> {
                        var index = 0;
                        var path = this.recipeTypePanel.recipeType.getRegistryName().getPath() + "/" + (isFuel ? "fuel/" : "") + "recipe_";
                        var id = new ResourceLocation(this.recipeTypePanel.recipeType.getRegistryName().getNamespace(), path + index++);
                        while (this.recipeTypePanel.recipeType.getBuiltinRecipes().containsKey(id)) {
                            id = new ResourceLocation(id.getNamespace(), path + index++);
                        }
                        addRecipe(this.recipeTypePanel.recipeType.recipeBuilder(id)
                                .isFuel(isFuel)
                                .duration(100)
                                .saveAsBuiltinRecipe());
                    });
            if (selected != null) {
                menu.crossLine();
                menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> {
                    var copiedID = new ResourceLocation(selected.getId().getNamespace(), selected.getId().getPath() + "_copy");
                    var index = 0;
                    while (this.recipeTypePanel.recipeType.getBuiltinRecipes().containsKey(copiedID)) {
                        copiedID = new ResourceLocation(copiedID.getNamespace(), copiedID.getPath() + "_" + index++);
                    }
                    var copied = selected.copy(copiedID);
                    this.recipeTypePanel.recipeType.getBuiltinRecipes().put(copiedID, copied);
                    addRecipe(copied);
                });
                menu.leaf(Icons.REMOVE_FILE, "editor.machine.recipe_type.remove_recipe", () -> removeRecipe(selected));
            }
            this.recipeTypePanel.editor.openMenu(mouseX, mouseY, menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

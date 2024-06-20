package com.lowdragmc.mbd2.common.gui.editor.recipe;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.texture.WidgetTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentWidget;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Tuple;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class ContentContainer extends WidgetGroup {
    private final Map<RecipeCapability<?>, List<Content>> contents;
    private final DraggableScrollableWidgetGroup container;
    private final Runnable onContentUpdate;
    @Getter
    @Nullable
    private Tuple<RecipeCapability, Content> selected;
    @Setter
    @Nullable
    private Runnable onSelected;

    public ContentContainer(int x, int y, int width, int height, Map<RecipeCapability<?>, List<Content>> contents, Runnable onContentUpdate) {
        super(x, y, width, height);
        this.contents = contents;
        this.onContentUpdate = onContentUpdate;
        this.container = new DraggableScrollableWidgetGroup(0, 15, width, height - 15);
        this.container.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2).transform(-0.5f, 0));
        this.container.setBackground(ColorPattern.T_WHITE.borderTexture(1));
        addWidget(container);
        createTitle();
        reloadContents();
    }

    public void clearSelected() {
        container.setSelected(null);
    }

    private void createTitle() {
        var width = container.getSizeWidth() - 5;
        var dur = 5;
        var x = 0;
        var textFieldWidth = Math.max(50, (width - 20 -dur - 20 -dur) / 4 - dur);
        appendTitleWidget(x, 20, "C");
        x += (20 + dur);
        if (x + 20 > width) return;
        appendTitleWidget(x, 20, "tick");
        x += (20 + dur);
        if (x + textFieldWidth > width) return;
        appendTitleWidget(x, textFieldWidth, "editor.machine.recipe_type.content.chance");
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return;
        appendTitleWidget(x, textFieldWidth, "editor.machine.recipe_type.content.tier_chance_boost");
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return;
        appendTitleWidget(x, textFieldWidth, "editor.machine.recipe_type.content.slot_name");
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return;
        appendTitleWidget(x, textFieldWidth, "editor.machine.recipe_type.content.ui_name");
    }

    private void appendTitleWidget(int x, int width, String C) {
        addWidget(new ImageWidget(x, 0, width, 15, new TextTexture(C).setWidth(width)));
    }

    private void reloadContents() {
        container.clearAllWidgets();
        for (var entry : contents.entrySet()) {
            var cap = (RecipeCapability) entry.getKey();
            var contentList = entry.getValue();
            for (var content : contentList) {
                var contentLine = createContentLine(cap, content);
                contentLine.setSelfPosition(0, container.getAllWidgetSize() * 20);
                contentLine.setSelectedTexture(new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture()));
                contentLine.setOnSelected(group -> {
                    selected = new Tuple<>(cap, content);
                    if (onSelected != null) {
                        onSelected.run();
                    }
                    if (Editor.INSTANCE != null) {
                        Editor.INSTANCE.getConfigPanel().openConfigurator(MachineEditor.SECOND, new IConfigurable() {
                            @Override
                            public void buildConfigurator(ConfiguratorGroup father) {
                                var contentGroup = new ConfiguratorGroup("editor.machine.basic_settings");
                                ConfiguratorParser.createConfigurators(contentGroup, new HashMap<>(), content.getClass(), content);
                                father.addConfigurators(contentGroup);
                                cap.createContentConfigurator(father, () -> cap.of(content.content), c -> content.content = c);
                            }
                        });

                    }
                });
                contentLine.setOnUnSelected(group -> {
                    selected = null;
                    if (Editor.INSTANCE != null) {
                        Editor.INSTANCE.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
                    }
                });
                container.addWidget(contentLine);
            }
        }
    }

    private SelectableWidgetGroup createContentLine(RecipeCapability<?> cap, Content content) {
        var width = container.getSizeWidth() - 5;
        var contentLine = new SelectableWidgetGroup(0, 0, width, 20);
        var contentWidget = new ContentWidget<>(0, 0, cap, content);
        var dur = 5;
        var x = 0;
        var textFieldWidth = Math.max(50, (width - 20 -dur - 20 -dur) / 4 - dur);
        contentLine.addWidget(contentWidget);
        x += (20 + dur);
        if (x + 20 > width) return contentLine;
        contentLine.addWidget(new SwitchWidget(x + 1, 1, 18, 18,
                (cd, pressed) -> content.perTick= pressed)
                .setSupplier(() -> content.perTick)
                .setTexture(
                        new GuiTextureGroup(ColorPattern.WHITE.borderTexture(-2)).scale(0.6f),
                        new GuiTextureGroup(ColorPattern.WHITE.borderTexture(-2), ColorPattern.WHITE.rectTexture().scale(0.5f)).scale(0.6f)
                        ));
        x += (20 + dur);
        if (x + textFieldWidth > width) return contentLine;
        createFloatField(contentLine, x, textFieldWidth, content.chance, f -> content.chance = f, 0, 1);
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return contentLine;
        createFloatField(contentLine, x, textFieldWidth, content.tierChanceBoost, f -> content.tierChanceBoost = f, 0, 1);
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return contentLine;
        createStringField(contentLine, x, textFieldWidth, content.slotName, s -> content.slotName = s);
        x += (textFieldWidth + dur);
        if (x + textFieldWidth > width) return contentLine;
        createStringField(contentLine, x, textFieldWidth, content.uiName, s -> content.uiName = s);
        return contentLine;
    }

    private static void createFloatField(WidgetGroup contentLine, int x, int textFieldWidth, float initial, Consumer<Float> setter, float min, float max) {
        var textField = new TextFieldWidget(x + 3, 5, textFieldWidth - 3, 10, null,
                s -> setter.accept(Float.parseFloat(s)))
                .setCurrentString(String.valueOf(initial))
                .setNumbersOnly(min, max);
        textField.setBordered(false);
        textField.setWheelDur(0.1f);
        contentLine.addWidget(new ImageWidget(x, 5, textFieldWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        contentLine.addWidget(textField);
    }

    private static void createStringField(WidgetGroup contentLine, int x, int textFieldWidth, @Nullable String initial, Consumer<String> setter) {
        var textField = new TextFieldWidget(x + 3, 5, textFieldWidth - 3, 10, null, setter)
                .setCurrentString(initial == null ? "" : initial);
        textField.setBordered(false);
        contentLine.addWidget(new ImageWidget(x, 5, textFieldWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        contentLine.addWidget(textField);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (container.isMouseOverElement(mouseX, mouseY) && button == 1 && Editor.INSTANCE != null) {
            var menu = TreeBuilder.Menu.start()
                    .branch(Icons.ADD_FILE, "editor.machine.recipe_type.add_content", m -> {
                        for (RecipeCapability cap : MBDRegistries.RECIPE_CAPABILITIES.values()) {
                            m.leaf(new WidgetTexture(cap.createPreviewWidget(cap.createDefaultContent()).setClientSideWidget()), "recipe.capability.%s.name".formatted(cap.name), () -> {
                                var content = cap.createDefaultContent();
                                contents.computeIfAbsent(cap, c -> new ArrayList<>()).add(
                                        new Content(content, false, 1, 0));
                                reloadContents();
                                onContentUpdate.run();
                            });
                        }
                    });
            if (selected != null) {
                menu.crossLine();
                menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> {
                    var copied = selected.getB().copy(selected.getA(), null);
                    contents.computeIfAbsent(selected.getA(), cap -> new ArrayList<>()).add(copied);
                    reloadContents();
                    onContentUpdate.run();
                });
                menu.leaf(Icons.REMOVE_FILE, "editor.machine.recipe_type.remove_content", () -> {
                    var contentList = contents.getOrDefault(selected.getA(), Collections.emptyList());
                    contentList.remove(selected.getB());
                    if (contentList.isEmpty()) {
                        contents.remove(selected.getA());
                    }
                    selected = null;
                    Editor.INSTANCE.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
                    reloadContents();
                    onContentUpdate.run();
                });
            }
            Editor.INSTANCE.openMenu(mouseX, mouseY, menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}

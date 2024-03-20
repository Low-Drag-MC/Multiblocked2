package com.lowdragmc.mbd2.common.gui.editor.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.trait.ITraitUIProvider;
import net.minecraft.world.item.Items;

public class TraitUIFloatView extends FloatViewWidget {

    protected final DraggableScrollableWidgetGroup traitList;

    public TraitUIFloatView() {
        super(100, 100, 206, 120, false);
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
            project.getDefinition().machineSettings().traitDefinitions()
                    .stream().filter(ITraitUIProvider.class::isInstance).map(ITraitUIProvider.class::cast)
                    .forEach(this::addUITrait);
        }
    }

    public void addUITrait(ITraitUIProvider provider) {
        int yOffset = 3 + traitList.getAllWidgetSize() * 20;
        var widgetGroup = new WidgetGroup(0, yOffset, 90, 18);
        widgetGroup.addWidget(new ImageWidget(1, 0, 18, 18, provider.getDefinition().getIcon()));
        widgetGroup.addWidget(new ImageWidget(20, 0, 80, 18,
                new TextTexture().setSupplier(provider.getDefinition()::getName).setType(TextTexture.TextType.HIDE)));
        widgetGroup.addWidget(new ButtonWidget(105, 2, 85, 14,
                new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(7),
                        ColorPattern.WHITE.borderTexture(-1).setRadius(7), new TextTexture("config.definition.trait.ui.generate")),
                cd -> {
                    if (getEditor().getCurrentProject() instanceof MachineProject project){
                        project.getUi().addWidget(provider.createTraitUITemplate());
                    }
                })
                .setHoverTexture(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(7),
                        ColorPattern.GREEN.borderTexture(-1).setRadius(7), new TextTexture("config.definition.trait.ui.generate")))
                .setHoverTooltips(
                        "config.definition.trait.ui.generate.tooltip",
                        "config.definition.trait.%s.ui.tooltip".formatted(provider.getDefinition().name())));
        traitList.addWidget(widgetGroup);
    }

}

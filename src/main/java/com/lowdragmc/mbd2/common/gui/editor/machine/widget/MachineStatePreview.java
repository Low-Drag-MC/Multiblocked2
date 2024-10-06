package com.lowdragmc.mbd2.common.gui.editor.machine.widget;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Optional;

public class MachineStatePreview extends DraggableWidgetGroup {
    @Getter
    private final MachineConfigPanel panel;
    @Getter
    private final MachineState state;
    private final WidgetGroup title;
    private final WidgetGroup content;
    @Getter
    private boolean isCollapse;
    protected MBDMachine previewMachine;
    // runtime
    private long lastClickTick;

    public MachineStatePreview(MachineConfigPanel panel, MachineState state) {
        super(0, 0, 100, 100 + 15);
        this.panel = panel;
        this.state = state;

        // init frame
        this.addWidget(this.title = new WidgetGroup(0, 0, this.getSize().width, 15));
        this.title.setBackground(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setTopRadius(5.0F), ColorPattern.GRAY.borderTexture(-1).setTopRadius(5.0F)));
        this.title.addWidget((new ImageWidget(2, 2, 11, 11, Icons.FILE)));
        this.title.addWidget(new ImageWidget(15, 0, this.getSize().width - 15, 15,
                new TextTexture("").setSupplier(state::name).setType(TextTexture.TextType.LEFT_ROLL).setWidth(this.getSize().width - 15)));
        this.addWidget(this.content = new WidgetGroup(0, 15, this.getSize().width, this.getSize().height - 15));
        this.content.setBackground(new GuiTextureGroup(ColorPattern.BLACK.rectTexture().setBottomRadius(5.0F), ColorPattern.GRAY.borderTexture(-1).setBottomRadius(5.0F)));

        // init scene
        var scene = new SceneWidget(4, 4, content.getSize().width - 8, content.getSize().height - 8, null);
        var level = new TrackedDummyWorld();
        content.addWidget(scene);
        scene.setIntractable(false);
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level);
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(MBDRegistries.FAKE_MACHINE().block()));
        Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof MachineBlockEntity holder && panel.getEditor().getCurrentProject() instanceof MachineProject project) {
                holder.setMachine(this.previewMachine = project.getDefinition().createMachine(holder));
                previewMachine.setMachineState(state.name());
            }
        });

        setSelectedTexture(ColorPattern.GREEN.borderTexture(1).setRadius(5.0F));
        // accept dragging renderer
        var draggingBorder = new ImageWidget(0, 0, this.getSize().width, this.getSize().height,
                ColorPattern.YELLOW.borderTexture(2).setRadius(5.0F));
        draggingBorder.setVisible(false);
        this.addWidget(draggingBorder);
        this.setDraggingConsumer(IRenderer.class::isInstance, o -> {
                    draggingBorder.setVisible(true);
                    draggingBorder.setSize(this.getSize().width, this.getSize().height);
                },
                o -> draggingBorder.setVisible(false), o -> {
                    draggingBorder.setVisible(false);
                    if (o instanceof IRenderer renderer) {
                        state.renderer().setEnable(true);
                        state.renderer().setValue(renderer);
                    }
                });
    }

    public void collapse() {
        this.isCollapse = !this.isCollapse;
        if (this.isCollapse) {
            this.title.setSize(new Size(this.content.getSize().width, 15));
            this.title.setBackground(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setRadius(5.0F), ColorPattern.GRAY.borderTexture(-1).setRadius(5.0F)));
            this.content.setVisible(false);
            this.content.setActive(false);
            this.setSize(new Size(this.content.getSize().width, 15));
        } else {
            this.title.setSize(new Size(this.content.getSize().width, 15));
            this.title.setBackground(new GuiTextureGroup(ColorPattern.T_RED.rectTexture().setTopRadius(5.0F), ColorPattern.GRAY.borderTexture(-1).setTopRadius(5.0F)));
            this.content.setVisible(true);
            this.content.setActive(true);
            this.setSize(new Size(this.content.getSize().width, this.content.getSize().height + 15));
        }
    }

    public IGuiTexture getIcon() {
        return Icons.HISTORY;
    }

    public boolean isRoot() {
        return this.state.isRoot();
    }

    @Override
    public boolean canDragOutRange() {
        return true;
    }

    @Override
    public void onSelected() {
        super.onSelected();
        panel.onStateSelected(state);
    }

    protected TreeBuilder.Menu createMenuTree() {
        var menu = TreeBuilder.Menu.start();
        menu.leaf(Icons.ADD, "editor.machine_state.add", () -> {
            DialogWidget.showStringEditorDialog(panel, "editor.machine_state.add", "new_state",
                    s -> true, s -> {
                        if (s != null && state.stateMachine() != null && !state.stateMachine().hasState(s)) {
                            var newState =  state.addChild(s);
                            panel.onStateAdded(newState);
                        }
                    });
        });
        if (!isRoot() && state.parent() != null) {
            menu.leaf(Icons.REMOVE, "editor.machine_state.remove", () -> {
                panel.onStateRemoved(state);
                state.parent().removeChild(state);
            });
        }
        return menu;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (content.isMouseOverElement(mouseX, mouseY) && button == 1) {
            var menu = createMenuTree();
            if (menu != null) {
                panel.waitToAdded(new MenuWidget<>((int) mouseX, (int) mouseY, 14, menu.build())
                        .setNodeTexture(MenuWidget.NODE_TEXTURE)
                        .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                        .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE)
                        .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                        .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                        .setKeyNameSupplier(TreeBuilder.Menu::getName)
                        .setOnNodeClicked(TreeBuilder.Menu::handle)
                        .setBackground(MenuWidget.BACKGROUND));
                return true;
            }
        }
        if (title.isMouseOverElement(mouseX, mouseY) && button == 0) {
            if (lastClickTick != 0 && gui.getTickCount() - lastClickTick < 10) {
                playButtonClickSound();
                collapse();
            } else {
                lastClickTick = gui.getTickCount();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}

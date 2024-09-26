package com.lowdragmc.mbd2.common.gui.editor.machine;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.machine.widget.MachineStatePreview;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MachineConfigPanel extends MachineScenePanel {
    protected final FloatView floatView;

    public MachineConfigPanel(MachineEditor editor) {
        super(editor);
        setDrawShapeFrameLines(true);
        setDrawRenderingBoxFrameLines(true);
        addWidget(floatView = new FloatView());
        floatView.setDraggable(true);
        loadMachineState();
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        if (editor.getCurrentProject() instanceof MachineProject project) {
            editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, project.getDefinition());
        }
    }

    /**
     * Load the machine state.
     */
    public void loadMachineState() {
        if (editor.getCurrentProject() instanceof MachineProject project) {
            var definition = project.getDefinition();
            loadMachineStateRecursive(definition.stateMachine().getRootState(), new ArrayList<>(), 0);
        }
    }

    private void loadMachineStateRecursive(MachineState state, List<Integer> depthCount, int depth) {
        if (depthCount.size() <= depth) depthCount.add(0);
        var count = depthCount.get(depth);
        depthCount.set(depth, count + 1);
        var preview = new MachineStatePreview(this, state);
        preview.setSelfPosition(new Position(50 + count * 200, 50 + depth * 120));
        preview.collapse();
        floatView.addWidget(preview);
        for (var child : state.children()) {
            loadMachineStateRecursive(child, depthCount, depth + 1);
        }
    }

    /**
     * add a state preview. it doesn't mean to add a real state to the state machine.
     */
    public void onStateAdded(MachineState newState) {
        var preview = new MachineStatePreview(this, newState);
        preview.setSelfPosition(new Position((getSize().width - preview.getSize().width) / 2, (getSize().height - preview.getSize().height) / 2));
        floatView.addWidgetAnima(preview,  new Transform().duration(200).scale(0.2f));
    }

    /**
     * remove a state preview. it doesn't mean to remove a real state from the state machine.
     */
    public void onStateRemoved(MachineState state) {
        for (Widget widget : floatView.widgets) {
            if (widget instanceof MachineStatePreview preview && preview.getState() == state) {
                floatView.removeWidgetAnima(preview, new Transform().duration(200).scale(0.2f));
                break;
            }
        }
        state.children().forEach(this::onStateRemoved);
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.SECOND);
        if (previewMachine != null) {
            previewMachine.setMachineState("base");
        }
    }

    /**
     * Called when a state is selected.
     */
    public void onStateSelected(MachineState state) {
        editor.getConfigPanel().openConfigurator(MachineEditor.SECOND, state);
        if (previewMachine != null) {
            previewMachine.setMachineState(state.name());
        }
    }

    /**
     * Making scene to be intractable even the float view is hovered.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (buttonGroup.isMouseOverElement(mouseX, mouseY)) {
            buttonGroup.mouseClicked(mouseX, mouseY, button);
        }
        if (getHoverElement(mouseX, mouseY) == floatView) {
            scene.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Making scene to be intractable even the float view is hovered.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scene.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Making scene to be intractable even the float view is hovered.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        scene.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Making scene to be intractable even the float view is hovered.
     */
    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (getHoverElement(mouseX, mouseY) == floatView) {
            scene.mouseWheelMove(mouseX, mouseY, wheelDelta);
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    public class FloatView extends DraggableScrollableWidgetGroup {
        private FloatView() {
            super(0, 0, MachineConfigPanel.super.getSize().width, MachineConfigPanel.super.getSize().height);
        }

        @Override
        protected boolean hookDrawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            for (var widget : widgets) {
                if (widget instanceof MachineStatePreview preview) {
                    var parent = preview.getState().parent();
                    if (parent != null) {
                        widgets.stream().filter(w -> w instanceof MachineStatePreview p && p.getState() == parent).findFirst().ifPresent(p -> {
                            var pPos = p.getPosition().add(new Size(p.getSize().width / 2, p.getSize().height / 2));
                            var pos = preview.getPosition().add(new Size(preview.getSize().width / 2, preview.getSize().height / 2));
                            DrawerHelper.drawRoundLine(graphics, pPos, pos, 1, ColorPattern.GRAY.color, ColorPattern.GRAY.color);
                        });
                    }
                }
            }
            return false;
        }
    }
}

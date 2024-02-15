package com.lowdragmc.mbd2.common.gui.editor.step;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.*;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.widget.MachineStatePreview;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class MachineConfigStepPanel extends WidgetGroup {
    @Getter
    protected final MachineEditor editor;
    @Getter
    protected final TrackedDummyWorld level;
    protected final SceneWidget scene;
    protected final FloatView floatView;
    @Nullable
    protected MBDMachine previewMachine;

    public MachineConfigStepPanel(MachineEditor editor) {
        super(0, MenuPanel.HEIGHT, editor.getSize().getWidth() - ConfigPanel.WIDTH, editor.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
        addWidget(scene = new SceneWidget(0, 0, this.getSize().width, this.getSize().height, null));
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level = new TrackedDummyWorld());
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        scene.setAfterWorldRender(this::renderAfterWorld);
        resetScene();

        addWidget(floatView = new FloatView());
        floatView.setDraggable(true);
        loadMachineState();
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, editor.getCurrentProject().getDefinition());
    }

    /**
     * Reset the scene, it will reset everything to the default state, in general, you don't need to call this method.
     * to change renderer, using {@link MachineConfigStepPanel#previewMachine} instead.
     */
    public void resetScene() {
        this.level.clear();
        this.level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(MBDRegistries.getFAKE_MACHINE().block()));
        Optional.ofNullable(this.level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof MachineBlockEntity holder) {
                holder.setMachine(this.previewMachine = new MBDMachine(holder, editor.getCurrentProject().getDefinition()));
            }
        });
    }

    /**
     * Load the machine state.
     */
    public void loadMachineState() {
        var definition = editor.getCurrentProject().getDefinition();
        loadMachineStateRecursive(definition.stateMachine().getRootState(), new ArrayList<>(), 0);
    }

    private void loadMachineStateRecursive(MachineState state, List<Integer> depthCount, int depth) {
        if (depthCount.size() <= depth) depthCount.add(0);
        var count = depthCount.get(depth);
        depthCount.set(depth, count + 1);
        var preview = new MachineStatePreview(this, state);
        preview.setSelfPosition(new Position(50 + count * 200, 50 + depth * 200));
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
        editor.getConfigPanel().clearAllConfigurators(MachineEditor.MACHINE_STATE);
        if (previewMachine != null) {
            previewMachine.setMachineState("base");
        }
    }

    /**
     * Called when a state is selected.
     */
    public void onStateSelected(MachineState state) {
        editor.getConfigPanel().openConfigurator(MachineEditor.MACHINE_STATE, state);
        if (previewMachine != null) {
            previewMachine.setMachineState(state.name());
        }
    }

    /**
     * render the scene after the world is rendered.
     * <br/> e.g. <br/>
     * shape frame lines.
     */
    private void renderAfterWorld(SceneWidget sceneWidget) {
        if (previewMachine == null) return;
        PoseStack PoseStack = new PoseStack();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        PoseStack.pushPose();
        var tessellator = Tesselator.getInstance();
        RenderSystem.disableCull();
        BufferBuilder buffer = tessellator.getBuilder();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.lineWidth(10);
        Matrix4f matrix4f = PoseStack.last().pose();

        previewMachine.getMachineState().getShape(previewMachine.getFrontFacing().orElse(Direction.NORTH)).forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            float f = (float)(x1 - x0);
            float f1 = (float)(y1 - y0);
            float f2 = (float)(z1 - z0);
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;
            buffer.vertex(matrix4f, (float)(x0), (float)(y0), (float)(z0)).color(-1).normal(PoseStack.last().normal(), f, f1, f2).endVertex();
            buffer.vertex(matrix4f, (float)(x1), (float)(y1), (float)(z1)).color(-1).normal(PoseStack.last().normal(), f, f1, f2).endVertex();
        });

        tessellator.end();

        PoseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    /**
     * Making scene to be intractable even the float view is hovered.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
            super(0, 0, MachineConfigStepPanel.super.getSize().width, MachineConfigStepPanel.super.getSize().height);
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

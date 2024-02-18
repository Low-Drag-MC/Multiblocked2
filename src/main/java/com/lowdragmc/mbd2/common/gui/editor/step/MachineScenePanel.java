package com.lowdragmc.mbd2.common.gui.editor.step;

import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

public class MachineScenePanel extends WidgetGroup {
    @Getter
    protected final MachineEditor editor;
    @Getter
    protected final TrackedDummyWorld level;
    @Getter
    protected final SceneWidget scene;
    @Nullable
    @Getter
    protected MBDMachine previewMachine;

    public MachineScenePanel(MachineEditor editor) {
        super(0, MenuPanel.HEIGHT, editor.getSize().getWidth() - ConfigPanel.WIDTH, editor.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
        addWidget(scene = new SceneWidget(0, 0, this.getSize().width, this.getSize().height, null));
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level = new TrackedDummyWorld());
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        resetScene();
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
        reloadAdditionalTraits();
    }

    public void reloadAdditionalTraits() {
        if (previewMachine != null) {
            previewMachine.loadAdditionalTraits();
            previewMachine.getAdditionalTraits().forEach(ITrait::onLoadingTraitInPreview);
        }
    }
}

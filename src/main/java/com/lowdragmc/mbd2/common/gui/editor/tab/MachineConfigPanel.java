package com.lowdragmc.mbd2.common.gui.editor.tab;

import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class MachineConfigPanel extends SceneWidget {
    protected final MachineEditor editor;
    protected final TrackedDummyWorld level = new TrackedDummyWorld();

    public MachineConfigPanel(MachineEditor editor) {
        super(0, MenuPanel.HEIGHT, editor.getSize().getWidth() - ConfigPanel.WIDTH, editor.getSize().height - MenuPanel.HEIGHT - 16, null);
        this.editor = editor;
        setRenderFacing(false);
        setRenderSelect(false);
        createScene(level);
        renderer.setOnLookingAt(null); // better performance
        setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        resetScene();
    }

    public void resetScene() {
        this.level.clear();
        this.level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(MBDRegistries.getFAKE_MACHINE().block()));
        Optional.ofNullable(this.level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof MachineBlockEntity holder) {
                holder.setMachine(new MBDMachine(holder, editor.getCurrentProject().getDefinition()));
            }
        });
    }
}

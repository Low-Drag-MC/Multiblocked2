package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.widget.PatternLayerList;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import lombok.Getter;
import lombok.val;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

public class MultiblockPatternPanel extends WidgetGroup {
    @Getter
    protected final MachineEditor editor;
    @Getter
    protected final MultiblockMachineProject project;
    @Getter
    protected final TrackedDummyWorld level;
    @Getter
    protected final SceneWidget scene;
    protected final WidgetGroup buttonGroup;

    public MultiblockPatternPanel(MachineEditor editor, MultiblockMachineProject project) {
        super(0, MenuPanel.HEIGHT, Editor.INSTANCE.getSize().getWidth() - ConfigPanel.WIDTH, Editor.INSTANCE.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
        this.project = project;
        addWidget(scene = new SceneEditorWidget(0, 0, this.getSize().width, this.getSize().height, null));
        addWidget(buttonGroup = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height));
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level = new TrackedDummyWorld());
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setAfterWorldRender(this::renderAfterWorld);
        scene.useCacheBuffer();
        reloadScene();
        prepareButtonGroup();
        buttonGroup.setSize(new Size(Math.max(0, buttonGroup.widgets.size() * 25 - 5), 20));
        buttonGroup.setSelfPosition(new Position(this.getSize().width - buttonGroup.getSize().width - 25, 25));
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators();
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().setTitle("editor.machine.multiblock.multiblock_pattern.layer");
        editor.getToolPanel().addNewToolBox("editor.machine.multiblock.multiblock_pattern.layer", Icons.WIDGET_CUSTOM, size -> new PatternLayerList(editor, size));
        if (editor.getToolPanel().inAnimate()) {
            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
        } else {
            editor.getToolPanel().show();
        }
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        editor.getToolPanel().setTitle("ldlib.gui.editor.group.tool_box");
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators();
    }

    public void reloadScene() {
        this.level.clear();
        var holders = project.getBlockPlaceholders();
        var positions = new HashSet<BlockPos>();
        for (int x = 0; x < holders.length; x++) {
            for (int y = 0; y < holders[x].length; y++) {
                for (int z = 0; z < holders[x][y].length; z++) {
                    var holder = holders[x][y][z];
                    if (holder != null) {
                        val pos = new BlockPos(x, y, z);
                        positions.add(pos);
                        if (holder.isController()) {
                            MBDRegistries.getFAKE_MACHINE().blockProperties().rotationState().property.ifPresent(property ->
                                    this.level.addBlock(pos, BlockInfo.fromBlockState(MBDRegistries.getFAKE_MACHINE()
                                            .block().defaultBlockState().setValue(property, holder.getFacing()))));
                            Optional.ofNullable(this.level.getBlockEntity(pos)).ifPresent(blockEntity -> {
                                if (blockEntity instanceof MachineBlockEntity machineBlockEntity) {
                                    var controllerMachine = new MBDMachine(machineBlockEntity, project.getDefinition());
                                    machineBlockEntity.setMachine(controllerMachine);
                                    controllerMachine.loadAdditionalTraits();
                                    controllerMachine.getAdditionalTraits().forEach(ITrait::onLoadingTraitInPreview);
                                }
                            });
                        } else {
                            holder.getPredicates().stream().findAny().map(holder.predicateResource::getResource).ifPresent(predicate -> {
                                if (predicate.candidates == null) return;
                                var blockInfo = Arrays.stream(predicate.candidates.get()).findAny();
                                blockInfo.ifPresent(info -> this.level.addBlock(pos, info));
                            });
                        }
                    }
                }
            }
        }
        scene.setRenderedCore(positions, null);
    }


    /**
     * prepare the button group, you can add buttons / switches here.
     */
    protected void prepareButtonGroup() {
    }

    /**
     * render the scene after the world is rendered.
     * <br/> e.g. <br/>
     * shape frame lines.
     */
    private void renderAfterWorld(SceneWidget sceneWidget) {

    }

    /**
     * Called when the block placeholders are changed in the project.
     */
    public void onBlockPlaceholdersChanged() {
        reloadScene();
    }
}

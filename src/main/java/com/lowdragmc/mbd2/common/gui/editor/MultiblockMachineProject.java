package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.BlockPlaceholder;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockAreaPanel;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockPatternPanel;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleInteger;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleShape;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
@LDLRegister(name = "mb", group = "editor.machine")
@NoArgsConstructor
public class MultiblockMachineProject extends MachineProject {
    protected BlockPlaceholder[][][] blockPlaceholders;
    protected PredicateResource predicateResource;
    // runtime
    protected MultiblockPatternPanel multiblockPatternPanel;

    public MultiblockMachineProject(Resources resources, MultiblockMachineDefinition definition, WidgetGroup ui) {
        this.resources = resources;
        this.definition = definition;
        this.ui = ui;
        this.blockPlaceholders = new BlockPlaceholder[1][1][1];
        if (resources.resources.get(PredicateResource.RESOURCE_NAME) instanceof PredicateResource resource) {
            this.predicateResource = resource;
            this.blockPlaceholders[0][0][0] = BlockPlaceholder.controller(predicateResource);
        }
    }

    public void updateBlockPlaceholders(BlockPlaceholder[][][] blockPlaceholders) {
        this.blockPlaceholders = blockPlaceholders;
        if (this.multiblockPatternPanel != null) {
            this.multiblockPatternPanel.onBlockPlaceholdersChanged();
        }
    }

    @Override
    protected Map<String, Resource<?>> createResources() {
        var resources = super.createResources();
        // predicate
        var predicate = new PredicateResource();
        resources.put(PredicateResource.RESOURCE_NAME, predicate);
        return resources;
    }

    @Override
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    protected MultiblockMachineDefinition createDefinition() {
        // use vanilla furnace model as an example
        var renderer = new IModelRenderer(new ResourceLocation("block/furnace"));
        var builder = MultiblockMachineDefinition.builder();
        builder.id(MBD2.id("new_machine"))
                .stateMachine(StateMachine.create(b -> b
                        .renderer(new ToggleRenderer(renderer))
                        .shape(new ToggleShape(Shapes.block()))
                        .lightLevel(new ToggleInteger(0))))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build());
        return builder.build();
    }

    @Override
    public MultiblockMachineProject newEmptyProject() {
        return new MultiblockMachineProject(new Resources(createResources()), createDefinition(), createDefaultUI());
    }

    @Override
    public File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "multiblock");
    }

    public CompoundTag serializeNBT() {
        var tag = super.serializeNBT();
        var placeholders = new ArrayList<BlockPlaceholder>();
        var placeHolderMap = new HashMap<BlockPlaceholder, Integer>();
        var placeHolderIndex = new ArrayList<Integer>();
        for (BlockPlaceholder[][] blockPlaceholder : blockPlaceholders) {
            for (BlockPlaceholder[] value : blockPlaceholder) {
                for (BlockPlaceholder holder : value) {
                    if (holder != null) {
                        if (!placeHolderMap.containsKey(holder)) {
                            placeHolderMap.put(holder, placeholders.size());
                            placeholders.add(holder);
                        }
                        placeHolderIndex.add(placeHolderMap.get(holder));
                    } else {
                        placeHolderIndex.add(-1);
                    }
                }
            }
        }
        var placeHoldersTag = new CompoundTag();
        var placeHoldersListTag = new ListTag();
        for (BlockPlaceholder placeholder : placeholders) {
            placeHoldersListTag.add(placeholder.serializeNBT());
        }
        placeHoldersTag.put("holders", placeHoldersListTag);
        placeHoldersTag.putInt("x", blockPlaceholders.length);
        placeHoldersTag.putInt("y", blockPlaceholders[0].length);
        placeHoldersTag.putInt("z", blockPlaceholders[0][0].length);
        placeHoldersTag.putIntArray("pattern", placeHolderIndex.stream().mapToInt(i -> i).toArray());
        tag.put("placeholders", placeHoldersTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        var placeHoldersTag = tag.getCompound("placeholders");
        var placeHoldersListTag = placeHoldersTag.getList("holders", Tag.TAG_COMPOUND);
        var x = placeHoldersTag.getInt("x");
        var y = placeHoldersTag.getInt("y");
        var z = placeHoldersTag.getInt("z");
        var pattern = placeHoldersTag.getIntArray("pattern");
        if (resources.resources.get(PredicateResource.RESOURCE_NAME) instanceof PredicateResource predicateResource) {
            this.blockPlaceholders = new BlockPlaceholder[x][y][z];
            for (int index : pattern) {
                var holder = index == -1 ? null : BlockPlaceholder.fromTag(predicateResource, placeHoldersListTag.getCompound(index));
                this.blockPlaceholders[index / (y * z)][(index / z) % y][index % z] = holder;
            }
        }
    }

    @Override
    public void onLoad(Editor editor) {
        if (editor instanceof MachineEditor machineEditor) {
            super.onLoad(editor);
            var tabContainer = machineEditor.getTabPages();
            var multiblockAreaPanel = new MultiblockAreaPanel(this);
            multiblockPatternPanel = new MultiblockPatternPanel(machineEditor, this);
            tabContainer.addTab("editor.machine.multiblock_area", multiblockAreaPanel, multiblockAreaPanel::onPanelSelected, multiblockAreaPanel:: onPanelDeselected);
            tabContainer.addTab("editor.machine.multiblock_pattern", multiblockPatternPanel, multiblockPatternPanel::onPanelSelected, multiblockPatternPanel::onPanelDeselected);
        }
    }

}

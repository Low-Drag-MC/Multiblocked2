package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.texture.IRendererSlotTexture;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleCreativeTab;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigItemProperties implements IConfigurable, IPersistedSerializable {

    @Configurable(name = "config.item_properties.use_block_light",
            tips = {"config.item_properties.use_block_light.tooltip.0", "config.item_properties.use_block_light.tooltip.1", "config.item_properties.use_block_light.tooltip.2"})
    @Builder.Default
    private boolean useBlockLight = true;

    @Configurable(name = "config.item_properties.is_gui_3d", tips = "config.item_properties.is_gui_3d.tooltip")
    @Builder.Default
    private boolean isGui3d = true;

    @Configurable(name = "config.item_properties.renderer", subConfigurable = true, tips =
            {"config.item_properties.renderer.tooltip.0", "config.item_properties.renderer.tooltip.1"})
    @Builder.Default
    private ToggleRenderer renderer = new ToggleRenderer();

    @Configurable(name = "config.item_properties.max_stack_size", tips = {"config.item_properties.max_stack_size.tooltip",
            "config.require_restart"})
    @NumberRange(range = {1, 64})
    @Builder.Default
    private int maxStackSize = 64;

    @Configurable(name = "config.item_properties.rarity", tips = {"config.item_properties.rarity.tooltip",
            "config.require_restart"})
    @Builder.Default
    private Rarity rarity = Rarity.COMMON;

    @Configurable(name = "config.item_properties.item_tooltips", tips = "config.item_properties.item_tooltips.tooltip")
    @Builder.Default
    private List<Component> itemTooltips = new ArrayList<>();

    @Persisted
    @Configurable(name = "config.item_properties.creative_tab", subConfigurable = true,
            tips = "config.item_properties.creative_tab.tooltip")
    @Builder.Default
    private ToggleCreativeTab creativeTab = new ToggleCreativeTab(true);

    public Item.Properties apply(Item.Properties itemProp) {
        return itemProp.stacksTo(maxStackSize).rarity(rarity);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        father.addConfigurators(new WrapperConfigurator("config.item_properties.slot_preview",
                new ImageWidget(0, 0, 18, 18,
                        new IRendererSlotTexture(() -> {
                            if (renderer.isEnable()) {
                                return renderer.getValue();
                            }
                            if (Editor.INSTANCE instanceof MachineEditor editor) {
                                if (editor.getCurrentProject() instanceof MachineProject project) {
                                    return project.getDefinition().getState("base").getRealRenderer();
                                }
                            }
                            return IRenderer.EMPTY;
                        }))));
    }
}

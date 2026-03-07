package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.gui.editor.texture.IRendererSlotTexture;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleCreativeTab;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleRenderer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigItemProperties implements IConfigurable, IPersistedSerializable {
    @Setter
    protected MBDMachineDefinition definition;

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
    @ConfigNumber(range = {1, 64})
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
        father.addConfigurator(new Configurator("config.item_properties.slot_preview").addInlineChild(
                new UIElement().layout(layout -> layout.width(18).height(18))
                        .style(style -> style.background(IGuiTexture.group(
                                MCSprites.RECT_1,
                                new IRendererSlotTexture(() -> {
                                    if (renderer.isEnable()) {
                                        return renderer.getValue();
                                    }
                                    return definition == null ? IRenderer.EMPTY : definition.getState("base").getRealRenderer();
                                })
                        )))
        ));
    }
}

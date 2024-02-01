package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

@Getter
@Accessors(fluent = true)
@Builder
public class ConfigItemProperties {

    @Configurable(name = "config.item_properties.use_block_light",
            tips = {"config.item_properties.use_block_light.tooltip.0", "config.item_properties.use_block_light.tooltip.1", "config.item_properties.use_block_light.tooltip.2"})
    @Builder.Default
    private boolean useBlockLight = true;

    @Configurable(name = "config.item_properties.is_gui_3d", tips = "config.item_properties.is_gui_3d.tooltip")
    @Builder.Default
    private boolean isGui3d = true;

    @Builder.Default
    private IRenderer renderer = IRenderer.EMPTY;

    @Configurable(name = "config.item_properties.max_stack_size", tips = "config.item_properties.max_stack_size.tooltip")
    @NumberRange(range = {1, 64})
    @Builder.Default
    private int maxStackSize = 64;

    @Configurable(name = "config.item_properties.rarity", tips = "config.item_properties.rarity.tooltip")
    @Builder.Default
    private Rarity rarity = Rarity.COMMON;

    public Item.Properties apply(Item.Properties itemProp) {
        return itemProp.stacksTo(maxStackSize).rarity(rarity);
    }
}

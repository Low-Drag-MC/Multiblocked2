package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleCatalyst;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigMultiblockSettings implements IConfigurable, IPersistedSerializable {

    @Configurable(name = "config.multiblock_settings.catalyst", subConfigurable = true, tips = "config.multiblock_settings.catalyst.tooltip")
    @Builder.Default
    private ToggleCatalyst catalyst = new ToggleCatalyst();

    @Configurable(name = "config.multiblock_settings.show_ui_only_formed")
    @Builder.Default
    private boolean showUIOnlyFormed = true;

    @Configurable(name = "config.multiblock_settings.show_ui_when_click_structure",
            tips = "config.multiblock_settings.show_ui_when_click_structure.tooltip")
    @Builder.Default
    private boolean showUIWhenClickStructure = false;

}

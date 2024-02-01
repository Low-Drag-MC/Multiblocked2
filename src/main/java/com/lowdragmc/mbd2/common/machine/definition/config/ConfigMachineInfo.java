package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@Builder
public class ConfigMachineInfo {
    @Configurable(name = "config.machine_info.is_gui_3d", tips = "config.machine_info.is_gui_3d.tooltip")
    @Builder.Default
    private MBDRecipeType recipeType = MBDRecipeType.DUMMY;
}

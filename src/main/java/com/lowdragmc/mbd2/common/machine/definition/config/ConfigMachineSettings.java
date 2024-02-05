package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@Builder
public class ConfigMachineSettings {
    @Builder.Default
    private MBDRecipeType recipeType = MBDRecipeType.DUMMY;
}

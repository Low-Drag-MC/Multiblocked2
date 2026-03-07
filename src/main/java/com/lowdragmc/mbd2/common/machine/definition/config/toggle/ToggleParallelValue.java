package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ToggleParallelValue implements IToggleConfigurable {
    protected boolean enable;

    @Configurable(name = "config.machine_settings.max_parallel", tips = "config.machine_settings.max_parallel.tooltip")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int maxParallel = 1;

    @Configurable(name = "config.machine_settings.modify_duration", tips = "config.machine_settings.modify_duration.tooltip")
    private boolean modifyDuration = false;

    public ToggleParallelValue() {
        enable = false;
    }

}

package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ToggleParallelValue implements IToggleConfigurable {

    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    @Configurable(name = "config.machine_settings.max_parallel", tips = "config.machine_settings.max_parallel.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int maxParallel = 1;

    @Configurable(name = "config.machine_settings.modify_duration", tips = "config.machine_settings.modify_duration.tooltip")
    private boolean modifyDuration = false;

    public ToggleParallelValue() {
        enable = false;
    }

}

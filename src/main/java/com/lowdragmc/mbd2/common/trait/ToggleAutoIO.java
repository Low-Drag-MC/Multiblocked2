package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;

public class ToggleAutoIO extends AutoIO implements IToggleConfigurable {
    @Getter
    @Setter
    @Persisted
    private boolean enable;
}

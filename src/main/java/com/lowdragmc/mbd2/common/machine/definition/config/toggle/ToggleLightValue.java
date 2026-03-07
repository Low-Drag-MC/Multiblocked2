package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;

public class ToggleLightValue extends ToggleObject<Integer> {
    @Configurable
    @ConfigNumber(range = {0, 15})
    private int value;

    public ToggleLightValue(int value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleLightValue(int value) {
        this(value, true);
    }

    public ToggleLightValue(boolean enable) {
        this(0, enable);
    }

    public ToggleLightValue() {
        this(false);
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Integer value) {
        this.value = value;
    }
}

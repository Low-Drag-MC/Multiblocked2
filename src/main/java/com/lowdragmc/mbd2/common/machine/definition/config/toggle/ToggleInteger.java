package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;

public class ToggleInteger extends ToggleObject<Integer> {
    @Configurable
    private int value;

    public ToggleInteger(int value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleInteger(int value) {
        this(value, true);
    }

    public ToggleInteger(boolean enable) {
        this(0, enable);
    }

    public ToggleInteger() {
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

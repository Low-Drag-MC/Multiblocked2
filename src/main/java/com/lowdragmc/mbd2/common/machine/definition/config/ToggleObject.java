package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;

public abstract class ToggleObject<T> implements IToggleConfigurable {

    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    public abstract T getValue();

    public abstract void setValue(T value);

    public ToggleObject(boolean enable) {
        this.enable = enable;
    }

    public ToggleObject() {
        this(false);
    }

}
